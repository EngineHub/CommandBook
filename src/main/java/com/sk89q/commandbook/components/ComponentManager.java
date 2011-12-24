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
import com.sk89q.minecraft.util.commands.CommandsManager;
import com.sk89q.minecraft.util.commands.SimpleInjector;
import com.sk89q.util.yaml.YAMLNode;
import org.bukkit.command.CommandSender;

import java.lang.reflect.Field;
import java.util.*;
import java.util.logging.Level;

/**
 * @author zml2008
 */
public class ComponentManager {
    protected List<ComponentLoader> loaders = new ArrayList<ComponentLoader>();
    protected Map<String, AbstractComponent> registeredComponents = new HashMap<String, AbstractComponent>();

    public boolean addComponentLoader(ComponentLoader loader) {
        loaders.add(loader);
        return true;
    }

    public boolean loadComponents() {
        Collection<AbstractComponent> components = new ArrayList<AbstractComponent>();
        for (ComponentLoader loader : loaders) {
            for (AbstractComponent component : loader.loadComponents()) {
                // Create a CommandsManager instance
                CommandsManager<CommandSender> commands = new CommandsManager<CommandSender>() {
                    @Override
                    public boolean hasPermission(CommandSender sender, String perm) {
                        return CommandBook.inst().hasPermission(sender, perm);
                    }
                };
                commands.setInjector(new SimpleInjector(component));

                YAMLNode componentConfig = loader.getConfiguration(component);

                component.setUp(commands, componentConfig);
            }
        }
        return true;
    }

    public void enableComponents() {
        for (AbstractComponent component : registeredComponents.values()) {
            for (Field field : component.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                /*if (field.isAnnotationPresent(RequiredService.class)) {
                    setFieldService(field, field.getType(), component);
                }*/
            }
            CommandBook.logger().log(Level.FINEST, "CommandBook: Component " + component.getClass().getSimpleName() + " successfully enabled!");
            component.initialize();
        }

    }

    public <T> void setFieldService(Field field, Class<T> service, AbstractComponent component) {
        T registration = CommandBook.server().getServicesManager().load(service);
        try {
            field.set(component, registration);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
