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
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

public class EntityUtil {
    /**
     * Attempts to match a creature type.
     *
     * @param sender The sender requesting a creature type match. Can be null.
     * @param filter The filter string for the match
     * @param requireSpawnable Whether to only allow EntityTypes that are spawnable
     * @return The matched creature type. Never null.
     * @throws com.sk89q.minecraft.util.commands.CommandException if no CreatureType could be found
     */
    public static EntityType matchCreatureType(CommandSender sender,
            String filter, boolean requireSpawnable) throws CommandException {

        for (EntityType type : EntityType.values()) {
            if (type.name().replace("_", "")
                    .equalsIgnoreCase(filter.replace("_", ""))
                    || (type.getName() != null && type.getName()
                            .equalsIgnoreCase(filter))
                    && (type.isSpawnable() || !requireSpawnable)) {
                return type;
            }
        }

        for (EntityType testType : EntityType.values()) {
            if (testType.getName() != null
                    && testType.getName().toLowerCase()
                            .startsWith(filter.toLowerCase())
                    && (testType.isSpawnable() || !requireSpawnable)) {
                return testType;
            }
        }

        throw new CommandException("Unknown mob specified! You can "
                + "choose from the list of: "
                + getCreatureNameList(requireSpawnable));
    }

    /**
     * Get a list of creature names.
     *
     * @param requireSpawnable Whether to only show entries that are spawnable
     * @return
     */
    public static String getCreatureNameList(boolean requireSpawnable) {
        StringBuilder str = new StringBuilder();
        for (EntityType type : EntityType.values()) {
            Class<?> entityClass = type.getEntityClass();
            if (entityClass == null) {
                continue;
            }
            if (!LivingEntity.class.isAssignableFrom(entityClass)) {
                continue;
            }
            if (!requireSpawnable || type.isSpawnable()) {
                if (str.length() > 0) {
                    str.append(", ");
                }
                str.append(type.getName());
            }
        }

        return str.toString();
    }
}
