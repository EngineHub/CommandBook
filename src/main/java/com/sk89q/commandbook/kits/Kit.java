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

import com.sk89q.commandbook.util.item.ItemUtil;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

import static com.sk89q.commandbook.CommandBook.logger;

/**
 * Specifies the kit.
 *
 * @author sk89q
 */
public class Kit {

    private class KitItem {
        public final String id;
        public final int amount;

        public KitItem(String id, int amount) {
            this.id = id;
            this.amount = amount;
        }

        public ItemStack getItem() {
            ItemStack template = ItemUtil.getItem(id);
            if (template != null) {
                template.setAmount(amount);
            } else {
                logger().warning("Invalid kit item: '" + id + "'");
            }
            return template;
        }
    }

    private volatile long coolDown;
    private final LinkedList<KitItem> items = new LinkedList<KitItem>();
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
     * @param itemId The item id (key) of the item to add
     * @param amount The amount of item to give
     */
    public void addItem(String itemId, int amount) {
        items.add(new KitItem(itemId, amount));
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

        for (KitItem item : items) {
            player.getInventory().addItem(item.getItem());
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
