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

package com.sk89q.commandbook.locations;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.commands.PaginatedResult;
import com.sk89q.commandbook.util.LocationUtil;
import com.sk89q.commandbook.util.PlayerUtil;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

/**
 * Parent class for components that use a RootLocationManager<NamedLocation> and deal with locations
 */
public abstract class LocationsComponent extends BukkitComponent {

    private final String name;

    private RootLocationManager<NamedLocation> manager;

    protected LocationsComponent(String name) {
        this.name = name;
    }

    @Override
    public void enable() {
        LocalConfiguration config = configure(new LocalConfiguration());
        LocationManagerFactory<LocationManager<NamedLocation>> warpsFactory =
                new FlatFileLocationsManager.LocationsFactory(CommandBook.inst().getDataFolder(), name + "s");
        manager = new RootLocationManager<NamedLocation>(warpsFactory, config.perWorld);
        CommandBook.registerEvents(new WorldListener());
    }

    private static class LocalConfiguration extends ConfigurationBase {
        @Setting("per-world") public boolean perWorld;
    }


    public RootLocationManager<NamedLocation> getManager() {
        return manager;
    }

    private class WorldListener implements Listener {
        @EventHandler
        public void loadWorld(WorldLoadEvent event) {
            manager.updateWorlds(event.getWorld());
        }

        @EventHandler
        public void unloadWorld(WorldUnloadEvent event) {
            manager.updateWorlds(event.getWorld());
        }
    }

    // -- Command helper methods

    public void remove(String name, World world, CommandSender sender) throws CommandException {
        NamedLocation loc = getManager().get(world, name);
        if (loc == null) {
            throw new CommandException("No " + name.toLowerCase() + " found for " + name + " in world " + world.getName());
        }
        if (!loc.getCreatorName().equals(sender.getName())) {
            CommandBook.inst().checkPermission(sender, "commandbook." + this.name.toLowerCase() + ".remove.other");
        }

        getManager().remove(world, name);
        sender.sendMessage(ChatColor.YELLOW + name + " for " + name + " removed.");
    }

    public void list(CommandContext args, CommandSender sender) throws CommandException {
        World world = null;
        if (getManager().isPerWorld()) {
            if (args.hasFlag('w')) {
                world = LocationUtil.matchWorld(sender, args.getFlag('w'));
            } else {
                world = PlayerUtil.checkPlayer(sender).getWorld();
            }
            if (world == null) throw new CommandException("Error finding world to use!");
        }
        getListResult().display(sender, getManager().getLocations(world), args.getInteger(0, 1));
    }

    public abstract PaginatedResult<NamedLocation> getListResult();
}
