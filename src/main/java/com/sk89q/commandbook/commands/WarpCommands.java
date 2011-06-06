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

public class WarpCommands {
    
    @Command(aliases = {"warp"},
            usage = "[world] [target] <warp>", desc = "Teleport to a warp",
            min = 1, max = 3)
    @CommandPermissions({"commandbook.warp.teleport"})
    public static void warp(CommandContext args, CommandBookPlugin plugin,
            CommandSender sender) throws CommandException {

        Iterable<Player> targets = null;
        NamedLocation warp = null;
        Location loc = null;
        
        // Detect arguments based on the number of arguments provided
        if (args.argsLength() == 1) {
            Player player = plugin.checkPlayer(sender);
            targets = plugin.matchPlayers(player);
            warp = plugin.getWarpsManager().get(player.getWorld(), args.getString(0));
        } else if (args.argsLength() == 2) {            
            targets = plugin.matchPlayers(sender, args.getString(0));
            if (plugin.getWarpsManager().isPerWorld()) {
                Player player = plugin.checkPlayer(sender);
                warp = plugin.getWarpsManager().get(player.getWorld(), args.getString(1));
            } else {
                warp = plugin.getWarpsManager().get(null, args.getString(1));
            }
            
            // Check permissions!
            plugin.checkPermission(sender, "commandbook.warp.teleport.other");
        } else if (args.argsLength() == 3) {            
            targets = plugin.matchPlayers(sender, args.getString(1));
            warp = plugin.getWarpsManager().get(
                    plugin.matchWorld(sender, args.getString(0)), args.getString(2));
            
            // Check permissions!
            plugin.checkPermission(sender, "commandbook.warp.teleport.other");
        }
        
        if (warp != null) {
            loc = warp.getLocation();
        } else {
            throw new CommandException("A warp by the given name does not exist.");
        }

        (new TeleportPlayerIterator(plugin, sender, loc)).iterate(targets);
    }
    
    @Command(aliases = {"setwarp"},
            usage = "<warp> [location]", desc = "Set a warp",
            min = 1, max = 2)
    @CommandPermissions({"commandbook.warp.set"})
    public static void setWarp(CommandContext args, CommandBookPlugin plugin,
            CommandSender sender) throws CommandException {
        String warpName = args.getString(0);
        Location loc;
        Player player = null;
        
        // Detect arguments based on the number of arguments provided
        if (args.argsLength() == 1) {
            player = plugin.checkPlayer(sender);
            loc = player.getLocation();
        } else {            
            loc = plugin.matchLocation(sender, args.getString(1));
        }
        
        plugin.getWarpsManager().create(warpName, loc, player);
        
        sender.sendMessage(ChatColor.YELLOW + "Warp '" + warpName + "' created.");
    }

    @Command(aliases = {"warps"}, desc = "Warp management")
    @NestedCommand({WarpManagementCommands.class})
    public static void warps(CommandContext args, CommandBookPlugin plugin,
            CommandSender sender) throws CommandException {
        
    }
}
