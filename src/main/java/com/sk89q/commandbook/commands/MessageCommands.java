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

package com.sk89q.commandbook.commands;

import static com.sk89q.commandbook.CommandBookUtil.replaceColorMacros;
import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerChatEvent;
import com.sk89q.commandbook.CommandBookPlugin;
import com.sk89q.commandbook.events.CommandSenderMessageEvent;
import com.sk89q.commandbook.events.SharedMessageEvent;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;

public class MessageCommands {

	protected static final Logger logger = Logger.getLogger("Minecraft.CommandBook");
	
    @Command(aliases = {"me"},
            usage = "<message...>", desc = "Send an action message",
            min = 1, max = -1)
    @CommandPermissions({"commandbook.say.me"})
    public static void me(CommandContext args, CommandBookPlugin plugin,
            CommandSender sender) throws CommandException {
        
        if (sender instanceof Player && plugin.getAdminSession((Player) sender).isMute()) {
            sender.sendMessage(ChatColor.RED + "You are muted.");
            return;
        }
        
        String name = plugin.toName(sender);
        String msg = args.getJoinedStrings(0);

        plugin.getServer().getPluginManager().callEvent(
                new SharedMessageEvent(name + " " + msg));
        
        Logger.getLogger("Minecraft").info("<" + name + ">: " + msg);
        
        plugin.getServer().broadcastMessage("* " + name + " " + msg);
    }
    
    @Command(aliases = {"say"},
            usage = "<message...>", desc = "Send a message",
            min = 1, max = -1)
    @CommandPermissions({"commandbook.say"})
    public static void say(CommandContext args, CommandBookPlugin plugin,
            CommandSender sender) throws CommandException {
        
        if (sender instanceof Player && plugin.getAdminSession((Player) sender).isMute()) {
            sender.sendMessage(ChatColor.RED + "You are muted.");
            return;
        }
        
        String name = plugin.toColoredName(sender, ChatColor.WHITE);
        String msg = args.getJoinedStrings(0);
        
        if (sender instanceof Player) {
            PlayerChatEvent event = new PlayerChatEvent((Player) sender, msg);
            plugin.getServer().getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return;
            }
        }

        plugin.getServer().getPluginManager().callEvent(
                new CommandSenderMessageEvent(sender, msg));
        
        Logger.getLogger("Minecraft").info("<" + name + ">: " + msg);
        
        if (sender instanceof Player) {
            plugin.getServer().broadcastMessage(
                    "<" + plugin.toColoredName(sender, ChatColor.WHITE)
                    + "> " + args.getJoinedStrings(0));
        } else {
            plugin.getServer().broadcastMessage(
                    replaceColorMacros(plugin.consoleSayFormat).replace(
                            "%s", args.getJoinedStrings(0)));
        }
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

        receiver.sendMessage(ChatColor.GRAY + "(From "
                + plugin.toName(sender) + "): "
                + ChatColor.WHITE + message);
        
        sender.sendMessage(ChatColor.GRAY + "(To "
                + plugin.toName(receiver) + "): "
                + ChatColor.WHITE + message);
        
        System.out.println(plugin.toName(sender) + " told "
                + plugin.toName(receiver) + ": " + message);
        
        logger.info(plugin.toName(sender) + " told "
                + plugin.toName(receiver) + ": " + message);
        
        plugin.getSession(sender).setLastRecipient(receiver);
        
        // If the receiver hasn't had any player talk to them yet or hasn't
        // send a message, then we add it to the receiver's last message target
        // so s/he can /reply easily
        plugin.getSession(receiver).setNewLastRecipient(sender);
    }

    @Command(aliases = {"reply"},
            usage = "<message...>", desc = "Reply to last user",
            min = 1, max = -1)
    @CommandPermissions({"commandbook.msg"})
    public static void reply(CommandContext args, CommandBookPlugin plugin,
            CommandSender sender) throws CommandException {
        
        String message = args.getJoinedStrings(0);
        CommandSender receiver;
        
        String lastRecipient = plugin.getSession(sender).getLastRecipient();
        
        if (lastRecipient != null) {
            // This will throw errors as needed
            receiver = plugin.matchPlayerOrConsole(sender, lastRecipient);
        } else {
            sender.sendMessage(ChatColor.RED + "You haven't messaged anyone.");
            return;
        }

        receiver.sendMessage(ChatColor.GRAY + "(From "
                + plugin.toName(sender) + "): "
                + ChatColor.WHITE + message);
        
        sender.sendMessage(ChatColor.GRAY + "(To "
                + plugin.toName(receiver) + "): "
                + ChatColor.WHITE + message);
        
        logger.info(plugin.toName(sender) + " told "
                + plugin.toName(receiver) + ": " + message);
        
        // If the receiver hasn't had any player talk to them yet or hasn't
        // send a message, then we add it to the receiver's last message target
        // so s/he can /reply easily
        plugin.getSession(receiver).setNewLastRecipient(sender);
    }
}
