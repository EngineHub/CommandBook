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

package com.sk89q.commandbook.locations;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.sk89q.commandbook.CommandBook.logger;

public class RootLocationManager<T> {

    private LocationManager<T> rootManager;
    private Map<String, LocationManager<T>> managers;
    private final LocationManagerFactory<LocationManager<T>> factory;
    private final boolean perWorld;

    public RootLocationManager(LocationManagerFactory<LocationManager<T>> factory, boolean perWorld) {
        this.factory = factory;
        this.perWorld = perWorld;

        if (perWorld) {
            managers = new HashMap<String, LocationManager<T>>();
        } else {
            rootManager = factory.createManager();

            try {
                rootManager.load();
            } catch (IOException e) {
                logger().warning("Failed to load warps: " + e.getMessage());
            }
        }
    }

    private LocationManager<T> getManager(World world) {
        if (!perWorld) {
            return rootManager;
        }

        LocationManager<T> manager = managers.get(world.getName());

        if (manager != null) {
            return manager;
        }

        manager = factory.createManager(world);
        manager.castWorld(world);

        try {
            manager.load();
        } catch (IOException e) {
            logger().warning("Failed to load warps for world " + world.getName()
                    + ": " + e.getMessage());
        }

        managers.put(world.getName(), manager);
        return manager;
    }

    public T get(World world, String id) {
        return getManager(world).get(id);
    }

    public T create(String id, Location loc, Player player) {
        LocationManager<T> manager = getManager(loc.getWorld());
        T ret = manager.create(id, loc, player);
        save(manager);
        return ret;
    }

    public boolean remove(World world, String id) {
        LocationManager<T> manager = getManager(world);
        boolean ret = getManager(world).remove(id);
        save(manager);
        return ret;
    }

    private void save(LocationManager<T> manager) {
        try {
            manager.save();
        } catch (IOException e) {
            logger().warning("Failed to save warps: " + e.getMessage());
        }
    }

    public List<T> getLocations(World world) {
        return getManager(world).getLocations();
    }

    public boolean isPerWorld() {
        return perWorld;
    }

    public void updateWorlds(World world) {
        getManager(world).updateWorlds();
    }
}
