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

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.sk89q.commandbook.CommandBookPlugin;
import com.sk89q.commandbook.locations.NamedLocation;
import com.sk89q.commandbook.util.TeleportPlayerIterator;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.minecraft.util.commands.NestedCommand;

public class HomeCommands {
    
    @Command(aliases = {"home"},
            usage = "[world] [target] [owner]", desc = "Teleport to a home",
            min = 0, max = 3)
    @CommandPermissions({"commandbook.home.teleport"})
    public static void home(CommandContext args, CommandBookPlugin plugin,
            CommandSender sender) throws CommandException {

        Iterable<Player> targets = null;
        NamedLocation home = null;
        Location loc = null;
        
        // Detect arguments based on the number of arguments provided
        if (args.argsLength() == 0) {
            Player player = plugin.checkPlayer(sender);
            targets = plugin.matchPlayers(player);
            home = plugin.getHomesManager().get(player.getWorld(), player.getName());
        } else if (args.argsLength() == 1) {
            Player player = plugin.checkPlayer(sender);
            targets = plugin.matchPlayers(player);
            home = plugin.getHomesManager().get(player.getWorld(), args.getString(0));
            
            // Check permissions!
            plugin.checkPermission(sender, "commandbook.home.other");
        } else if (args.argsLength() == 2) {            
            targets = plugin.matchPlayers(sender, args.getString(0));
            if (plugin.getHomesManager().isPerWorld()) {
                Player player = plugin.checkPlayer(sender);
                home = plugin.getHomesManager().get(player.getWorld(), args.getString(1));
            } else {
                home = plugin.getHomesManager().get(null, args.getString(1));
            }
            
            // Check permissions!
            plugin.checkPermission(sender, "commandbook.home.teleport.other");
            plugin.checkPermission(sender, "commandbook.home.other");
        } else if (args.argsLength() == 3) {            
            targets = plugin.matchPlayers(sender, args.getString(1));
            home = plugin.getHomesManager().get(
                    plugin.matchWorld(sender, args.getString(0)), args.getString(2));
            
            // Check permissions!
            plugin.checkPermission(sender, "commandbook.home.teleport.other");
            plugin.checkPermission(sender, "commandbook.home.other");
        }
        
        if (home != null) {
            loc = home.getLocation();
        } else {
            throw new CommandException("A home for the given player does not exist.");
        }

        (new TeleportPlayerIterator(plugin, sender, loc)).iterate(targets);
    }
    
    @Command(aliases = {"sethome"},
            usage = "[owner] [location]", desc = "Set a home",
            min = 0, max = 2)
    @CommandPermissions({"commandbook.home.set"})
    public static void setHome(CommandContext args, CommandBookPlugin plugin,
            CommandSender sender) throws CommandException {
        String homeName;
        Location loc;
        Player player = null;
        
        // Detect arguments based on the number of arguments provided
        if (args.argsLength() == 0) {
            player = plugin.checkPlayer(sender);
            homeName = player.getName();
            loc = player.getLocation();
        } else if (args.argsLength() == 1) {
            homeName = args.getString(1);
            player = plugin.checkPlayer(sender);
            loc = player.getLocation();
            
            // Check permissions!
            plugin.checkPermission(sender, "commandbook.home.set.other");
        } else {
            homeName = args.getString(1);
            loc = plugin.matchLocation(sender, args.getString(0));
            
            // Check permissions!
            plugin.checkPermission(sender, "commandbook.home.set.other");
        }
        
        plugin.getHomesManager().create(homeName, loc, player);
        
        sender.sendMessage(ChatColor.YELLOW + "Home set.");
    }

    @Command(aliases = {"homes"}, desc = "Home management")
    @NestedCommand({HomeManagementCommands.class})
    public static void homes(CommandContext args, CommandBookPlugin plugin,
            CommandSender sender) throws CommandException {
        
    }
}
