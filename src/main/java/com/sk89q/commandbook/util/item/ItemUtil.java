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
import com.sk89q.worldedit.blocks.*;
import org.bukkit.DyeColor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

/**
 * Various item helper methods that do not depend on user-configurable information.
 */
public class ItemUtil {

    /**
     * Gets the name of an item.
     *
     * @param id
     * @return
     */
    public static String toItemName(int id) {
        ItemType type = ItemType.fromID(id);

        if (type != null) {
            return type.getName();
        } else {
            return "#" + id;
        }
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
        int id;
        int dmg = 0;
        String dataName = null;
        String enchantmentName = null;

        if (name.contains("|")) {
            String[] parts = name.split("\\|");
            name = parts[0];
            enchantmentName = parts[1];
        }

        if (name.contains(":")) {
            String[] parts = name.split(":", 2);
            dataName = parts[1];
            name = parts[0];
        }



        try {
            id = Integer.parseInt(name);
        } catch (NumberFormatException e) {
            // First check the configurable list of aliases
            Integer idTemp = CommandBook.inst().getItemNames().get(name.toLowerCase());

            if (idTemp != null) {
                id = idTemp;
            } else {
                // Then check WorldEdit
                ItemType type = ItemType.lookup(name);

                if (type == null) {
                    throw new CommandException("No item type known by '" + name + "'");
                }

                id = type.getID();
            }
        }

        // If the user specified an item data or damage value, let's try
        // to parse it!
        if (dataName != null) {
            dmg = matchItemData(id, dataName);
        }

        ItemStack stack = new ItemStack(id, 1, (short)dmg);

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

                if (ench.getMaxLevel() < level) {
                    throw new CommandException("Level '" + level +
                            "' is above the maximum level for enchantment '" + ench.getName() + "'");
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
        if (item == null || item.getAmount() == 0 || item.getTypeId() <= 0) {
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
     * Attempt to match item data values.
     *
     * @param id
     * @param filter
     * @return
     * @throws com.sk89q.minecraft.util.commands.CommandException
     */
    public static int matchItemData(int id, String filter) throws CommandException {
        try {
            // First let's try the filter as if it was a number
            return Integer.parseInt(filter);
        } catch (NumberFormatException ignored) {
        }

        // So the value isn't a number, but it may be an alias!
        switch (id) {
            case BlockID.WOOD:
                if (filter.equalsIgnoreCase("redwood")) {
                    return 1;
                } else if (filter.equalsIgnoreCase("birch")) {
                    return 2;
                }

                throw new CommandException("Unknown wood type name of '" + filter + "'.");
            case BlockID.STEP:
            case BlockID.DOUBLE_STEP:
                BlockType dataType = BlockType.lookup(filter);

                if (dataType != null) {
                    if (dataType == BlockType.STONE) {
                        return 0;
                    } else if (dataType == BlockType.SANDSTONE) {
                        return 1;
                    } else if (dataType == BlockType.WOOD) {
                        return 2;
                    } else if (dataType == BlockType.COBBLESTONE) {
                        return 3;
                    } else {
                        throw new CommandException("Invalid slab material of '" + filter + "'.");
                    }
                } else {
                    throw new CommandException("Unknown slab material of '" + filter + "'.");
                }
            case BlockID.CLOTH:
            case BlockID.STAINED_CLAY:
            case BlockID.STAINED_GLASS:
            case BlockID.STAINED_GLASS_PANE:
                ClothColor col = ClothColor.lookup(filter);
                if (col != null) {
                    return col.getID();
                }

                throw new CommandException("Unknown wool color name of '" + filter + "'.");
            case ItemID.INK_SACK: // Dye
                ClothColor dyeCol = ClothColor.lookup(filter);
                if (dyeCol != null) {
                    return 15 - dyeCol.getID();
                }

                throw new CommandException("Unknown dye color name of '" + filter + "'.");
            default:
                throw new CommandException("Invalid data value of '" + filter + "'.");
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
            return DyeColor.getByData((byte) new Random().nextInt(15));
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
