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

package com.sk89q.commandbook;

import java.util.logging.Logger;
import org.bukkit.World;
import org.bukkit.event.world.WorldListener;
import org.bukkit.event.world.WorldLoadEvent;

public class CommandBookWorldListener extends WorldListener {
    
    protected static final Logger logger = Logger.getLogger("Minecraft.CommandBook");
    
    protected CommandBook plugin;
    
    /**
     * Construct the listener.
     * 
     * @param plugin
     */
    public CommandBookWorldListener(CommandBook plugin) {
        this.plugin = plugin;
    }

    /**
     * Called when a World is loaded.
     */
    @Override
    public void onWorldLoad(WorldLoadEvent event) {
        World world = event.getWorld();
        Integer lockedTime = plugin.getLockedTimes().get(world.getName());
        
        if (lockedTime != null) {
            world.setTime(lockedTime);
            plugin.getTimeLockManager().lock(world);
            logger.info("CommandBook: Time locked to '"
                    + CommandBookUtil.getTimeString(lockedTime) + "' for world '"
                    + world.getName() + "'");
        }
    }

}
