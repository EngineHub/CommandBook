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

package com.sk89q.commandbook;

import java.util.Random;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;

public class AdministratorCommands {
    
    protected static Random random = new Random();
    
    @Command(aliases = {"slap"},
            usage = "[target]", desc = "Slap a player", flags = "hdvp",
            min = 0, max = 1)
    @CommandPermissions({"commandbook.slap"})
    public static void slap(CommandContext args, CommandBookPlugin plugin,
            CommandSender sender) throws CommandException {

        Iterable<Player> targets = null;
        boolean included = false;
        int count = 0;
        
        // Detect arguments based on the number of arguments provided
        if (args.argsLength() == 0) {
            targets = plugin.matchPlayers(plugin.checkPlayer(sender));
        } else if (args.argsLength() == 1) {            
            targets = plugin.matchPlayers(sender, args.getString(0));
            
            // Check permissions!
            plugin.checkPermission(sender, "commandbook.slap.other");
        }

        for (Player player : targets) {
            count++;
            
            if (args.hasFlag('v')) {
                player.setVelocity(new Vector(
                        random.nextDouble() * 10.0 - 5,
                        random.nextDouble() * 10,
                        random.nextDouble() * 10.0 - 5));
            } else if (args.hasFlag('h')) {
                player.setVelocity(new Vector(
                        random.nextDouble() * 5.0 - 2.5,
                        random.nextDouble() * 5,
                        random.nextDouble() * 5.0 - 2.5));
            } else {
                player.setVelocity(new Vector(
                        random.nextDouble() * 2.0 - 1,
                        random.nextDouble() * 1,
                        random.nextDouble() * 2.0 - 1));
            }
            
            if (args.hasFlag('d')) {
                player.setHealth(player.getHealth() - 1);
            }

            if (args.hasFlag('p')) {
                // Tell the user
                if (player.equals(sender)) {
                    player.sendMessage(ChatColor.YELLOW + "Slapped!");
                    
                    // Keep track of this
                    included = true;
                } else {
                    player.sendMessage(ChatColor.YELLOW + "You've been slapped by "
                            + plugin.toName(sender) + ".");
                    
                }
            } else {
                if (count < 6) {
                    plugin.getServer().broadcastMessage(
                            ChatColor.YELLOW + plugin.toName(sender)
                            + " slapped " + plugin.toName(sender));
                } else if (count == 6) {
                    plugin.getServer().broadcastMessage(
                            ChatColor.YELLOW + plugin.toName(sender)
                            + " slapped more people...");
                }
            }
        }
        
        // The player didn't receive any items, then we need to send the
        // user a message so s/he know that something is indeed working
        if (!included && args.hasFlag('p')) {
            sender.sendMessage(ChatColor.YELLOW.toString() + "Players slapped.");
        }
    }
    
    @Command(aliases = {"rocket"},
            usage = "[target]", desc = "Rocket a player", flags = "hp",
            min = 0, max = 1)
    @CommandPermissions({"commandbook.rocket"})
    public static void rocket(CommandContext args, CommandBookPlugin plugin,
            CommandSender sender) throws CommandException {

        Iterable<Player> targets = null;
        boolean included = false;
        int count = 0;
        
        // Detect arguments based on the number of arguments provided
        if (args.argsLength() == 0) {
            targets = plugin.matchPlayers(plugin.checkPlayer(sender));
        } else if (args.argsLength() == 1) {            
            targets = plugin.matchPlayers(sender, args.getString(0));
            
            // Check permissions!
            plugin.checkPermission(sender, "commandbook.rocket.other");
        }

        for (Player player : targets) {
            if (args.hasFlag('h')) {
                player.setVelocity(new Vector(0, 50, 0));
            } else {
                player.setVelocity(new Vector(0, 20, 0));
            }

            if (args.hasFlag('p')) {
                // Tell the user
                if (player.equals(sender)) {
                    player.sendMessage(ChatColor.YELLOW + "Rocketed!");
                    
                    // Keep track of this
                    included = true;
                } else {
                    player.sendMessage(ChatColor.YELLOW + "You've been rocketed by "
                            + plugin.toName(sender) + ".");
                    
                }
            } else {
                if (count < 6) {
                    plugin.getServer().broadcastMessage(
                            ChatColor.YELLOW + plugin.toName(sender)
                            + " rocketed " + plugin.toName(sender));
                } else if (count == 6) {
                    plugin.getServer().broadcastMessage(
                            ChatColor.YELLOW + plugin.toName(sender)
                            + " rocketed more people...");
                }
            }
        }
        
        // The player didn't receive any items, then we need to send the
        // user a message so s/he know that something is indeed working
        if (!included && args.hasFlag('p')) {
            sender.sendMessage(ChatColor.YELLOW.toString() + "Players rocketed.");
        }
    }
    
}
