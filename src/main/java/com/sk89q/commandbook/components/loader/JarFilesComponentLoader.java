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

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.components.AbstractComponent;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * A component loader that loads components from all the jar files in a given folder
 */
public class JarFilesComponentLoader extends FileComponentLoader {
    private final File jarDir;

    public JarFilesComponentLoader(File jarDir, File configDir) {
        super(configDir);
        this.jarDir = jarDir;
    }

    @Override
    public Collection<AbstractComponent> loadComponents() {
        final List<AbstractComponent> components = new ArrayList<AbstractComponent>();

        // Iterate through the files in the jar dirs
        for (final File file : jarDir.listFiles()) {
            if (!file.getName().endsWith(".jar")) continue;
            JarFile jarFile;
            ClassLoader loader;
            try {
                jarFile = new JarFile(file);
                loader = AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                    @Override
                    public ClassLoader run() {
                        try {
                            return new URLClassLoader(new URL[] {file.toURI().toURL()}, CommandBook.inst().getClass().getClassLoader());
                        } catch (MalformedURLException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            } catch (IOException e) {
                continue;
            }

            // And then the files in the jar
            for (Enumeration<JarEntry> en = jarFile.entries(); en.hasMoreElements(); ) {
                JarEntry next = en.nextElement();
                // Make sure it's a class
                if (!next.getName().endsWith(".class")) continue;

                Class<?> clazz = null;
                try {
                    clazz = loader.loadClass(formatPath(next.getName()));
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }

                if (!isComponentClass(clazz)) continue;

                try {
                    components.add(instantiateComponent(clazz));
                } catch (Throwable t) {
                    CommandBook.logger().warning("Error initializing component " + clazz + ": " + t.getMessage());
                    t.printStackTrace();
                }
            }
        }
        return components;
    }
}
