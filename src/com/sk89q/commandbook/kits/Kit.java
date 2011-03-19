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

import java.util.LinkedList;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Specifies the kit.
 * 
 * @author sk89q
 */
public class Kit {
    
    protected LinkedList<ItemStack> items = new LinkedList<ItemStack>();;
    
    /**
     * Add an item to the kit.
     * 
     * @param item
     */
    public void addItem(ItemStack item) {
        items.add(item);
    }
    
    /**
     * Distribute the kit to a player.
     * 
     * @param player
     */
    public void distribute(Player player) {
        for (ItemStack item : items) {
            player.getInventory().addItem(item);
        }
    }
    
}
