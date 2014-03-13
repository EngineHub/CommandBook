/*
 * CommandBook
 * Copyright (C) 2011 sk89q <http://www.sk89q.com>
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

package com.sk89q.commandbook.util;

import com.sk89q.commandbook.locations.NamedLocation;
import com.sk89q.commandbook.locations.RootLocationManager;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.worldedit.blocks.BlockType;
import com.zachsthings.libcomponents.bukkit.BasePlugin;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LocationUtil {

    /**
     * Find a position for the player to stand that is not inside a block.
     * Blocks above the player will be iteratively tested until there is
     * a series of two free blocks. The player will be teleported to
     * that free position.
     *
     * @param searchPos search position
     * @return
     */
    public static Location findFreePosition(Location searchPos) {
        World world = searchPos.getWorld();
        Location loc = searchPos.clone();
        int x = searchPos.getBlockX();
        int y = Math.max(0, searchPos.getBlockY());
        int origY = y;
        int z = searchPos.getBlockZ();

        byte free = 0;

        while (y <= world.getMaxHeight() + 2) {
            if (BlockType.canPassThrough(world.getBlockTypeIdAt(x, y, z))) {
                free++;
            } else {
                free = 0;
            }

            if (free == 2) {
                if (y - 1 != origY) {
                    loc.setX(x + 0.5);
                    loc.setY(y - 1);
                    loc.setZ(z + 0.5);
                }

                return loc;
            }

            y++;
        }

        return null;
    }

    /**
     * Try to extract the world of a command sender.
     *
     * @param sender command sender
     * @return world or null
     */
    public static World extractWorld(CommandSender sender) {
        if (sender instanceof Player) {
            return ((Player) sender).getWorld();
        } else if (sender instanceof BlockCommandSender) {
            return ((BlockCommandSender) sender).getBlock().getWorld();
        } else {
            return BasePlugin.server().getWorlds().get(0);
        }
    }

    /**
     * Get a location from a location manager.
     *
     * @param manager RootLocationManager to look in
     * @param world
     * @param id name of the location
     * @return a Bukkit location
     * @throws CommandException if the location by said id does not exist
     */
    public static Location getManagedLocation(RootLocationManager<NamedLocation> manager,
                                       World world, String id) throws CommandException {
        NamedLocation loc = manager.get(world, id);
        if (loc == null) throw new CommandException("A location by that name could not be found.");
        return loc.getLocation();
    }

    /**
     * Get the 2D distance between two points a and b.
     *
     * @param a first location
     * @param b second location
     * @return squared 2D distance between the two points
     */
    public static double distanceSquared2D(Location a, Location b) {
        return Math.pow(a.getX() - b.getX(), 2) + Math.pow(a.getZ() - b.getZ(), 2);
    }
}
