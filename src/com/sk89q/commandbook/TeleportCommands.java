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

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;

public class TeleportCommands {
    
    @Command(aliases = {"spawn"},
            usage = "", desc = "Teleport to spawn",
            min = 0, max = 0)
    @CommandPermissions({"commandbook.spawn"})
    public static void spawn(CommandContext args, CommandBookPlugin plugin,
            CommandSender sender) throws CommandException {
        
        Player player = plugin.checkPlayer(sender);
        // Teleport the player!
        player.teleportTo(player.getWorld().getSpawnLocation());
    }
    
    @Command(aliases = {"teleport"},
            usage = "[target] <destination>", desc = "Teleport to a location",
            min = 1, max = 2)
    @CommandPermissions({"commandbook.teleport"})
    public static void teleport(CommandContext args, CommandBookPlugin plugin,
            CommandSender sender) throws CommandException {

        Iterable<Player> targets = null;
        Location loc = null;
        boolean included = false;
        
        // Detect arguments based on the number of arguments provided
        if (args.argsLength() == 1) {
            targets = plugin.matchPlayers(plugin.checkPlayer(sender));
            loc = plugin.matchLocation(sender, args.getString(0));
        } else if (args.argsLength() == 2) {            
            targets = plugin.matchPlayers(sender, args.getString(0));
            loc = plugin.matchLocation(sender, args.getString(1));
            
            // Check permissions!
            plugin.checkPermission(sender, "commandbook.teleport.other");
        }

        for (Player player : targets) {
            player.teleportTo(loc);
            
            // Tell the user
            if (player.equals(sender)) {
                player.sendMessage(ChatColor.YELLOW + "Teleported!");
                
                // Keep track of this
                included = true;
            } else {
                player.sendMessage(ChatColor.YELLOW + "You've been teleported by "
                        + plugin.toName(sender) + ".");
                
            }
        }
        
        // The player didn't receive any items, then we need to send the
        // user a message so s/he know that something is indeed working
        if (!included) {
            sender.sendMessage(ChatColor.YELLOW.toString() + "Players teleported.");
        }
    }
    
    @Command(aliases = {"bring"},
            usage = "<target>", desc = "Bring a player to you",
            min = 1, max = 1)
    @CommandPermissions({"commandbook.teleport.other"})
    public static void bring(CommandContext args, CommandBookPlugin plugin,
            CommandSender sender) throws CommandException {

        Iterable<Player> targets = plugin.matchPlayers(sender, args.getString(0));
        Location loc = plugin.checkPlayer(sender).getLocation();
        boolean included = false;

        for (Player player : targets) {
            player.teleportTo(loc);
            
            // Tell the user
            if (player.equals(sender)) {
                player.sendMessage(ChatColor.YELLOW + "Teleported!");
                
                // Keep track of this
                included = true;
            } else {
                player.sendMessage(ChatColor.YELLOW + "You've been teleported by "
                        + plugin.toName(sender) + ".");
                
            }
        }
        
        // The player didn't receive any items, then we need to send the
        // user a message so s/he know that something is indeed working
        if (!included) {
            sender.sendMessage(ChatColor.YELLOW.toString() + "Players teleported.");
        }
    }
    
    @Command(aliases = {"put"},
            usage = "<target>", desc = "Put a player at where you are looking",
            min = 1, max = 1)
    @CommandPermissions({"commandbook.teleport.other"})
    public static void put(CommandContext args, CommandBookPlugin plugin,
            CommandSender sender) throws CommandException {

        Iterable<Player> targets = plugin.matchPlayers(sender, args.getString(0));
        Location loc = plugin.matchLocation(sender, "#target");
        boolean included = false;

        for (Player player : targets) {
            Location playerLoc = player.getLocation();
            loc.setPitch(playerLoc.getPitch());
            loc.setYaw(playerLoc.getYaw());
            player.teleportTo(loc);
            
            // Tell the user
            if (player.equals(sender)) {
                player.sendMessage(ChatColor.YELLOW + "Teleported!");
                
                // Keep track of this
                included = true;
            } else {
                player.sendMessage(ChatColor.YELLOW + "You've been teleported by "
                        + plugin.toName(sender) + ".");
                
            }
        }
        
        // The player didn't receive any items, then we need to send the
        // user a message so s/he know that something is indeed working
        if (!included) {
            sender.sendMessage(ChatColor.YELLOW.toString() + "Players teleported.");
        }
    }
}
