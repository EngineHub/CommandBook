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

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.*;
import java.util.logging.Level;

/**
 * @author zml2008
 */
public class ComponentManager {
    protected List<ComponentLoader> loaders = new ArrayList<ComponentLoader>();
    protected List<AbstractComponent> registeredComponents = new ArrayList<AbstractComponent>();
    protected final Map<Class<? extends Annotation>, AnnotationHandler<?>> annotationHandlers = new LinkedHashMap<Class<? extends Annotation>, AnnotationHandler<?>>();

    public synchronized boolean addComponentLoader(ComponentLoader loader) {
        loaders.add(loader);
        return true;
    }

    public synchronized boolean loadComponents() {
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

                component.setUp(commands, componentConfig, loader);
                
                registeredComponents.add(component);
            }
        }
        return true;
    }

    public synchronized void enableComponents() {
        for (AbstractComponent component : registeredComponents) {
            for (Field field : component.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                for (Annotation annotation : field.getAnnotations()) {
                    AnnotationHandler<Annotation> handler =
                            (AnnotationHandler<Annotation>)annotationHandlers.get(annotation.annotationType());
                    if (handler != null) {
                        if (!handler.handle(component, field, annotation)) {
                            CommandBook.logger().log(Level.WARNING, "CommandBook: Component "
                                    + component.getClass().getSimpleName() +
                                    " could not be enabled! Error in annotation handler for field " + field);
                        }
                    }
                }
            }
            component.initialize();
            component.setEnabled(true);
            CommandBook.logger().log(Level.FINEST, "CommandBook: Component " +
                    component.getClass().getSimpleName() + " successfully enabled!");
        }

    }
    
    public synchronized void unloadComponents() {
        for (AbstractComponent component : registeredComponents) {
            component.unload();
            component.unregisterCommands();
        }
    }
    
    public synchronized void reloadComponents() {
        for (AbstractComponent component : registeredComponents) {
            component.setRawConfiguration(component.getComponentLoader().getConfiguration(component));
            component.reload();
        }
    }
    
    public synchronized <T> T getComponent(Class<T> type) {
        for (AbstractComponent component : registeredComponents) {
            if (component.getClass().equals(type)) {
                return type.cast(component);
            }
        }
        return null;
    }
    
    public synchronized <T extends Annotation> void registerAnnotationHandler(Class<T> annotation, AnnotationHandler<T> handler) {
        annotationHandlers.put(annotation, handler);
    }
    
    public synchronized <T extends Annotation> AnnotationHandler<T> getAnnotatioHandler(Class<T> annotation) {
        return (AnnotationHandler<T>)annotationHandlers.get(annotation);
    }
}
