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

import java.io.IOException;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public interface LocationManager<T> {

    /**
     * Set this manager to be for one world only.
     *
     * @param world The world to use
     */
    public void castWorld(World world);

    /**
     * Load from file.
     *
     * @throws IOException when an error occurs during IO
     */
    public void load() throws IOException;

    /**
     * Save  to file.
     *
     * @throws IOException when an error occurs during IO
     */
    public void save() throws IOException;

    /**
     * Get by name.
     *
     * @param id The name to get
     * @return The {@link T} if registered
     */
    public T get(String id);

    /**
     * Updates warps from unloaded worlds.
     */
    public void updateWorlds();

    /**
     * Create a location.
     *
     * @param id The name of the location
     * @param loc The location
     * @param player The player to own the location
     * @return The created location
     */
    public T create(String id, Location loc, Player player);

    /**
     * Removes a location.
     *
     * @param id The id to remove
     * @return whether it was removed
     */
    public boolean remove(String id);

    /**
     * Gets all the locations that this location manager has.
     *
     * @return This location manager's locations.
     */
    public List<T> getLocations();
}
