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

package com.sk89q.commandbook.kits;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Specifies the kit.
 *
 * @author sk89q
 */
public class Kit {

    private volatile long coolDown;
    private final LinkedList<ItemStack> items = new LinkedList<ItemStack>();
    private final Map<String, Long> lastDistribution = new HashMap<String, Long>();

    /**
     * Get the cooldown time in milliseconds.
     *
     * @return The time of cooldown
     */
    public long getCoolDown() {
        return coolDown;
    }

    /**
     * Set the cooldown time in milliseconds.
     *
     * @param coolDown The time to set the cooldown to
     */
    public void setCoolDown(long coolDown) {
        this.coolDown = coolDown;
    }

    /**
     * Add an item to the kit.
     *
     * @param item The item to add
     */
    public void addItem(ItemStack item) {
        items.add(item);
    }

    /**
     * Distribute the kit to a player.
     *
     * @param player The player to distribute to
     * @return false if it has been too soon
     */
    public synchronized boolean distribute(Player player) {
        if (coolDown > 0) {
            Long time = lastDistribution.get(player.getName());
            long now = System.currentTimeMillis();

            if (time != null) {
                // Not enough time has passed
                if (now - time < coolDown) {
                    return false;
                }
            }

            lastDistribution.put(player.getName(), now);
        }

        for (ItemStack item : items) {
            player.getInventory().addItem(item.clone());
        }

        return true;
    }

    /**
     * Get rid of old distribution records.
     */
    public synchronized void flush() {
        long now = System.currentTimeMillis();
        Iterator<Long> it = lastDistribution.values().iterator();

        try {
            while (it.hasNext()) {
                if (now - it.next() > coolDown) {
                    it.remove();
                }
            }
        } catch (NoSuchElementException ignore) {
        }
    }
}
