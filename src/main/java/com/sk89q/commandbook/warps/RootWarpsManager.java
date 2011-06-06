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
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class RootWarpsManager {
    
    private static final Logger logger = Logger.getLogger("Minecraft.CommandBook");

    private WarpsManager rootManager;
    private Map<String, WarpsManager> managers;
    private WarpsManagerFactory factory;
    private boolean perWorld;
    
    public RootWarpsManager(WarpsManagerFactory factory, boolean perWorld) {
        this.factory = factory;
        this.perWorld = perWorld;
        
        if (perWorld) {
            managers = new HashMap<String, WarpsManager>();
        } else {
            rootManager = factory.createManager();
            
            try {
                rootManager.load();
            } catch (IOException e) {
                logger.warning("Failed to load warps: " + e.getMessage());
            }
        }
    }
    
    private WarpsManager getManager(World world) {
        WarpsManager manager = managers.get(world.getName());
        
        if (manager != null) {
            return manager;
        }
        
        manager = factory.createManager(world);
        
        try {
            manager.load();
        } catch (IOException e) {
            logger.warning("Failed to load warps for world " + world.getName()
                    + ": " + e.getMessage());
        }
        
        managers.put(world.getName(), manager);
        return manager;
    }
    
    public Warp getWarp(World world, String id) {
        if (perWorld) {
            return getManager(world).getWarp(id);
        } else {
            return rootManager.getWarp(id);
        }
    }
    
    public Warp createWarp(String id, Location loc, Player player) {
        if (perWorld) {
            WarpsManager manager = getManager(loc.getWorld());
            Warp ret = manager.createWarp(id, loc, player);
            save(manager);
            return ret;
        } else {
            Warp ret = rootManager.createWarp(id, loc, player);
            save(rootManager);
            return ret;
        }
    }
    
    public boolean removeWarp(World world, String id) {
        if (perWorld) {
            WarpsManager manager = getManager(world);
            boolean ret = getManager(world).removeWarp(id);
            save(manager);
            return ret;
        } else {
            boolean ret = rootManager.removeWarp(id);
            save(rootManager);
            return ret;
        }
    }
    
    private void save(WarpsManager manager) {
        try {
            manager.save();
        } catch (IOException e) {
            logger.warning("Failed to save warps: " + e.getMessage());
        }
    }
    
    public boolean isPerWorld() {
        return perWorld;
    }
}
