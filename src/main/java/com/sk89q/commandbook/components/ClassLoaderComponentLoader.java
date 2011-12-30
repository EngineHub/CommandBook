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
import com.sk89q.util.yaml.YAMLFormat;
import com.sk89q.util.yaml.YAMLNode;
import com.sk89q.util.yaml.YAMLProcessor;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

/**
 * @author zml2008
 */
public class ClassLoaderComponentLoader implements ComponentLoader {
    private final URLClassLoader loader;
    private final File classDir;
    private final File configDir;

    public ClassLoaderComponentLoader(File classDir, File configDir) {
        this.classDir = classDir;
        try {
            this.loader = new URLClassLoader(new URL[] {classDir.toURI().toURL()}, CommandBook.inst().getClass().getClassLoader());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        this.configDir = configDir;
        if (!configDir.exists() || !configDir.isDirectory()) {
            configDir.mkdirs();
        }
    }

    @Override
    public Collection<AbstractComponent> loadComponents() {
        final List<AbstractComponent> components = new ArrayList<AbstractComponent>();
        for (String string : getClassNames()) {
            Class<?> clazz = null;
            try {
                clazz = loader.loadClass(string);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            if (clazz == null || !AbstractComponent.class.isAssignableFrom(clazz)) {
                continue;
            }

            try {
                Constructor<? extends AbstractComponent> construct = clazz.asSubclass(AbstractComponent.class).getConstructor();
                components.add(construct.newInstance());
            } catch (Throwable t) {
                CommandBook.logger().warning("CommandBook: Error initializing component " + clazz + ": " + t.getMessage());
                t.printStackTrace();
                continue;
            }
        }
        return components;
    }

    @Override
    public YAMLNode getConfiguration(AbstractComponent component) {
        final File configFile = new File(configDir, component.getClass().getName() + ".yml");
        YAMLProcessor config = new YAMLProcessor(configFile, true, YAMLFormat.EXTENDED);
        try {
            if (!configFile.exists()) configFile.createNewFile();
            config.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return config;
    }
    
    public Set<String> getClassNames() {
        return recursiveGetClasses(classDir, "");
    }
    
    public Set<String> recursiveGetClasses(File dir, String parentName) {
        Set<String> classNames = new HashSet<String>();
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                classNames.addAll(recursiveGetClasses(file, parentName + file.getName() + "."));
            } else if (file.getName().endsWith(".class")) {
                classNames.add(parentName + file.getName().substring(0, file.getName().length() - 6).replaceAll("\\$", "."));
            }
        }
        return classNames;
    }
}
