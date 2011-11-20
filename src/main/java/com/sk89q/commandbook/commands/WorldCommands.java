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

import static com.sk89q.commandbook.CommandBookUtil.getCardinalDirection;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.sk89q.commandbook.CommandBookPlugin;
import com.sk89q.commandbook.CommandBookUtil;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;


public class WorldCommands {

    @Command(aliases = {"time"},
            usage = "[world] <time|\"current\">", desc = "Get/change the world time",
            flags = "l", min = 0, max = 2)
    public static void time(CommandContext args, CommandBookPlugin plugin,
            CommandSender sender) throws CommandException {
        
        World world;
        String timeStr;
        boolean onlyLock = false;
    
        // Easy way to get the time
        if (args.argsLength() == 0) {
            world = plugin.checkPlayer(sender).getWorld();
            timeStr = "current";
        // If no world was specified, get the world from the sender, but
        // fail if the sender isn't player
        } else if (args.argsLength() == 1) {
            world = plugin.checkPlayer(sender).getWorld();
            timeStr = args.getString(0);
        } else { // A world was specified!
            world = plugin.matchWorld(sender, args.getString(0));
            timeStr = args.getString(1);
        }
        
        // Let the player get the time
        if (timeStr.equalsIgnoreCase("current")
                || timeStr.equalsIgnoreCase("cur")
                || timeStr.equalsIgnoreCase("now")) {
            
            // We want to lock to the current time
            if (!args.hasFlag('l')) {
                plugin.checkPermission(sender, "commandbook.time.check");
                sender.sendMessage(ChatColor.YELLOW
                        + "Time: " + CommandBookUtil.getTimeString(world.getTime()));
                return;
            }
            
            onlyLock = true;
        }
        
        plugin.checkPermission(sender, "commandbook.time");
    
        if (!onlyLock) {
            plugin.getTimeLockManager().unlock(world);
            world.setTime(plugin.matchTime(timeStr));
        }
        
        String verb = "set";
        
        // Locking
        if (args.hasFlag('l')) {
            plugin.checkPermission(sender, "commandbook.time.lock");
            plugin.getTimeLockManager().lock(world);
            verb = "locked";
        }
        
        if (plugin.broadcastChanges) { 
            plugin.getServer().broadcastMessage(ChatColor.YELLOW
                    + plugin.toName(sender) + " " + verb + " the time of '"
                    + world.getName() + "' to "
                    + CommandBookUtil.getTimeString(world.getTime()) + ".");
        } else {
            sender.sendMessage(ChatColor.YELLOW + "Time " + verb + " to "
                    + CommandBookUtil.getTimeString(world.getTime()) + ".");
        }
    }

    @Command(aliases = {"compass"},
            usage = "[player]", desc = "Show your current compass direction",
            flags = "", min = 0, max = 1)
    @CommandPermissions({"commandbook.whereami.compass.other"})
    public static void compass(CommandContext args, CommandBookPlugin plugin,
            CommandSender sender) throws CommandException {
    
        Player player;
        
        if (args.argsLength() == 0) {
            player = plugin.checkPlayer(sender);
        } else {
            plugin.checkPermission(sender, "commandbook.whereami.other");
            
            player = plugin.matchSinglePlayer(sender, args.getString(0));
        }
    
        sender.sendMessage(ChatColor.YELLOW +
                String.format("Your direction: %s",
                        getCardinalDirection(player)));
    }

    @Command(aliases = {"biome"},
            usage = "[player]", desc = "Get your current biome",
            flags = "", min = 0, max = 1)
    @CommandPermissions({"commandbook.biome"})
    public static void biome(CommandContext args, CommandBookPlugin plugin,
            CommandSender sender) throws CommandException {
        
        Player player;
    
        if (args.argsLength() == 0) {
            player = plugin.checkPlayer(sender);
        } else {
            plugin.checkPermission(sender, "commandbook.biome.other");
            
            player = plugin.matchSinglePlayer(sender, args.getString(0));
        }
    
        sender.sendMessage(ChatColor.YELLOW + player.getLocation().getBlock().getBiome().name().toLowerCase().replace("_"," ")+" biome.");
    
    }

    @Command(aliases = {"setspawn"},
            usage = "[location]", desc = "Change spawn location",
            flags = "", min = 0, max = 1)
    @CommandPermissions({"commandbook.setspawn"})
    public static void setspawn(CommandContext args, CommandBookPlugin plugin,
            CommandSender sender) throws CommandException {
        
        World world;
        Location loc;
        
        if (args.argsLength() == 0) {
            Player player = plugin.checkPlayer(sender);
            world = player.getWorld();
            loc = player.getLocation();
        } else {
            loc = plugin.matchLocation(sender, args.getString(0));
            world = loc.getWorld();
        }
    
        plugin.getSpawnManager().setWorldSpawn(loc);
    
        sender.sendMessage(ChatColor.YELLOW +
                "Spawn location of '" + world.getName() + "' set!");
    }

