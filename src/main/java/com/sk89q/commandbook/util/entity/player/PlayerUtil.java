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

package com.sk89q.commandbook.util.entity.player;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.minecraft.util.commands.CommandException;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class PlayerUtil {

    /**
     * Checks to see if the sender is a player, otherwise throw an exception.
     *
     * @param sender
     * @return
     * @throws com.sk89q.minecraft.util.commands.CommandException
     */
    public static Player checkPlayer(CommandSender sender) throws CommandException {
        if (sender instanceof Player) {
            return (Player) sender;
        } else {
            throw new CommandException("A player context is required. (Specify a world or player if the command supports it.)");
        }
    }

    /**
     * Teleports a player with vehicle support
     *
     * @param sender
     * @param player
     * @param target
     * @param allowVehicles
     */
    public static boolean teleportTo(CommandSender sender, Player player, Location target, boolean allowVehicles) {
        target.getChunk().load(true);
        if (player.getVehicle() != null) {
            Entity vehicle = player.getVehicle();
            vehicle.eject();

            boolean success = player.teleport(target);

            if (!allowVehicles) {
                return success;
            }

            // Check vehicle permissions
            String permString = "commandbook.teleport.vehicle." + vehicle.getType().getName().toLowerCase();

            if (CommandBook.inst().hasPermission(player, permString)) {
                if (player.getWorld().equals(target.getWorld())
                        || CommandBook.inst().hasPermission(player, target.getWorld(), permString)) {
                    success = success && vehicle.teleport(player);
                    vehicle.setPassenger(player);
                }
            }
            return success;
        } else {
            return player.teleport(target);
        }
    }
}
