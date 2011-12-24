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

package com.sk89q.commandbook.components;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.config.ConfigurationBase;
import com.sk89q.commandbook.config.Setting;
import com.sk89q.commandbook.config.SettingBase;
import com.sk89q.util.yaml.YAMLNode;

import java.lang.reflect.Field;
import java.util.logging.Level;

import static com.sk89q.commandbook.config.ConfigUtil.*;

/**
 * @author zml2008
 */
public abstract class AbstractComponent {

    public abstract void initialize();

    public void unload() {}

    public <T extends ConfigurationBase> T configure(T config) {
        if (config.getClass().isAnnotationPresent(SettingBase.class)) {
            YAMLNode node = getNode(CommandBook.inst().getGlobalConfiguration(),
                    config.getClass().getAnnotation(SettingBase.class).value());
            for (Field field : config.getClass().getFields()) {
                if (!field.isAnnotationPresent(Setting.class)) continue;
                String key = field.getAnnotation(Setting.class).value();
                System.out.println(field.getGenericType());
                final Object value = smartCast(field.getGenericType(), node.getProperty(key));
                if (value != null && field.getType().isAssignableFrom(value.getClass())) {
                    try {
                        field.setAccessible(true);
                        field.set(config, value);
                    } catch (IllegalAccessException e) {
                        CommandBook.logger().log(Level.SEVERE, "Error setting configuration value of field: ", e);
                        e.printStackTrace();
                    }
                }
            }
        }
        return config;
    }
    
    public <T extends ConfigurationBase>  T saveConfiguration(T config) {
        if (config.getClass().isAnnotationPresent(SettingBase.class)) {
            YAMLNode node = getNode(CommandBook.inst().getGlobalConfiguration(),
                    config.getClass().getAnnotation(SettingBase.class).value());
            for (Field field : config.getClass().getFields()) {
                field.setAccessible(true);
                if (!field.isAnnotationPresent(Setting.class)) continue;
                String key = field.getAnnotation(Setting.class).value();
                try {
                    node.setProperty(key, field.get(config));
                } catch (IllegalAccessException e) {
                    CommandBook.logger().log(Level.SEVERE, "Error setting configuration value of field: ", e);
                    e.printStackTrace();
                }
            }
        }
        return config;
    }
}
