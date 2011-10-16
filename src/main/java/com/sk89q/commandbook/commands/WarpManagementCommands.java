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

import com.sk89q.commandbook.CommandBookPlugin;
import com.sk89q.commandbook.locations.NamedLocation;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

public class WarpManagementCommands {

    @Command(
            aliases = {"del", "delete", "remove", "rem"},
            usage = "<warpname> [world]",
            desc = "Remove a warp",
            min = 1, max = 2 )
    @CommandPermissions({"commandbok.warp.remove"})
    public static void remove(CommandContext args, CommandBookPlugin plugin,
        CommandSender sender) throws CommandException {
        World world;
        String warpName = args.getString(0);
        if (args.argsLength() == 2) {
            world = plugin.matchWorld(sender, args.getString(1));
        } else {
            world = plugin.checkPlayer(sender).getWorld();
        }
        NamedLocation warp = plugin.getWarpsManager().get(world, warpName);
        if (warp == null) {
            throw new CommandException("No warp named " + warpName + " found for world " + world.getName());
        }
        if (!warp.getCreatorName().equals(sender.getName())) {
            plugin.checkPermission(sender, "commandbook.warp.remove.other");
        }

        plugin.getWarpsManager().remove(world, warpName);
        sender.sendMessage(ChatColor.YELLOW + "Warp " + warpName + " removed.");
    }
}
