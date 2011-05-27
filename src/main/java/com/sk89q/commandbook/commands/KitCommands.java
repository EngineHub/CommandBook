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

import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.sk89q.commandbook.CommandBookPlugin;
import com.sk89q.commandbook.kits.Kit;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;

public class KitCommands {
    
    @Command(aliases = {"kit"},
            usage = "<id> [target]", desc = "Get a kit",
            flags = "", min = 0, max = 2)
    public static void kit(CommandContext args, CommandBookPlugin plugin,
            CommandSender sender) throws CommandException {
        
        // List kits
        if (args.argsLength() == 0) {
            plugin.checkPermission(sender, "commandbook.kit.list");
            
            Map<String, Kit> kits = plugin.getKitManager().getKits();
            
            if (kits.size() == 0) {
                sender.sendMessage(ChatColor.RED + "No kits are configured.");
                return;
            }
            
            StringBuilder str = new StringBuilder();
            int count = 0;
            
            for (String id : kits.keySet()) {
                if (!plugin.hasPermission(sender, 
                        "commandbook.kit.kits." + id.replace(".", ""))) {
                    continue;
                }
                
                if (str.length() != 0) {
                    str.append(", ");
                }
                
                str.append(id);
                count++;
            }
            
            if (count == 0) {
                sender.sendMessage(ChatColor.RED + "You have access to no kits.");
                return;
            }

            sender.sendMessage(ChatColor.YELLOW + "Kits (" + count + "): "
                    + ChatColor.WHITE + str.toString());
            sender.sendMessage(ChatColor.YELLOW + "Use /kit kitname to get a kit.");
        
        // Give a kit
        } else {
            Iterable<Player> targets;
            String id = args.getString(0).toLowerCase();
            boolean included = false;
            
            if (args.argsLength() == 2) {
                plugin.checkPermission(sender, "commandbook.kit.other");
                
                targets = plugin.matchPlayers(sender, args.getString(1));
            } else {
                targets = plugin.matchPlayers(plugin.checkPlayer(sender));
            }
            
            Kit kit = plugin.getKitManager().getKit(id);
            
            if (kit == null) {
                sender.sendMessage(ChatColor.RED + "No kit by that name exists.");
                return;
            }
            
            plugin.checkPermission(sender, "commandbook.kit.kits." + id.replace(".", ""));
    
            for (Player player : targets) {
                boolean success = kit.distribute(player);
                
                // Tell the user
                if (player.equals(sender)) {
                    if (success) {
                        player.sendMessage(ChatColor.YELLOW + "Kit '" + id + "' given!");
                    } else {
                        player.sendMessage(ChatColor.RED + "You have to wait before you can get this kit again.");
                    }
                    
                    included = true;
                } else {
                    if (success) {
                        player.sendMessage(ChatColor.YELLOW + "You've been given " +
                                "the '" + id + "' kit by "
                                + plugin.toName(sender) + ".");
                    } else {
                        player.sendMessage(ChatColor.RED + "A kit could not be given to you because it has been too soon.");
                    }
                    
                }
            }
            
            // The player didn't receive any items, then we need to send the
            // user a message so s/he know that something is indeed working
            if (!included) {
                sender.sendMessage(ChatColor.YELLOW.toString() + "Kits given.");
            }
        }
    }
}
