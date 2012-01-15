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

import com.sk89q.bukkit.util.CommandsManagerRegistration;
import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.components.loader.ComponentLoader;
import com.sk89q.commandbook.config.ConfigurationBase;
import com.sk89q.minecraft.util.commands.*;
import com.sk89q.util.yaml.YAMLNode;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.Map;

/**
 * @author zml2008
 */
public abstract class AbstractComponent implements CommandExecutor {

    /**
     * The {@link CommandsManager} where all commands are registered for this component.
     */
    private CommandsManager<CommandSender> commands;

    /**
     * The {@link CommandsManagerRegistration} used to handle dynamic registration of commands contained within this component
     */
    private CommandsManagerRegistration commandRegistration;

    /**
     * The raw configuration for this component. This is usually accessed through
     * ConfigurationBase subclasses and #configure()
     */
    private YAMLNode rawConfiguration;

    private ComponentLoader loader;
    
    private ComponentInformation info;

    private boolean enabled;

    public void setUp(CommandsManager<CommandSender> commands, ComponentLoader loader, ComponentInformation info) {
        this.commands = commands;
        this.loader = loader;
        this.info = info;
        commandRegistration = new CommandsManagerRegistration(CommandBook.inst(), this, commands);
    }

    /**
     * This method is called once all of this Component's fields have been set up
     * and all other Component classes have been discovered
     */
    public abstract void initialize();

    public void unload() {}

    public void reload() {
        if (rawConfiguration != null) {
            rawConfiguration = getComponentLoader().getConfiguration(this);
        }
    }

    public <T extends ConfigurationBase> T configure(T config) {
        config.load(getRawConfiguration());
        return config;
    }
    
    public <T extends ConfigurationBase>  T saveConfig(T config) {
        config.save(getRawConfiguration());
        return config;
    }

    public void registerCommands(final Class<?> clazz)  {
        if (CommandBook.inst().lowPriorityCommandRegistration) {
         CommandBook.server().getScheduler().scheduleSyncDelayedTask(CommandBook.inst(), new Runnable() {
             @Override
             public void run() {
                 commandRegistration.register(clazz);
             }
         }, 0L);
        } else {
            commandRegistration.register(clazz);
        }
    }

    public void unregisterCommands() {
        commandRegistration.unregisterCommands();
    }
    
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
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
    
    public ComponentInformation getInformation() {
        return info;
    }
    
    public YAMLNode getRawConfiguration() {
        if (rawConfiguration != null) {
            return rawConfiguration;
        } else {
            return rawConfiguration = getComponentLoader().getConfiguration(this);
        }
    }
    
    public Map<String, String> getCommands() {
        if (commands == null) {
            return Collections.emptyMap();
        } else {
            return commands.getCommands();
        }
    }
}
