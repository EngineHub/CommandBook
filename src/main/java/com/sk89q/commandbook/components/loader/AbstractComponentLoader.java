/*
 * CommandBook
 * Copyright (C) 2012 sk89q <http://www.sk89q.com>
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

package com.sk89q.commandbook.components.loader;

import com.sk89q.commandbook.components.AbstractComponent;
import com.sk89q.commandbook.components.ComponentInformation;
import com.sk89q.commandbook.components.InvalidComponentException;
import com.sk89q.util.yaml.YAMLFormat;
import com.sk89q.util.yaml.YAMLNode;
import com.sk89q.util.yaml.YAMLProcessor;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;

/**
 * A parent class that contains several useful component loader helper methods
 */
public abstract class AbstractComponentLoader implements ComponentLoader {
    private final File configDir;
    
    protected AbstractComponentLoader(File configDir) {
        this.configDir = configDir;
        if (!configDir.exists() || !configDir.isDirectory()) {
            configDir.mkdirs();
        }
    }

    @Override
    public YAMLNode getConfiguration(AbstractComponent component) {
        final File configFile = new File(configDir, toFileName(component) + ".yml");
        YAMLProcessor config = new YAMLProcessor(configFile, true, YAMLFormat.EXTENDED);
        try {
            if (!configFile.exists()) configFile.createNewFile();
            config.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return config;
    }

    /**
     * Instantiates a component, performing checks for various requirements.
     * @param clazz The class that should be the component;
     * @return An instance of the component class
     * @throws Throwable when an error occurs while initializing the component for some reason.
     */
    protected AbstractComponent instantiateComponent(Class<?> clazz) throws Throwable {
        // Do we have the component information?
        if (!clazz.isAnnotationPresent(ComponentInformation.class)) {
            throw new InvalidComponentException(clazz, "No ComponentInformation annotation!");
        }

        // Instantiation!
        Constructor<? extends AbstractComponent> construct = clazz.asSubclass(AbstractComponent.class).getConstructor();
        return construct.newInstance();
    }
    
    public boolean isComponentClass(Class<?> clazz) {
        return clazz != null && AbstractComponent.class.isAssignableFrom(clazz);
    }
    
    public String toFileName(AbstractComponent component) {
        return component.getInformation().friendlyName().replaceAll(" ", "-").toLowerCase();
    }
}
