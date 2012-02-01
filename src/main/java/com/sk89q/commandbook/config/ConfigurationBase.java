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

package com.sk89q.commandbook.config;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.util.yaml.YAMLNode;
import com.sk89q.util.yaml.YAMLProcessor;

import java.lang.reflect.Field;
import java.util.logging.Level;

import static com.sk89q.commandbook.config.ConfigUtil.getNode;
import static com.sk89q.commandbook.config.ConfigUtil.prepareSerialization;
import static com.sk89q.commandbook.config.ConfigUtil.smartCast;

/**
 * The base class for configuration of {@link com.sk89q.commandbook.components.AbstractComponent}s
 */
public abstract class ConfigurationBase {
    private boolean isConfigured;

    public boolean isConfigured() {
        return isConfigured;
    }
    
    public void load(YAMLNode node) {
        if (getClass().isAnnotationPresent(SettingBase.class)) {
            node = getNode(node, getClass().getAnnotation(SettingBase.class).value());
        }
        for (Field field : getClass().getFields()) {
            if (!field.isAnnotationPresent(Setting.class)) continue;
            String key = field.getAnnotation(Setting.class).value();
            final Object value = smartCast(field.getGenericType(), node.getProperty(key));
            try {
                field.setAccessible(true);
                if (value != null) {
                    field.set(this, value);
                } else {
                    node.setProperty(key, prepareSerialization(field.get(this)));
                }
            } catch (IllegalAccessException e) {
                CommandBook.logger().log(Level.SEVERE, "Error setting configuration value of field: ", e);
                e.printStackTrace();
            }
        }
        isConfigured = true;
    }
    
    public void save(YAMLNode node) {
        if (getClass().isAnnotationPresent(SettingBase.class)) {
            node = getNode(node, getClass().getAnnotation(SettingBase.class).value());
        }
        for (Field field : getClass().getFields()) {
            field.setAccessible(true);
            if (!field.isAnnotationPresent(Setting.class)) continue;
            String key = field.getAnnotation(Setting.class).value();
            try {
                node.setProperty(key, prepareSerialization(field.get(this)));
            } catch (IllegalAccessException e) {
                CommandBook.logger().log(Level.SEVERE, "Error getting configuration value of field: ", e);
                e.printStackTrace();
            }
        }
    }
}
