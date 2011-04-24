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

import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.sk89q.commandbook.CommandBookPlugin;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.minecraft.util.commands.NestedCommand;

public class ModerationCommands {

    protected static final Logger logger = Logger.getLogger("Minecraft.CommandBook");

    /*@Command(aliases = {"freeze"},
            usage = "<target>", desc = "Freeze a player",
            min = 1, max = 1)
    @CommandPermissions({"commandbook.freeze"})
    public static void freeze(CommandContext args, CommandBookPlugin plugin,
            CommandSender sender) throws CommandException {
        
        Player player = plugin.matchSinglePlayer(sender, args.getString(0));
        plugin.freezePlayer(player);
        
        // Let's check if the player was frozen to begin with
        if (plugin.isFrozen(player)) {
            sender.sendMessage(ChatColor.RED + "That player is already frozen.");
            return;
        }

        player.sendMessage(ChatColor.YELLOW + "You've been frozen by "
                + plugin.toName(sender));
        sender.sendMessage(ChatColor.YELLOW + "You've frozen "
                + plugin.toName(player));
    

    @Command(aliases = {"unfreeze"},
            usage = "<target>", desc = "unFreeze a player",
            min = 1, max = 1)
    @CommandPermissions({"commandbook.freeze.unfreeze"})
    public static void unfreeze(CommandContext args, CommandBookPlugin plugin,
            CommandSender sender) throws CommandException {
        
        Player player = plugin.matchSinglePlayer(sender, args.getString(0));
        
        // Let's check if the player was frozen to begin with
        if (!sender.isFrozen(player)) {
            sender.sendMessage(ChatColor.RED + "That player is not frozen.");
            return;
        }
        
        plugin.unfreezePlayer(player);

        player.sendMessage(ChatColor.YELLOW + "You've been unfrozen by "
                + plugin.toName(sender));
        sender.sendMessage(ChatColor.YELLOW + "You've unfrozen "
                + plugin.toName(player));
    }*/
    
    @Command(aliases = {"mute"},
            usage = "<target>", desc = "Mute a player",
            min = 1, max = 1)
    @CommandPermissions({"commandbook.mute"})
    public static void mute(CommandContext args, CommandBookPlugin plugin,
            CommandSender sender) throws CommandException {
        
        Player player = plugin.matchSinglePlayer(sender, args.getString(0));

        plugin.getAdminSession(player).setMute(true);

        player.sendMessage(ChatColor.YELLOW + "You've been muted by "
                + plugin.toName(sender));
        sender.sendMessage(ChatColor.YELLOW + "You've muted "
                + plugin.toName(player));
    }
    
    @Command(aliases = {"unmute"},
            usage = "<target>", desc = "Unmute a player",
            min = 1, max = 1)
    @CommandPermissions({"commandbook.mute"})
    public static void unmute(CommandContext args, CommandBookPlugin plugin,
            CommandSender sender) throws CommandException {
        
        Player player = plugin.matchSinglePlayer(sender, args.getString(0));

        plugin.getAdminSession(player).setMute(false);

        player.sendMessage(ChatColor.YELLOW + "You've been unmuted by "
                + plugin.toName(sender));
        sender.sendMessage(ChatColor.YELLOW + "You've unmuted "
                + plugin.toName(player));
    }

    @Command(aliases = {"kick"},
            usage = "<target> [reason...]", desc = "Kick a user",
            min = 1, max = -1)
    @CommandPermissions({"commandbook.kick"})
    public static void kick(CommandContext args, CommandBookPlugin plugin,
            CommandSender sender) throws CommandException {
        
        Player player = plugin.matchSinglePlayer(sender, args.getString(0));
        String message = args.argsLength() >= 2 ? args.getJoinedStrings(1)
                : "Kicked!";
        
        player.kickPlayer(message);
        
        sender.sendMessage(ChatColor.YELLOW + player.getName()
                + " (" + player.getDisplayName() + ChatColor.WHITE + ") kicked.");
        
        plugin.getBanDatabase().logKick(player, sender, message);
    }

