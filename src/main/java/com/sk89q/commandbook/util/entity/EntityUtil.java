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

package com.sk89q.commandbook.util.entity;

import com.sk89q.minecraft.util.commands.CommandException;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

public class EntityUtil {

    /**
     * Get the cardinal compass direction of an entity.
     *
     * @param entity
     * @return
     */
    public static String getCardinalDirection(Entity entity) {
        double rot = (entity.getLocation().getYaw() - 90) % 360;
        if (rot < 0) {
            rot += 360.0;
        }
        return getDirection(rot);
    }

    /**
     * Converts a rotation to a cardinal direction name.
     *
     * @param rot
     * @return
     */
    private static String getDirection(double rot) {
        if (0 <= rot && rot < 22.5) {
            return "West";
        } else if (22.5 <= rot && rot < 67.5) {
            return "Northwest";
        } else if (67.5 <= rot && rot < 112.5) {
            return "North";
        } else if (112.5 <= rot && rot < 157.5) {
            return "Northeast";
        } else if (157.5 <= rot && rot < 202.5) {
            return "East";
        } else if (202.5 <= rot && rot < 247.5) {
            return "Southeast";
        } else if (247.5 <= rot && rot < 292.5) {
            return "South";
        } else if (292.5 <= rot && rot < 337.5) {
            return "Southwest";
        } else if (337.5 <= rot && rot < 360.0) {
            return "West";
        } else {
            return null;
        }
    }

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
        
        EntityType partialMatch = null;

        for (EntityType type : EntityType.values()) {
            Class<?> clazz = type.getEntityClass();
            if (clazz == null) continue;
            if (!LivingEntity.class.isAssignableFrom(clazz)) continue;
            if (requireSpawnable && !type.isSpawnable()) continue;
            
            if (type.name().replace("_", "")
                    .equalsIgnoreCase(filter.replace("_", ""))) {
                return type;
            }
            
            if (type.getName() != null) {
                if (type.getName().equalsIgnoreCase(filter)) {
                    return type;
                }
                
                if (type.getName().toLowerCase().startsWith(filter.toLowerCase())) {
                    partialMatch = type;
                }
            }
            
            if (type.name().replace("_", "")
                    .equalsIgnoreCase(filter.replace("_", ""))
                    || (type.getName() != null && type.getName()
                            .equalsIgnoreCase(filter))
                    && (type.isSpawnable() || !requireSpawnable)) {
                return type;
            }
        }
        
        if (partialMatch != null) {
            return partialMatch;
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
