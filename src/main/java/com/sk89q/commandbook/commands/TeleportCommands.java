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

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.sk89q.commandbook.CommandBookPlugin;
import com.sk89q.commandbook.util.PlayerIteratorAction;
import com.sk89q.commandbook.util.TeleportPlayerIterator;
import com.sk89q.minecraft.util.commands.*;

public class TeleportCommands {
    
    @Command(aliases = {"spawn"},
            usage = "[player]", desc = "Teleport to spawn", 
            flags = "m",
            min = 0, max = 1)
    @CommandPermissions({"commandbook.spawn"})
    public static void spawn(CommandContext args, final CommandBookPlugin plugin,
            CommandSender sender) throws CommandException {

        Iterable<Player> targets = null;
        
        // Detect arguments based on the number of arguments provided
        if (args.argsLength() == 1) {
            targets = plugin.matchPlayers(sender, args.getString(0));
            
            // Check permissions!
            plugin.checkPermission(sender, "commandbook.spawn.other");
        } else {
            targets = plugin.matchPlayers(plugin.checkPlayer(sender));
        }
        
        if (args.hasFlag('m')) {
      
            (new PlayerIteratorAction(plugin, sender) {
            
                @Override
                public void perform(Player player) {
                    player.teleport(plugin.getSpawnManager().getWorldSpawn(plugin.getServer().getWorlds().get(0)));
                }

                @Override
                public void onCaller(Player player) {
                    player.sendMessage(ChatColor.YELLOW + "Teleported to the main spawn.");
                }
            
                @Override
                public void onVictim(CommandSender sender, Player player) {
                    player.sendMessage(ChatColor.YELLOW + "Teleported to the main spawn by "
                            + plugin.toName(sender) + ".");
                }
            
                @Override
                public void onInformMany(CommandSender sender, int affected) {
                    sender.sendMessage(ChatColor.YELLOW.toString()
                            + affected + " teleported to the main spawn.");
                }
            
            }).iterate(targets);
        
        } else {
            
            (new PlayerIteratorAction(plugin, sender) {
            
                @Override
                public void perform(Player player) {
                    player.teleport(plugin.getSpawnManager().getWorldSpawn(player.getWorld()));
                }

                @Override
                public void onCaller(Player player) {
                    player.sendMessage(ChatColor.YELLOW + "Teleported to spawn.");
                }
            
                @Override
                public void onVictim(CommandSender sender, Player player) {
                    player.sendMessage(ChatColor.YELLOW + "Teleported to spawn by "
                            + plugin.toName(sender) + ".");
                }
            
                @Override
                public void onInformMany(CommandSender sender, int affected) {
                    sender.sendMessage(ChatColor.YELLOW.toString()
                            + affected + " teleported to spawn.");
                }
            
            }).iterate(targets);
        }
    }
    
    @Command(aliases = {"teleport"},
            usage = "[target] <destination>", desc = "Teleport to a location",
            min = 1, max = 2)
    @CommandPermissions({"commandbook.teleport"})
    public static void teleport(CommandContext args, final CommandBookPlugin plugin,
            CommandSender sender) throws CommandException {

        Iterable<Player> targets = null;
        final Location loc;
        
        // Detect arguments based on the number of arguments provided
        if (args.argsLength() == 1) {
            targets = plugin.matchPlayers(plugin.checkPlayer(sender));
            loc = plugin.matchLocation(sender, args.getString(0));
            if (sender instanceof Player && ((Player) sender).getLocation().getWorld().getName().equals(loc.getWorld().getName())) {
                plugin.checkPermission(sender, loc.getWorld(), "commandbook.teleport");
            }
        } else {
            targets = plugin.matchPlayers(sender, args.getString(0));
            loc = plugin.matchLocation(sender, args.getString(1));
            
            // Check permissions!
            plugin.checkPermission(sender, "commandbook.teleport.other");
            if (sender instanceof Player && ((Player) sender).getLocation().getWorld().getName().equals(loc.getWorld().getName())) {
                plugin.checkPermission(sender, loc.getWorld(), "commandbook.teleport.other");
            }
        }
        
        (new TeleportPlayerIterator(plugin, sender, loc)).iterate(targets);
    }
    
