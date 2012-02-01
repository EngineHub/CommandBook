package com.sk89q.commandbook;

import com.sk89q.bukkit.util.DynamicPluginCommand;
import com.sk89q.commandbook.commands.PaginatedResult;
import com.sk89q.commandbook.components.AbstractComponent;
import com.sk89q.commandbook.components.ComponentInformation;
import com.sk89q.commandbook.config.ConfigurationBase;
import com.sk89q.commandbook.config.Setting;
import com.sk89q.minecraft.util.commands.*;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.util.ReflectionUtil;
import com.sk89q.util.yaml.YAMLFormat;
import com.sk89q.util.yaml.YAMLProcessor;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.command.defaults.VanillaCommand;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import static com.sk89q.commandbook.CommandBookUtil.replaceColorMacros;

/**
 * Simple but very messy Help component.
 */
@ComponentInformation(friendlyName = "Help", desc = "Provides help on various command actions")
public class HelpComponent extends AbstractComponent {
    private YAMLProcessor help;
    private final Map<String, String[]> messages = new HashMap<String, String[]>();
    private LocalConfiguration config;
    
    @Override
    public void enable() {
        config = configure(new LocalConfiguration());
        File helpFile = new File(CommandBook.inst().getDataFolder(), config.helpFile);
        if (!helpFile.getParentFile().exists() || !helpFile.getParentFile().isDirectory())
            helpFile.getParentFile().mkdirs();
        if (!helpFile.exists() || !helpFile.isFile()) try {
            helpFile.createNewFile();
        } catch (IOException e) {}
        help = new YAMLProcessor(helpFile, true, YAMLFormat.EXTENDED);
        reloadMessages();
        registerCommands(HelpCommands.class);
    }
    
    @Override
    public void reload() {
        super.reload();
        reloadMessages();
        configure(config);
    }
    
    private static class LocalConfiguration extends ConfigurationBase {
        @Setting("help-file") public String helpFile = "help.yml";
        @Setting("command-help") public boolean commandHelp = true;
    }
    
    private static final String demoHelpMessage =
            "This is a component to provide help for servers\n" +
            "`r/help -c <commandname>`w gives help for a command\n" +
            "`r/help <topic>`w gives help for a topic from a list specified by the server admin.";

    private boolean reloadMessages() {
        try {
            help.load();
        } catch (IOException e) {
            return false;
        }
        
        List<String> keys = help.getKeys("topics");
        if (keys == null) {
            help.setProperty("topics.help", demoHelpMessage);
            keys = new ArrayList<String>();
            keys.add("help");
            help.save();
        }
        
        for (String key : keys) {
            String information = help.getString("topics." + key);
            if (information != null && information.trim().length() != 0) {
                information = replaceColorMacros(information);
                String[] split = information.split("\\n");
                for (int i = 0; i < split.length; i++) {
                    split[i] = split[i].replaceAll("[\\r\\n]", "");
                }
                messages.put(key, split);
            }

        }
        return true;
    }

    public void printTopics(CommandSender sender, String prefix) {
        StringBuilder sb = new StringBuilder(ChatColor.YELLOW.toString());
        if (prefix != null && !prefix.isEmpty()) sb.append(prefix).append(" ");
        sb.append("Available options are: ");
        boolean first = true;
        int count = 0;
        for (String key : messages.keySet()) {
            if (!CommandBook.inst().hasPermission(sender, "commandbook.help.topic." + key)) continue;
            if (!first) sb.append(ChatColor.YELLOW).append(", ");
            sb.append(ChatColor.LIGHT_PURPLE).append(key);
            first = false;
            ++count;
        }
        if (count <= 0) {
            sender.sendMessage(ChatColor.YELLOW + "No available help options!");
        } else {
            sender.sendMessage(sb.toString());
        }
    }

    /**
     * The method that returns fallback commands in SimpleCommandMap. 
     * Cached here for better performance when running commands
     */
    private static final Method SimpleCommandMap_getFallback;
    private static final Field SimpleCommandMap_knownCommands;
    static {
        Method method = null;
        try {
            method = SimpleCommandMap.class.getDeclaredMethod("getFallback", String.class);
            method.setAccessible(true);
            if (!org.bukkit.command.Command.class.isAssignableFrom(method.getReturnType())) {
                method = null;
                CommandBook.logger().severe("SimpleCommandMap.getFallback does not return a Command!");
            }
        } catch (NoSuchMethodException e) {
            CommandBook.logger().severe("Unable to find getFallback method in SimpleCommandMap!");
        }
        SimpleCommandMap_getFallback = method;

        Field field = null;
        try {
            field = SimpleCommandMap.class.getDeclaredField("knownCommands");
            field.setAccessible(true);
            if (!Map.class.isAssignableFrom(field.getType())) {
                field = null;
                CommandBook.logger().severe("SimpleCommandMap.knownCommands is not a Map!");
            }
        } catch (NoSuchFieldException e) {
            CommandBook.logger().severe("Unable to find knownCommands field in SimpleCommandMap!");
        }
        SimpleCommandMap_knownCommands = field;
    }
    
