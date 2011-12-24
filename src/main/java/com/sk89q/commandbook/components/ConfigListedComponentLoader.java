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
import com.sk89q.commandbook.config.ConfigUtil;
import com.sk89q.commandbook.config.InputStreamYAMLProcessor;
import com.sk89q.util.yaml.YAMLNode;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * @author zml2008
 */
public class ConfigListedComponentLoader implements ComponentLoader {
    private InputStreamYAMLProcessor jarComponentAliases;

    public ConfigListedComponentLoader() {
        jarComponentAliases = new InputStreamYAMLProcessor(null, false) {
            @Override
            public InputStream getInputStream() {
                return CommandBook.inst().getResource("/defaults/modules.yml");
            }
        };
        try {
            jarComponentAliases.load();
        } catch (IOException e) {
            CommandBook.logger().severe("CommandBook: Error loading component aliases!");
            e.printStackTrace();
        }
    }

    @Override
    public Collection<AbstractComponent> loadComponents() {
        List<AbstractComponent> components = new ArrayList<AbstractComponent>();
        List<String> disabledComponents = CommandBook.inst().getGlobalConfiguration().getStringList("components.disabled", null);
        List<String> stagedEnabled = CommandBook.inst().getGlobalConfiguration().getStringList("components.enabled", null);
        for (Iterator<String> i = stagedEnabled.iterator(); i.hasNext(); ) {
            String nextName = i.next();
            nextName = jarComponentAliases.getString(nextName, nextName);
            Class<?> next = null;
            try {
                next = Class.forName(nextName);
            } catch (ClassNotFoundException e) {

            }

            if (next == null || !AbstractComponent.class.isAssignableFrom(next)) {
                CommandBook.logger().warning("CommandBook: Invalid or unknown class found in enabled components: "
                        + nextName + ". Moving to disabled components list.");
                i.remove();
                disabledComponents.add(nextName);
                continue;
            }

            try {
                Constructor<? extends AbstractComponent> construct = next.asSubclass(AbstractComponent.class).getConstructor();
                components.add(construct.newInstance());
            } catch (InvocationTargetException e) {
                e.printStackTrace();
                continue;
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }

        }

        CommandBook.inst().getGlobalConfiguration().setProperty("components.disabled", disabledComponents);
        CommandBook.inst().getGlobalConfiguration().setProperty("components.enabled", stagedEnabled);
        return components;
    }

    @Override
    public YAMLNode getConfiguration(AbstractComponent component) {
        return ConfigUtil.getNode(CommandBook.inst().getGlobalConfiguration(), "component." + component.getClass().getSimpleName());
    }
}
