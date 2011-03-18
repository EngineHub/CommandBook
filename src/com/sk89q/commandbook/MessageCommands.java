// $Id$
/*
 * CommandBook
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

package com.sk89q.commandbook;

import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;

public class MessageCommands {
    
    @Command(aliases = {"me"},
            usage = "<message...>", desc = "Send an action message",
            min = 1, max = -1)
    @CommandPermissions({"commandbook.say.me"})
    public static void me(CommandContext args, CommandBookPlugin plugin,
            CommandSender sender) throws CommandException {
        
        plugin.getServer().broadcastMessage(
                "* " + plugin.toName(sender) + " " + args.getJoinedStrings(0));
    }
    
    @Command(aliases = {"say"},
            usage = "<message...>", desc = "Send a message",
            min = 1, max = -1)
    @CommandPermissions({"commandbook.say"})
    public static void say(CommandContext args, CommandBookPlugin plugin,
            CommandSender sender) throws CommandException {
        
        plugin.getServer().broadcastMessage(
                "<" + plugin.toColoredName(sender, ChatColor.WHITE)
                + "> " + args.getJoinedStrings(0));
    }
    
    @Command(aliases = {"msg"},
            usage = "<target> <message...>", desc = "Private message a user",
            min = 2, max = -1)
    @CommandPermissions({"commandbook.msg"})
    public static void msg(CommandContext args, CommandBookPlugin plugin,
            CommandSender sender) throws CommandException {
        
        // This will throw errors as needed
        CommandSender receiver =
                plugin.matchPlayerOrConsole(sender, args.getString(0));
        String message = args.getJoinedStrings(1);
        String senderUName = plugin.toUniqueName(sender);
        String receiverUName = plugin.toUniqueName(receiver);
        
        // We'll be using this
        Map<String, String> memory = plugin.getMessageTargets();

        receiver.sendMessage(ChatColor.GRAY + "(From "
                + plugin.toName(sender) + "): "
                + ChatColor.WHITE + message);
        
        sender.sendMessage(ChatColor.GRAY + "(To "
                + plugin.toName(receiver) + "): "
                + ChatColor.WHITE + message);
        
        memory.put(senderUName, receiverUName);
        
        // If the receiver hasn't had any player talk to them yet or hasn't
        // send a message, then we add it to the receiver's last message target
        // so s/he can /reply easily
        if (!memory.containsKey(receiverUName)) {
            memory.put(receiverUName, senderUName);
        }
    }

    @Command(aliases = {"reply"},
            usage = "<message...>", desc = "Reply to last user",
            min = 1, max = -1)
    @CommandPermissions({"commandbook.msg"})
    public static void reply(CommandContext args, CommandBookPlugin plugin,
            CommandSender sender) throws CommandException {
        
        String message = args.getJoinedStrings(0);
        String senderUName = plugin.toUniqueName(sender);
        CommandSender receiver;

        Map<String, String> memory = plugin.getMessageTargets();
        
        if (memory.containsKey(senderUName)) {
            // This will throw errors as needed
            receiver = plugin.matchPlayerOrConsole(sender,
                    memory.get(senderUName));
        } else {
            sender.sendMessage(ChatColor.RED + "You haven't messaged anyone.");
            return;
        }       
        
        String receiverUName = plugin.toUniqueName(receiver);

        receiver.sendMessage(ChatColor.GRAY + "(From "
                + plugin.toName(sender) + "): "
                + ChatColor.WHITE + message);
        
        sender.sendMessage(ChatColor.GRAY + "(To "
                + plugin.toName(receiver) + "): "
                + ChatColor.WHITE + message);
        
        // If the receiver hasn't had any player talk to them yet or hasn't
        // send a message, then we add it to the receiver's last message target
        // so s/he can /reply easily
        if (!memory.containsKey(receiverUName)) {
            memory.put(receiverUName, senderUName);
        }
    }
}