    @Command(aliases = {"ban"},
            usage = "<target> [reason...]", desc = "Ban a user",
            flags = "e", min = 1, max = -1)
    @CommandPermissions({"commandbook.bans.ban"})
    public static void ban(CommandContext args, CommandBookPlugin plugin,
            CommandSender sender) throws CommandException {
        
        String banName;
        String message = args.argsLength() >= 2 ? args.getJoinedStrings(1)
                : "Banned!";
        
        // Check if it's a player in the server right now
        try {
            Player player;
            
            // Exact mode matches names exactly
            if (args.hasFlag('e')) {
                player = plugin.matchPlayerExactly(sender, args.getString(0));
            } else {
                player = plugin.matchSinglePlayer(sender, args.getString(0));
            }
            
            // Need to kick + log
            player.kickPlayer(message);
            plugin.getBanDatabase().logKick(player, sender, message);
            
            banName = player.getName();
            
            sender.sendMessage(ChatColor.YELLOW + player.getName()
                    + " (" + player.getDisplayName() + ChatColor.WHITE
                    + ") banned and kicked.");
        } catch (CommandException e) {
            banName = args.getString(0)
                    .replace("\r", "")
                    .replace("\n", "")
                    .replace("\0", "")
                    .replace("\b", "");
            
            sender.sendMessage(ChatColor.YELLOW + banName
                    + " banned.");
        }
        
        plugin.getBanDatabase().banName(banName, sender, message);
        
        if (!plugin.getBanDatabase().save()) {
            sender.sendMessage(ChatColor.RED + "Bans database failed to save. See console.");
        }
    }
/*
    @Command(aliases = {"banip"},
            usage = "<target> [reason...]", desc = "Ban an IP address",
            min = 1, max = -1)
    @CommandPermissions({"commandbook.bans.ban.ip"})
    public static void banIP(CommandContext args, CommandBookPlugin plugin,
            CommandSender sender) throws CommandException {
        
        String message = args.argsLength() >= 2 ? args.getJoinedStrings(1)
                : "Banned!";
        
        String addr = args.getString(0)
                    .replace("\r", "")
                    .replace("\n", "")
                    .replace("\0", "")
                    .replace("\b", "");
        
        // Need to kick + log
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.getAddress().getAddress().getHostAddress().equals(addr)) {
                player.kickPlayer(message);
                plugin.getBanDatabase().logKick(player, sender, message);
            }
        }
        
        plugin.getBanDatabase().banAddress(addr, sender, message);
        
        sender.sendMessage(ChatColor.YELLOW + addr + " banned.");
        
        if (!plugin.getBanDatabase().save()) {
            sender.sendMessage(ChatColor.RED + "Bans database failed to save. See console.");
        }
    }
*/
    @Command(aliases = {"unban"},
            usage = "<target>", desc = "Unban a user",
            min = 1, max = -1)
    @CommandPermissions({"commandbook.bans.unban"})
    public static void unban(CommandContext args, CommandBookPlugin plugin,
            CommandSender sender) throws CommandException {
        
        String message = args.argsLength() >= 2 ? args.getJoinedStrings(1)
                : "Unbanned!";
        
        String banName = args.getString(0)
                    .replace("\r", "")
                    .replace("\n", "")
                    .replace("\0", "")
                    .replace("\b", "");
        
        if (plugin.getBanDatabase().unbanName(banName, sender, message)) {
            sender.sendMessage(ChatColor.YELLOW + banName + " unbanned.");
            
            if (!plugin.getBanDatabase().save()) {
                sender.sendMessage(ChatColor.RED + "Bans database failed to save. See console.");
            }
        } else {
            sender.sendMessage(ChatColor.RED + banName + " was not banned.");
        }
    }
/*
    @Command(aliases = {"unbanip"},
            usage = "<target> [reason...]", desc = "Unban an IP address",
            min = 1, max = -1)
    @CommandPermissions({"commandbook.bans.unban.ip"})
    public static void unbanIP(CommandContext args, CommandBookPlugin plugin,
            CommandSender sender) throws CommandException {

        String addr = args.getString(0)
                    .replace("\r", "")
                    .replace("\n", "")
                    .replace("\0", "")
                    .replace("\b", "");
        String message = args.argsLength() >= 2 ? args.getJoinedStrings(1)
                : "Unbanned!";
        
        if (plugin.getBanDatabase().unbanAddress(addr, sender, message)) {
            sender.sendMessage(ChatColor.YELLOW + addr + " unbanned.");
            
            if (!plugin.getBanDatabase().save()) {
                sender.sendMessage(ChatColor.RED + "Bans database failed to save. See console.");
            }
        } else {
            sender.sendMessage(ChatColor.RED + addr + " was not banned.");
        }
    }
*/
    @Command(aliases = {"isbanned"},
            usage = "<target>", desc = "Check if a user is banned",
            min = 1, max = 1)
    @CommandPermissions({"commandbook.bans.isbanned"})
    public static void isBanned(CommandContext args, CommandBookPlugin plugin,
            CommandSender sender) throws CommandException {
        
        String banName = args.getString(0)
                    .replace("\r", "")
                    .replace("\n", "")
                    .replace("\0", "")
                    .replace("\b", "");
        
        if (plugin.getBanDatabase().isBannedName(banName)) {
            sender.sendMessage(ChatColor.YELLOW + banName + " is banned.");
        } else {
            sender.sendMessage(ChatColor.YELLOW + banName + " NOT banned.");
        }
    }

    @Command(aliases = {"bans"}, desc = "Ban management")
    @NestedCommand({BanCommands.class})
    public static void bans(CommandContext args, CommandBookPlugin plugin,
            CommandSender sender) throws CommandException {
    }
}
