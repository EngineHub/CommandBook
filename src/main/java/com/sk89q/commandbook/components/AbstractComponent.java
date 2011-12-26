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
import com.sk89q.commandbook.commands.CommandBookComponentCommand;
import com.sk89q.commandbook.config.ConfigurationBase;
import com.sk89q.commandbook.config.Setting;
import com.sk89q.commandbook.config.SettingBase;
import com.sk89q.commandbook.util.ReflectionUtil;
import com.sk89q.minecraft.util.commands.*;
import com.sk89q.util.yaml.YAMLNode;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;

import java.lang.reflect.Field;
import java.util.*;
import java.util.logging.Level;

import static com.sk89q.commandbook.config.ConfigUtil.*;

/**
 * @author zml2008
 */
public abstract class AbstractComponent {

    /**
     * The {@link CommandsManager} where all commands are registered for this component.
     */
    private CommandsManager<CommandSender> commands;

    /**
     * The raw configuration for this component. This is usually accessed through
     * ConfigurationBase subclasses and #configure()
     */
    private YAMLNode rawConfiguration;

    private ComponentLoader loader;

    private boolean enabled;

    public void setUp(CommandsManager<CommandSender> commands, YAMLNode rawConfiguration, ComponentLoader loader) {
        this.commands = commands;
        this.rawConfiguration = rawConfiguration;
        this.loader = loader;
    }

    /**
     * This method is called once all of this Components fields have been set up
     * and all other Component classes have been discovered
     */
    public abstract void initialize();

    public void unload() {}

    public void reload() {}

    public <T extends ConfigurationBase> T configure(T config) {
        YAMLNode node = rawConfiguration;
        if (config.getClass().isAnnotationPresent(SettingBase.class)) {
            node = getNode(rawConfiguration, config.getClass().getAnnotation(SettingBase.class).value());
        }
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
        return config;
    }
    
    public <T extends ConfigurationBase>  T saveConfig(T config) {
        YAMLNode node = rawConfiguration;
        if (config.getClass().isAnnotationPresent(SettingBase.class)) {
            node = getNode(rawConfiguration, config.getClass().getAnnotation(SettingBase.class).value());
        }
        for (Field field : config.getClass().getFields()) {
            field.setAccessible(true);
            if (!field.isAnnotationPresent(Setting.class)) continue;
            String key = field.getAnnotation(Setting.class).value();
            try {
                node.setProperty(key, field.get(config));
            } catch (IllegalAccessException e) {
                CommandBook.logger().log(Level.SEVERE, "Error getting configuration value of field: ", e);
                e.printStackTrace();
            }
        }
        return config;
    }

    public void registerCommands(Class<?> clazz)  {
        List<Command> registered = commands.registerAndReturn(clazz);
        SimpleCommandMap commandMap = ReflectionUtil.getField(CommandBook.server().getPluginManager(), "commandMap");
        if (registered == null || commandMap == null) {
            return;
        }
        for (Command command : registered) {
            commandMap.register(CommandBook.inst().getDescription().getName() + getClass().getSimpleName(), new CommandBookComponentCommand(command, this));
        }
    }

    public void unregisterCommands() {
        SimpleCommandMap commandMap = ReflectionUtil.getField(CommandBook.server().getPluginManager(), "commandMap");
        List<String> toRemove = new ArrayList<String>();
        Map<String, org.bukkit.command.Command> knownCommands = ReflectionUtil.getField(commandMap, "knownCommands");
        Set<String> aliases = ReflectionUtil.getField(commandMap, "aliases");
        for (Iterator<org.bukkit.command.Command> i = knownCommands.values().iterator(); i.hasNext();) {
            org.bukkit.command.Command cmd = i.next();
            if (cmd instanceof CommandBookComponentCommand && ((CommandBookComponentCommand) cmd).getComponent().equals(this)) {
                i.remove();
                for (String alias : cmd.getAliases()) {
                    org.bukkit.command.Command aliasCmd = knownCommands.get(alias);
                    if (cmd.equals(aliasCmd)) {
                        aliases.remove(alias);
                        toRemove.add(alias);
                    }
                }
            }
        }
        for (String string : toRemove) {
            knownCommands.remove(string);
        }
    }
    
    public boolean handleCommand(CommandSender sender, String alias, String[] args) {
        try {
            commands.execute(alias, args, sender, sender);
            return true;
        } catch (CommandPermissionsException e) {
            sender.sendMessage(ChatColor.RED + "You don't have permission.");
        } catch (MissingNestedCommandException e) {
            sender.sendMessage(ChatColor.RED + e.getUsage());
        } catch (CommandUsageException e) {
            sender.sendMessage(ChatColor.RED + e.getMessage());
            sender.sendMessage(ChatColor.RED + e.getUsage());
        } catch (WrappedCommandException e) {
            if (e.getCause() instanceof NumberFormatException) {
                sender.sendMessage(ChatColor.RED + "Number expected, string received instead.");
            } else {
                sender.sendMessage(ChatColor.RED + "An error has occurred. See console.");
                e.printStackTrace();
            }
        } catch (CommandException e) {
            sender.sendMessage(ChatColor.RED + e.getMessage());
        }
        return false;
    }

    public boolean isEnabled() {
        return enabled;
    }

    void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public ComponentLoader getComponentLoader() {
        return loader;
    }
    
    public void setRawConfiguration(YAMLNode node) {
        this.rawConfiguration = node;
    }
}
