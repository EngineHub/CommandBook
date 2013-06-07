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

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.CommandBookUtil;
import com.sk89q.commandbook.locations.*;
import com.sk89q.minecraft.util.commands.CommandException;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

import static com.sk89q.commandbook.util.PlayerUtil.*;

public class LocationUtil {
    // No instantiation here...
    private LocationUtil() {}

    /**
     * Match a world.
     * @param sender
     *
     * @param filter
     * @return
     * @throws com.sk89q.minecraft.util.commands.CommandException
     */
    public static World matchWorld(CommandSender sender, String filter) throws CommandException {
        List<World> worlds = CommandBook.server().getWorlds();

        // Handle special hash tag groups
        if (filter.charAt(0) == '#') {
            // #main for the main world
            if (filter.equalsIgnoreCase("#main")) {
                return worlds.get(0);

                // #normal for the first normal world
            } else if (filter.equalsIgnoreCase("#normal")) {
                for (World world : worlds) {
                    if (world.getEnvironment() == World.Environment.NORMAL) {
                        return world;
                    }
                }

                throw new CommandException("No normal world found.");

                // #nether for the first nether world
            } else if (filter.equalsIgnoreCase("#nether")) {
                for (World world : worlds) {
                    if (world.getEnvironment() == World.Environment.NETHER) {
                        return world;
                    }
                }

                throw new CommandException("No nether world found.");

                // #skylands for the first skylands world
            } else if (filter.equalsIgnoreCase("#skylands") || filter.equalsIgnoreCase("#theend") || filter.equalsIgnoreCase("#end")) {
                World.Environment skylandsEnv = CommandBookUtil.getSkylandsEnvironment();
                for (World world : worlds) {
                    if (world.getEnvironment() == skylandsEnv) {
                        return world;
                    }
                }

                throw new CommandException("No skylands world found.");
                // Handle getting a world from a player
            } else if (filter.matches("^#player$")) {
                String parts[] = filter.split(":", 2);

                // They didn't specify an argument for the player!
                if (parts.length == 1) {
                    throw new CommandException("Argument expected for #player.");
                }

                return matchPlayers(sender, parts[1]).iterator().next().getWorld();
            } else {
                throw new CommandException("Invalid identifier '" + filter + "'.");
            }
        }

        for (World world : worlds) {
            if (world.getName().equals(filter)) {
                return world;
            }
        }

        throw new CommandException("No world by that exact name found.");
    }

    /**
     * Match a target.
     *
     * @param source
     * @param filter
     * @return iterator for players
     * @throws CommandException no matches found
     */
    public static Location matchLocation(CommandSender source, String filter)
            throws CommandException {

        // Handle coordinates
        if (filter.matches("^[\\-0-9\\.]+,[\\-0-9\\.]+,[\\-0-9\\.]+(?:.+)?$")) {
            CommandBook.inst().checkPermission(source, "commandbook.locations.coords");

            String[] args = filter.split(":");
            String[] parts = args[0].split(",");
            double x, y, z;

            try {
                x = Double.parseDouble(parts[0]);
                y = Double.parseDouble(parts[1]);
                z = Double.parseDouble(parts[2]);
            } catch (NumberFormatException e) {
                throw new CommandException("Coordinates expected numbers!");
            }

            if (args.length > 1) {
                return new Location(matchWorld(source, args[1]), x, y, z);
            } else {
                Player player = checkPlayer(source);
                return new Location(player.getWorld(), x, y, z);
            }

            // Handle special hash tag groups
        } else if (filter.charAt(0) == '#') {
            String[] args = filter.split(":");

            // Handle #world, which matches player of the same world as the
            // calling source
            if (args[0].equalsIgnoreCase("#spawn")) {
                CommandBook.inst().checkPermission(source, "commandbook.spawn");
                if (args.length > 1) {
                    return matchWorld(source, args[1]).getSpawnLocation();
                } else {
                    Player sourcePlayer = checkPlayer(source);
                    return sourcePlayer.getLocation().getWorld().getSpawnLocation();
                }

                // Handle #target, which matches the player's target position
            } else if (args[0].equalsIgnoreCase("#target")) {
                CommandBook.inst().checkPermission(source, "commandbook.locations.target");
                Player player = checkPlayer(source);
                Location playerLoc = player.getLocation();
                Block targetBlock = player.getTargetBlock(null, 100);

                if (targetBlock == null) {
                    throw new CommandException("Failed to find a block in your target!");
                } else {
                    Location loc = targetBlock.getLocation();
                    playerLoc.setX(loc.getX());
                    playerLoc.setY(loc.getY());
                    playerLoc.setZ(loc.getZ());
                    return CommandBookUtil.findFreePosition(playerLoc);
                }
                // Handle #home and #warp, which matches a player's home or a warp point
            } else if (args[0].equalsIgnoreCase("#home")
                    || args[0].equalsIgnoreCase("#warp")) {
                String type = args[0].substring(1);
                CommandBook.inst().checkPermission(source, "commandbook.locations." + type);
                LocationsComponent component = type.equalsIgnoreCase("warp")
                        ? CommandBook.inst().getComponentManager().getComponent(WarpsComponent.class)
                        : CommandBook.inst().getComponentManager().getComponent(HomesComponent.class);
                if (component == null)  {
                    throw new CommandException("This type of location is not enabled!");
                }
                RootLocationManager<NamedLocation> manager = component.getManager();
                if (args.length == 1) {
                    if (type.equalsIgnoreCase("warp")) {
                        throw new CommandException("Please specify a warp name.");
                    }
                    // source player home
                    Player ply = checkPlayer(source);
                    NamedLocation loc = manager.get(ply.getWorld(), ply.getName());
                    if (loc == null) {
                        throw new CommandException("You have not set your home yet.");
                    }
                    return loc.getLocation();
                } else if (args.length == 2) {
                    if (source instanceof Player) {
                        Player player = (Player) source;
                        NamedLocation loc = manager.get(player.getWorld(), args[1]);
                        if (loc != null && !(loc.getCreatorName().equalsIgnoreCase(player.getName()))) {
                            CommandBook.inst().checkPermission(source, "commandbook.locations." + type + ".other");
                        }
                    }
                    return getManagedLocation(manager, checkPlayer(source).getWorld(), args[1]);
                } else if (args.length == 3) {
                    if (source instanceof Player) {
                        Player player = (Player) source;
                        NamedLocation loc = manager.get(matchWorld(source, args[2]), args[1]);
                        if (loc != null && !(loc.getCreatorName().equalsIgnoreCase(player.getName()))) {
                            CommandBook.inst().checkPermission(source, "commandbook.locations." + type + ".other");
                        }
                    }
                    return getManagedLocation(manager, matchWorld(source, args[2]), args[1]);
                }
                // Handle #me, which is for when a location argument is required
            } else if (args[0].equalsIgnoreCase("#me")) {
                return checkPlayer(source).getLocation();
            } else {
                throw new CommandException("Invalid group '" + filter + "'.");
            }
        }

        List<Player> players = matchPlayerNames(filter);

        // Check to see if there were any matches
        if (players.size() == 0) {
            throw new CommandException("No players matched query.");
        }

        return players.get(0).getLocation();
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
    
    public static String toFriendlyString(Location location) {
        return location.getBlockX() + "," + 
                location.getBlockY() + "," + 
                location.getBlockZ() + "@" + 
                location.getWorld().getName();
    }
}