    public org.bukkit.command.Command getCommand(String name) throws CommandException {
        CommandMap commandMap = ReflectionUtil.getField(CommandBook.server().getPluginManager(), "commandMap");
        if (commandMap == null) {
            return null;
        }
        org.bukkit.command.Command command = commandMap.getCommand(name);
        if (command == null && SimpleCommandMap_getFallback != null) {
            try {
                command = (org.bukkit.command.Command)SimpleCommandMap_getFallback.invoke(commandMap, name);
            } catch (IllegalAccessException e) {
                throw new WrappedCommandException(e);
            } catch (InvocationTargetException e) {
                throw new WrappedCommandException(e);
            }
        }
        return command;
    }
    
    public Collection<org.bukkit.command.Command> getServerCommands() throws CommandException {
        CommandMap commandMap = ReflectionUtil.getField(CommandBook.server().getPluginManager(), "commandMap");
        if (commandMap == null || SimpleCommandMap_knownCommands == null) {
            return Collections.emptySet();
        }
        try {
            return ((Map<Object, org.bukkit.command.Command>)SimpleCommandMap_knownCommands
                    .get(commandMap)).values();
        } catch (IllegalAccessException e) {
            throw new WrappedCommandException(e);
        }
    }
    
    public void printCommandHelp(CommandSender sender, org.bukkit.command.Command cmd) {
        sender.sendMessage(ChatColor.YELLOW + "Command: " + cmd.getName());
        final String aliases = cmd.getAliases().toString().replaceAll("\\[(.*)\\]", "$1");
        if (aliases.length() > 0) {
            sender.sendMessage(ChatColor.YELLOW + "Aliases: " + aliases);
        }
        sender.sendMessage(ChatColor.YELLOW + "Description: " + cmd.getDescription());
        sender.sendMessage(ChatColor.YELLOW + "Usage: " + cmd.getUsage());
        if (cmd instanceof PluginCommand) {
            sender.sendMessage(ChatColor.YELLOW + "Plugin: " + 
                    ((PluginCommand)cmd).getPlugin().getDescription().getName());
        } else if (cmd instanceof DynamicPluginCommand) {
            sender.sendMessage(ChatColor.YELLOW + "Owner: " + 
                    ((DynamicPluginCommand) cmd).getOwner().getClass().getSimpleName());
        } else if (cmd instanceof VanillaCommand) {
            sender.sendMessage(ChatColor.YELLOW + "Vanilla command");
        }
    }


    public class HelpCommands {
        @Command(aliases = "help",
        usage = "[-p page] [topic]", desc = "Provides help for the server!",
        flags = "cp:", min = 0, max = 1)
        @CommandPermissions({"commandbook.help", "commandbook.help.command", "commandbook.help.topic"})
        public void help(CommandContext args, CommandSender sender) throws CommandException {
            if (args.hasFlag('c')) { // Looking up command help
                if (!config.commandHelp) {
                    throw new CommandException("Help for commands is not enabled!");
                }

                if (args.argsLength() == 0) {
                    Collection<org.bukkit.command.Command> serverCommands = getServerCommands();
                    for (Iterator<org.bukkit.command.Command> i = serverCommands.iterator(); i.hasNext();) {
                        final String permission = i.next().getPermission();
                        if (!(permission == null || permission.length() == 0 || CommandBook.inst().hasPermission(sender, permission))) {
                            i.remove();
                        }
                    }
                    new PaginatedResult<org.bukkit.command.Command>("Usage - Description") {
                        @Override
                        public String format(org.bukkit.command.Command entry) {
                            return entry.getUsage() + " - " 
                                    + entry.getDescription();
                        }
                    }.display(sender, serverCommands, args.getFlagInteger('p', 1));
                } else {
                    org.bukkit.command.Command cmd = getCommand(args.getString(0));
                    if (cmd == null) {
                        throw new CommandException("Unknown command '" + args.getString(0) + "'; no help available");
                    } else {
                        CommandBook.inst().checkPermission(sender, "commandbook.help.command." + cmd.getName());
                        printCommandHelp(sender, cmd);
                    }
                }
            } else if (args.argsLength() == 0) {
                    printTopics(sender, "No help option specified.");
            } else {
                if (!messages.containsKey(args.getString(0).toLowerCase())) {
                    printTopics(sender, "Unknown topic specified.");
                } else {
                    String chosen = args.getString(0);
                    CommandBook.inst().checkPermission(sender, "commandbook.help.topic." + chosen);
                    String[] lines = messages.get(chosen);
                    sender.sendMessage(ChatColor.YELLOW + "Help about " + chosen + ":");
                    for (String line : lines) {
                        sender.sendMessage(ChatColor.AQUA + line.replaceAll(ChatColor.WHITE.toString(), ChatColor.AQUA.toString()));
                    }
                }
            }
        }

    }
}
