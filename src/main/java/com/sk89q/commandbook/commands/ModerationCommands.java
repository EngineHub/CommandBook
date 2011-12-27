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

import static com.sk89q.commandbook.CommandBookUtil.replaceColorMacros;

import com.sk89q.commandbook.util.PlayerUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;

public class ModerationCommands {

    private CommandBook plugin;
    
    public ModerationCommands(CommandBook plugin) {
        this.plugin = plugin;
    }
    


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
}