    @Command(aliases = {"weather"},
            usage = "<'stormy'|'sunny'> [duration] [world]", desc = "Change the world weather",
            min = 1, max = 3)
    @CommandPermissions({"commandbook.weather"})
    public static void weather(CommandContext args, CommandBookPlugin plugin,
            CommandSender sender) throws CommandException {
        
        World world;
        String weatherStr = args.getString(0);
        int duration = -1;
    
        if (args.argsLength() == 1) {
            world = plugin.checkPlayer(sender).getWorld();
        } else if (args.argsLength() == 2) {
            world = plugin.checkPlayer(sender).getWorld();
            duration = args.getInteger(1);
        } else { // A world was specified!
            world = plugin.matchWorld(sender, args.getString(2));
            duration = args.getInteger(1);
        }
        
        if (weatherStr.equalsIgnoreCase("stormy")
                || weatherStr.equalsIgnoreCase("rainy")
                || weatherStr.equalsIgnoreCase("snowy")
                || weatherStr.equalsIgnoreCase("rain")
                || weatherStr.equalsIgnoreCase("snow")
                || weatherStr.equalsIgnoreCase("on")) {
            
            world.setStorm(true);
            
            if (duration > 0) {
                world.setWeatherDuration(duration * 20);
            }
    
            if (plugin.broadcastChanges) { 
                plugin.getServer().broadcastMessage(ChatColor.YELLOW
                        + plugin.toName(sender) + " has started on a storm on '"
                        + world.getName() + "'.");
            }
            
            // Tell console, since console won't get the broadcast message.
            if (!plugin.broadcastChanges) {
                sender.sendMessage(ChatColor.YELLOW + "Stormy weather enabled.");
            }
            
        } else if (weatherStr.equalsIgnoreCase("clear")
                || weatherStr.equalsIgnoreCase("sunny")
                || weatherStr.equalsIgnoreCase("snowy")
                || weatherStr.equalsIgnoreCase("rain")
                || weatherStr.equalsIgnoreCase("snow")
                || weatherStr.equalsIgnoreCase("off")) {
            
            world.setStorm(false);
            
            if (duration > 0) {
                world.setWeatherDuration(duration * 20);
            }
    
            if (plugin.broadcastChanges) { 
                plugin.getServer().broadcastMessage(ChatColor.YELLOW
                        + plugin.toName(sender) + " has stopped a storm on '"
                        + world.getName() + "'.");
            }
            
            // Tell console, since console won't get the broadcast message.
            if (!plugin.broadcastChanges) {
                sender.sendMessage(ChatColor.YELLOW + "Stormy weather disabled.");
            }
            
        } else {
            throw new CommandException("Unknown weather state! Acceptable states: sunny or stormy");
        }
    }

    @Command(aliases = {"thunder"},
            usage = "<'on'|'off'> [duration] [world]", desc = "Change the thunder state",
            min = 1, max = 3)
    @CommandPermissions({"commandbook.weather.thunder"})
    public static void thunder(CommandContext args, CommandBookPlugin plugin,
            CommandSender sender) throws CommandException {
        
        World world;
        String weatherStr = args.getString(0);
        int duration = -1;
    
        if (args.argsLength() == 1) {
            world = plugin.checkPlayer(sender).getWorld();
        } else if (args.argsLength() == 2) {
            world = plugin.checkPlayer(sender).getWorld();
            duration = args.getInteger(1);
        } else { // A world was specified!
            world = plugin.matchWorld(sender, args.getString(2));
            duration = args.getInteger(1);
        }
        
        if (weatherStr.equalsIgnoreCase("on")) {
            world.setThundering(true);
            
            if (duration > 0) {
                world.setThunderDuration(duration * 20);
            }
            
            sender.sendMessage(ChatColor.YELLOW + "Thunder enabled.");
        } else if (weatherStr.equalsIgnoreCase("off")) {
            world.setThundering(false);
            
            if (duration > 0) {
                world.setThunderDuration(duration * 20);
            }
    
            sender.sendMessage(ChatColor.YELLOW + "Thunder disabled.");
        } else {
            throw new CommandException("Unknown thunder state! Acceptable states: on or off");
        }
    }

}
