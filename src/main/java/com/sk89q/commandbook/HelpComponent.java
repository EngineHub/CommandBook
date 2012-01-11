package com.sk89q.commandbook;

import com.sk89q.commandbook.components.AbstractComponent;
import com.sk89q.commandbook.components.ComponentInformation;
import com.sk89q.commandbook.config.ConfigurationBase;
import com.sk89q.commandbook.config.Setting;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.WrappedCommandException;
import com.sk89q.util.ReflectionUtil;
import com.sk89q.util.yaml.YAMLFormat;
import com.sk89q.util.yaml.YAMLProcessor;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.sk89q.commandbook.CommandBookUtil.replaceColorMacros;

/**
 * Simple but very messy Help component.
 */
@ComponentInformation(friendlyName = "Help", desc = "Provides help on various command actions")
public class HelpComponent extends AbstractComponent {
    private YAMLProcessor help;
    private Map<String, String[]> messages = new HashMap<String, String[]>();
    private LocalConfiguration config;
    @Override
    public void initialize() {
        config = configure(new LocalConfiguration());
        File helpFile = new File(CommandBook.inst().getDataFolder(), config.helpFile);
        if (!helpFile.getParentFile().exists() || !helpFile.getParentFile().isDirectory()) helpFile.getParentFile().mkdirs();
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

    public boolean reloadMessages() {
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
            CommandBook.logger().severe("CommandBook: Unable to find getFallback method in SimpleCommandMap!");
        }
        SimpleCommandMap_getFallback = method;
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


    public class HelpCommands {
        @Command(aliases = "help",
        usage = "[topic]", desc = "Provides help for the server!",
        flags = "c", min = 0, max = 1)
        public void help(CommandContext args, CommandSender sender) throws CommandException {
            if (args.hasFlag('c')) { // Looking up command help
                if (args.argsLength() < 1) {
                    throw new CommandException("No command given!");
                }
                org.bukkit.command.Command cmd = getCommand(args.getString(0));
                if (cmd == null) {
                    throw new CommandException("Unknown command '" + args.getString(0) + "'; no help available");
                } else {
                    sender.sendMessage(ChatColor.YELLOW + "Command: " + cmd.getName());
                    sender.sendMessage(ChatColor.YELLOW + "Aliases: " + cmd.getAliases().toString().replaceAll("\\[(.*)\\]", "$1"));
                    sender.sendMessage(ChatColor.YELLOW + "Description: " + cmd.getDescription());
                    sender.sendMessage(ChatColor.YELLOW + "Usage: " + cmd.getUsage());
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
