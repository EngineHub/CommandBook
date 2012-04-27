// $Id$
/*
 * Copyright (C) 2010, 2011 sk89q <http://www.sk89q.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

package com.sk89q.commandbook.commands;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.minecraft.util.commands.*;
import com.zachsthings.libcomponents.AbstractComponent;
import com.zachsthings.libcomponents.ComponentInformation;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.Map;

public class CommandBookCommands {
    
    public static class CommandBookParentCommand {
        @Command(aliases = {"cmdbook"}, desc = "CommandBook commands",
                flags = "d", min = 1, max = 3)
        @NestedCommand({CommandBookCommands.class})
        public static void cmdBook() {
        }
    }
    
    @Command(aliases = {"version"}, usage = "", desc = "CommandBook version information", min = 0, max = 0)
    public static void version(CommandContext args, CommandSender sender) throws CommandException {
        sender.sendMessage(ChatColor.YELLOW + "CommandBook " + CommandBook.inst().getDescription().getVersion());
        sender.sendMessage(ChatColor.YELLOW + "http://www.sk89q.com");
    }
    
    @Command(aliases = {"reload"}, usage = "", desc = "Reload CommandBook's settings", min = 0, max = 0)
    @CommandPermissions({"commandbook.reload"})
    public static void reload(CommandContext args, CommandSender sender) throws CommandException {
        CommandBook.inst().populateConfiguration();
        CommandBook.inst().getComponentManager().reloadComponents();
        
        sender.sendMessage(ChatColor.YELLOW + "CommandBook's configuration has been reloaded.");
    }

    @Command(aliases = {"save"}, usage = "", desc = "Save CommandBook's settings", min = 0, max = 0)
    @CommandPermissions({"commandbook.save"})
    public static void save(CommandContext args, CommandSender sender) throws CommandException {
        CommandBook.inst().getGlobalConfiguration().save();

        sender.sendMessage(ChatColor.YELLOW + "CommandBook's configuration has been reloaded.");
    }
    
    @Command(aliases = {"help", "doc"}, usage = "<component>", desc = "Get documentation for a component",
            flags = "p:", min = 0, max = 1)
    @CommandPermissions("commandbook.component.help")
    public static void help(CommandContext args, CommandSender sender) throws CommandException {
        if (args.argsLength() == 0) {
            new PaginatedResult<AbstractComponent>("Name - Description") {
                @Override
                public String format(AbstractComponent entry) {
                    return entry.getInformation().friendlyName() + " - " + entry.getInformation().desc();
                }
            }.display(sender, CommandBook.inst().getComponentManager().getComponents(), args.getFlagInteger('p', 1));
            
        } else {
            final String componentName = args.getString(0).replaceAll(" ", "-").toLowerCase();
            AbstractComponent component = CommandBook.inst().getComponentManager().getComponent(componentName);
            if (component == null) {
                throw new CommandException("No such component: " + componentName);
            }
            final ComponentInformation info = component.getInformation();
            sender.sendMessage(ChatColor.YELLOW + info.friendlyName() + " - " + info.desc());
            if (info.authors().length > 0 && info.authors()[0].length() > 0) {
                sender.sendMessage(ChatColor.YELLOW + "Authors: " + 
                        Arrays.toString(info.authors()).replaceAll("[(.*)]", "$1"));
            }
            Map<String, String> commands = component.getCommands();
            if (commands.size() > 0) {
                new PaginatedResult<Map.Entry<String, String>>("    Command - Description") {
                    @Override
                    public String format(Map.Entry<String, String> entry) {
                        return "    /" + entry.getKey() + " " + entry.getValue();
                    }
                }.display(sender, commands.entrySet(), args.getFlagInteger('p', 1));
            } else {
                sender.sendMessage(ChatColor.YELLOW + "No commands");
            }
        }
        
    }
    
}
