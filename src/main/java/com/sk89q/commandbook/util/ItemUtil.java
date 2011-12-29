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

package com.sk89q.commandbook.util;

import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.blocks.BlockType;
import com.sk89q.worldedit.blocks.ClothColor;
import com.sk89q.worldedit.blocks.ItemType;
import org.bukkit.DyeColor;

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
        } catch (NumberFormatException e) {
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
                ClothColor col = ClothColor.lookup(filter);
                if (col != null) {
                    return col.getID();
                }

                throw new CommandException("Unknown wool color name of '" + filter + "'.");
            case 351: // Dye
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
        } catch (IllegalArgumentException e) {}
        throw new CommandException("Unknown dye color name of '" + filter + "'.");
    }
}
