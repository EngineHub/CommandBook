package com.sk89q.commandbook;

import com.sk89q.commandbook.commands.PaginatedResult;
import com.zachsthings.libcomponents.spout.SpoutComponent;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import com.sk89q.util.yaml.YAMLFormat;
import com.sk89q.util.yaml.YAMLProcessor;
import org.spout.api.ChatColor;
import org.spout.api.command.CommandContext;
import org.spout.api.command.CommandSource;
import org.spout.api.command.annotated.Command;
import org.spout.api.command.annotated.CommandPermissions;
import org.spout.api.exception.CommandException;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static com.sk89q.commandbook.CommandBookUtil.replaceColorMacros;

/**
 * Simple but very messy Help component.
 */
@ComponentInformation(friendlyName = "Help", desc = "Provides help on various command actions")
public class HelpComponent extends SpoutComponent {
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

    public void printTopics(CommandSource sender, String prefix) {
        StringBuilder sb = new StringBuilder(ChatColor.YELLOW.toString());
        if (prefix != null && !prefix.isEmpty()) sb.append(prefix).append(" ");
        sb.append("Available options are: ");
        boolean first = true;
        int count = 0;
        for (String key : messages.keySet()) {
            if (!sender.hasPermission("commandbook.help.topic." + key)) continue;
            if (!first) sb.append(ChatColor.YELLOW).append(", ");
            sb.append(ChatColor.PINK).append(key);
            first = false;
            ++count;
        }
        if (count <= 0) {
            sender.sendMessage(ChatColor.YELLOW + "No available help options!");
        } else {
            sender.sendMessage(sb.toString());
        }
    }
    
    public org.spout.api.command.Command getCommand(String name) throws CommandException {
        return CommandBook.game().getRootCommand().getChild(name);
    }
    
    public Collection<org.spout.api.command.Command> getServerCommands() throws CommandException {
        return CommandBook.game().getRootCommand().getChildCommands();
    }
    
    public void printCommandHelp(CommandSource sender, org.spout.api.command.Command cmd) {
        sender.sendMessage(ChatColor.YELLOW + "Command: " + cmd.getPreferredName());
        final String aliases = cmd.getNames().toString().replaceAll("\\[(.*)\\]", "$1");
        if (aliases.length() > 0) {
            sender.sendMessage(ChatColor.YELLOW + "Aliases: " + aliases);
        }
        sender.sendMessage(ChatColor.YELLOW + "Usage: " + cmd.getUsage());
        sender.sendMessage(ChatColor.YELLOW + "Description: " + cmd.getHelp());
        sender.sendMessage(ChatColor.YELLOW + "Owner: " + cmd.getOwnerName());
    }


    public class HelpCommands {
        @Command(aliases = "help",
        usage = "[-p page] [topic]", desc = "Provides help for the server!",
        flags = "cp:", min = 0, max = 1)
        @CommandPermissions({"commandbook.help", "commandbook.help.command", "commandbook.help.topic"})
        public void help(CommandContext args, CommandSource sender) throws CommandException {
            if (args.hasFlag('c')) { // Looking up command help
                if (!config.commandHelp) {
                    throw new CommandException("Help for commands is not enabled!");
                }

                if (args.length() == 0) {
                    Collection<org.spout.api.command.Command> serverCommands = getServerCommands();
                    for (Iterator<org.spout.api.command.Command> i = serverCommands.iterator(); i.hasNext();) {
                        if (!i.next().hasPermission(sender)) {
                            i.remove();
                        }
                    }
                    new PaginatedResult<org.spout.api.command.Command>("Usage - Description") {
                        @Override
                        public String format(org.spout.api.command.Command entry) {
                            return entry.getUsage() + " - " + entry.getHelp();
                        }
                    }.display(sender, serverCommands, args.getFlagInteger('p', 1));
                } else {
                    org.spout.api.command.Command cmd = getCommand(args.getString(0));
                    if (cmd == null) {
                        throw new CommandException("Unknown command '" + args.getString(0) + "'; no help available");
                    } else {
                        CommandBook.inst().checkPermission(sender, "commandbook.help.command." + cmd.getPreferredName());
                        printCommandHelp(sender, cmd);
                    }
                }
            } else if (args.length() == 0) {
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
                        sender.sendMessage(ChatColor.CYAN + line.replaceAll(ChatColor.WHITE.toString(), ChatColor.CYAN.toString()));
                    }
                }
            }
        }

    }
}