    @Command(aliases = {"call"},
            usage = "<target>", desc = "Request a teleport",
            min = 1, max = 1)
    @CommandPermissions({"commandbook.call"})
    public static void requestTeleport(CommandContext args, final CommandBookPlugin plugin,
            CommandSender sender) throws CommandException {

        Player player = plugin.checkPlayer(sender);
        Player target = plugin.matchSinglePlayer(sender, args.getString(0));

        if (!player.getWorld().getName().equals(target.getWorld().getName())) {
            plugin.checkPermission(sender, player.getWorld(), "commandbook.call");
        }

        plugin.getSession(player).checkLastTeleportRequest(target);
        plugin.getSession(target).addBringable(player);

        sender.sendMessage(ChatColor.YELLOW.toString() + "Teleport request sent.");
        target.sendMessage(ChatColor.AQUA + "**TELEPORT** " + plugin.toName(sender)
                + " requests a teleport! Use /bring <name> to accept.");
    }
    
    @Command(aliases = {"bring"},
            usage = "<target>", desc = "Bring a player to you",
            min = 1, max = 1)
    public static void bring(CommandContext args, final CommandBookPlugin plugin,
            CommandSender sender) throws CommandException {

        Player player = plugin.checkPlayer(sender);
        if (!plugin.hasPermission(sender, "commandbook.teleport.other")) {
            Player target = plugin.matchSinglePlayer(sender, args.getString(0));

            if (plugin.getSession(player).isBringable(target)) {
                sender.sendMessage(ChatColor.YELLOW + "Player teleported.");
                target.sendMessage(ChatColor.YELLOW + "Your teleport request to "
                        + plugin.toName(sender) + " was accepted.");
                target.teleport(player);
            } else {
                throw new CommandException("That person didn't request a " +
                        "teleport (recently) and you don't have " +
                        "permission to teleport anyone.");
            }

            return;
        }

        Iterable<Player> targets = plugin.matchPlayers(sender, args.getString(0));
        Location loc = player.getLocation();

        (new TeleportPlayerIterator(plugin, sender, loc) {
            @Override
            public void perform(Player player) {
                if (sender instanceof Player) {
                    if (!player.getWorld().getName().equals(((Player) sender).getWorld().getName())) {
                        if (!plugin.hasPermission(sender, player.getWorld(), "commandbook.teleport.other")) {
                            return;
                        }
                    }
                }
                oldLoc = player.getLocation();
                player.teleport(loc);
            }
        }).iterate(targets);
    }
    
    @Command(aliases = {"put"},
            usage = "<target>", desc = "Put a player at where you are looking",
            min = 1, max = 1)
    @CommandPermissions({"commandbook.teleport.other"})
    public static void put(CommandContext args, final CommandBookPlugin plugin,
            CommandSender sender) throws CommandException {

        Iterable<Player> targets = plugin.matchPlayers(sender, args.getString(0));
        Location loc = plugin.matchLocation(sender, "#target");
        
        (new TeleportPlayerIterator(plugin, sender, loc) {
            @Override
            public void perform(Player player) {
                oldLoc = player.getLocation();
                
                Location playerLoc = player.getLocation();
                loc.setPitch(playerLoc.getPitch());
                loc.setYaw(playerLoc.getYaw());
                player.teleport(loc);
            }
            
        }).iterate(targets);
    }
    
    @Command(aliases = {"return"},
            usage = "", desc = "Teleport back to your last location",
            min = 0, max = 0)
    @CommandPermissions({"commandbook.return"})
    public static void ret(CommandContext args, final CommandBookPlugin plugin,
            CommandSender sender) throws CommandException {
        
        Player player = plugin.checkPlayer(sender);
        Location lastLoc = plugin.getSession(player).popLastLocation();

        if (lastLoc != null) {
            plugin.getSession(player).setIgnoreLocation(lastLoc);
            player.teleport(lastLoc);
            sender.sendMessage(ChatColor.YELLOW + "You've been returned.");
        } else {
            sender.sendMessage(ChatColor.RED + "There's no past location in your history.");
        }
    }
}
