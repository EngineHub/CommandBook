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

import java.util.List;

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

    private static final int PER_PAGE = 5;
    @Command(
            aliases = {"list", "show"},
            usage = "[ -p owner] [-w world] [page]",
            desc = "List warps",
            flags = "p:w:", min = 0, max = 1 )
    @CommandPermissions({"commandbook.warp.list"})
    public static void list(CommandContext args, CommandBookPlugin plugin,
        CommandSender sender) throws CommandException {
        World world = null;
        String owner = args.getFlag('p');
        int page = args.getInteger(0, 1) - 1;
        if (plugin.getWarpsManager().isPerWorld()) {
            if (args.hasFlag('w')) {
                world = plugin.matchWorld(sender, args.getFlag('w'));
            } else {
                world = plugin.checkPlayer(sender).getWorld();
            }
        }
        List<NamedLocation> locations = plugin.getWarpsManager().getLocations(world);
        for (int i = 0; i < locations.size(); i++) {
            if (owner != null &&
                    !locations.get(i).getCreatorName().equals(owner)) {
                locations.remove(i);
                i--;
            }
        }
        if (locations.size() == 0) throw new CommandException("No warps match!");

        int maxPages = locations.size() / PER_PAGE;
        if (page < 0 || page > maxPages) throw new CommandException(
                "Unknown page selected! " + maxPages + " total pages.");

        sender.sendMessage(ChatColor.YELLOW +
                "Name - Owner - World  - Location (page " + (page + 1) + "/" + (maxPages + 1) + ")");
        String defaultWorld = plugin.getServer().getWorlds().get(0).getName();
        for (int i = PER_PAGE * page; i < PER_PAGE * page + PER_PAGE  && i < locations.size(); i++) {
            NamedLocation loc = locations.get(i);
            sender.sendMessage(ChatColor.YELLOW.toString() + loc.getName()
                    + " - " + loc.getCreatorName()
                    + " - " + (loc.getWorldName() == null ? defaultWorld : loc.getWorldName())
                    + " - " + loc.getLocation().getBlockX() + "," + loc.getLocation().getBlockY()
                    + "," + loc.getLocation().getBlockZ());
        }
    }
}
