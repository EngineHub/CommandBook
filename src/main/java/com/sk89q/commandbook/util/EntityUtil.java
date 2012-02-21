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

import com.sk89q.commandbook.CommandBookUtil;
import com.sk89q.minecraft.util.commands.CommandException;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.CreatureType;

public class EntityUtil {
    /**
     * Attempts to match a creature type.
     *
     * @param sender The sender requesting a creature type match. Can be null.
     * @param filter The filter string for the match
     * @return The matched creature type. Never null.
     * @throws com.sk89q.minecraft.util.commands.CommandException if no CreatureType could be found
     */
    public static CreatureType matchCreatureType(CommandSender sender,
                                          String filter) throws CommandException {
        
        for (CreatureType type : CreatureType.values()) {
            if (type.name().replace("_", "").equalsIgnoreCase(filter.replace("_", ""))
                    || type.getName().equalsIgnoreCase(filter)) {
                return type;
            }
        }

        for (CreatureType testType : CreatureType.values()) {
            if (testType.getName().toLowerCase().startsWith(filter.toLowerCase())) {
                return testType;
            }
        }

        throw new CommandException("Unknown mob specified! You can "
                + "choose from the list of: "
                + CommandBookUtil.getCreatureTypeNameList());
    }
}
