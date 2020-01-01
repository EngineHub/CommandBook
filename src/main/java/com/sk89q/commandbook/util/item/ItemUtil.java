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

package com.sk89q.commandbook.util.item;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.blocks.*;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

/**
 * Various item helper methods that do not depend on user-configurable information.
 */
public class ItemUtil {
    /**
     * Returns a matched item.
     *
     * @param name The name to match
     * @return item
     * @see  #getCommandItem(String)
     */
    public static BaseItem getItem(String name) {
        try {
            return getCommandItem(name);
        } catch (CommandException e) {
            return null;
        }
    }


    public static BaseItem getCommandItem(String name) throws CommandException {
        try {
            return WorldEdit.getInstance().getItemFactory().parseFromInput(name, new ParserContext());
        } catch (InputParseException e) {
            throw new CommandException(e.getMessage());
        }
    }

    /**
     * Expand a stack of items.
     *
     * @param item
     * @param infinite
     */
    public static void expandStack(ItemStack item, boolean infinite, boolean overrideStackSize) {
        if (item == null || item.getAmount() == 0 || item.getType() == Material.AIR) {
            return;
        }

        int stackSize = overrideStackSize ? 64 : item.getType().getMaxStackSize();

        if (item.getType().getMaxStackSize() == 1) {
            return;
        }

        if (infinite) {
            item.setAmount(-1);
        } else if (item.getAmount() < stackSize){
            item.setAmount(stackSize);
        }
    }

    /**
     * Attempt to match a dye color for sheep wool.
     *
     * @param filter
     * @return
     * @throws CommandException
     */
    public static DyeColor matchDyeColor(String filter) throws CommandException {
        if (filter.equalsIgnoreCase("random")) {
            DyeColor[] values = DyeColor.values();
            return values[new Random().nextInt(values.length)];
        }

        try {
            return DyeColor.valueOf(filter.toUpperCase());
        } catch (IllegalArgumentException ignored) {}

        throw new CommandException("Unknown dye color name of '" + filter + "'.");
    }
}
