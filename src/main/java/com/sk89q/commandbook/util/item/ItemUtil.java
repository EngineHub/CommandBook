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

import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.worldedit.world.item.ItemType;
import com.sk89q.worldedit.world.item.ItemTypes;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

/**
 * Various item helper methods that do not depend on user-configurable information.
 */
public class ItemUtil {

    private static final Random RANDOM = new Random();

    public static String toItemName(Material type) throws CommandException {
        ItemType itemType = ItemTypes.get("minecraft:" + type.name().toLowerCase());
        if (itemType == null)
            throw new CommandException("Unknown item type '" + type.name() + "'");
        return itemType.getName();
    }

    /**
     * Returns a matched item.
     *
     * @param name The name to match
     * @return item
     * @see  #getCommandItem(String)
     */
    public static ItemStack getItem(String name) {
        try {
            return getCommandItem(name);
        } catch (CommandException e) {
            return null;
        }
    }


    public static ItemStack getCommandItem(String name) throws CommandException {
        int dmg = 0;
        String enchantmentName = null;

        if (name.contains("|")) {
            String[] parts = name.split("\\|");
            name = parts[0];
            enchantmentName = parts[1];
        }

        name = name.toUpperCase();
        Material material = Material.getMaterial(name.startsWith("MINECRAFT:") ? name.substring(10) : name);
        if (material == null) {
            throw new CommandException("No item type known by '" + name + "'");
        }

        ItemStack stack = new ItemStack(material, 1);
        if (dmg != 0) {
            stack.setDurability((short) dmg);
        }

        if (enchantmentName != null) {
            String[] enchantments = enchantmentName.split(",");
            for (String enchStr : enchantments) {
                int level = 1;
                if (enchStr.contains(":")) {
                    String[] parts = enchStr.split(":");
                    enchStr = parts[0];
                    try {
                        level = Integer.parseInt(parts[1]);
                    } catch (NumberFormatException ignore) {}
                }

                Enchantment ench = null;
                final String testName = enchStr.toLowerCase().replaceAll("[_\\-]", "");
                for (Enchantment possible : Enchantment.values()) {
                    if (possible.getName().toLowerCase().replaceAll("[_\\-]", "").equals(testName)) {
                        ench = possible;
                        break;
                    }
                }

                if (ench == null) {
                    throw new CommandException("Unknown enchantment '" + enchStr + "'");
                }

                stack.addUnsafeEnchantment(ench, level);
            }

        }

        return stack;
    }

    /**
     * Expand a stack of items.
     *
     * @param item
     * @param infinite
     */
    public static void expandStack(ItemStack item, boolean infinite, boolean overrideStackSize) {
        if (item == null || item.getAmount() == 0) {
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
            return DyeColor.values()[RANDOM.nextInt(DyeColor.values().length)];
        }
        try {
            DyeColor match = DyeColor.valueOf(filter.toUpperCase());
            if (match != null) {
                return match;
            }
        } catch (IllegalArgumentException ignored) {}
        throw new CommandException("Unknown dye color name of '" + filter + "'.");
    }
}
