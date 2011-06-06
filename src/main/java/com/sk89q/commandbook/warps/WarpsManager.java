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

package com.sk89q.commandbook.warps;

import java.io.IOException;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * Manager for kits.
 * 
 * @author sk89q
 */
public interface WarpsManager {
    
    /**
     * Set this warps manager to be for one world only.
     * 
     * @param world
     */
    public void castWorld(World world);

    /**
     * Load warps from file.
     * 
     * @throws IOException 
     */
    public void load() throws IOException;

    /**
     * Save warps to file.
     * 
     * @throws IOException 
     */
    public void save() throws IOException;

    /**
     * Get a warp by name.
     * 
     * @param id
     * @return
     */
    public Warp getWarp(String id);
    
    /**
     * Create a warp.
     * 
     * @param id
     * @param loc
     * @param player
     * @return
     */
    public Warp createWarp(String id, Location loc, Player player);

    /**
     * Removes a warp.
     * 
     * @param id
     * @return whether it was removed
     */
    public boolean removeWarp(String id);
}