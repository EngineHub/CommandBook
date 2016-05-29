// $Id$
/*
 * CommandBook
 * Copyright (C) 2010, 2011 sk89q <http://www.sk89q.com>
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

package com.sk89q.commandbook;

import com.sk89q.bukkit.util.CommandsManagerRegistration;
import com.sk89q.commandbook.api.ItemProvider;
import com.sk89q.commandbook.commands.CommandBookCommands;
import com.sk89q.commandbook.config.LegacyCommandBookConfigurationMigrator;
import com.sk89q.commandbook.locations.NamedLocation;
import com.sk89q.commandbook.locations.RootLocationManager;
import com.sk89q.commandbook.locations.WarpsComponent;
import com.sk89q.commandbook.session.SessionComponent;
import com.sk89q.commandbook.util.item.ItemUtil;
import com.sk89q.minecraft.util.commands.*;
import com.sk89q.util.yaml.YAMLFormat;
import com.sk89q.util.yaml.YAMLProcessor;
import com.zachsthings.libcomponents.ComponentManager;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.InjectComponentAnnotationHandler;
import com.zachsthings.libcomponents.bukkit.*;
import com.zachsthings.libcomponents.config.ConfigurationFile;
import com.zachsthings.libcomponents.loader.ClassLoaderComponentLoader;
import com.zachsthings.libcomponents.loader.ConfigListedComponentLoader;
import com.zachsthings.libcomponents.loader.JarFilesComponentLoader;
import com.zachsthings.libcomponents.loader.StaticComponentLoader;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base plugin class for CommandBook.
 *
 * @author sk89q
 */
public final class CommandBook extends BasePlugin implements com.sk89q.commandbook.api.CommandBook {

    private static CommandBook instance;

    private CommandsManager<CommandSender> commands;

    private Map<String, Integer> itemNames;
    public boolean broadcastChanges;
    public boolean useDisplayNames;
    public boolean lookupWithDisplayNames;
    public boolean crappyWrapperCompat;


    public CommandBook() {
        super();
        instance = this;
    }

    public static CommandBook inst() {
        return instance;
    }

    public static Logger logger() {
        return inst().getLogger();
    }

    public static void registerEvents(Listener listener) {
        server().getPluginManager().registerEvents(listener, inst());
    }

    /**
     * Called when the plugin is enabled. This is where configuration is loaded,
     * and the plugin is setup.
     */
    public void onEnable() {
        super.onEnable();

        // Register the commands that we want to use
        final CommandBook plugin = this;
        commands = new CommandsManager<CommandSender>() {
            @Override
            public boolean hasPermission(CommandSender player, String perm) {
                return plugin.hasPermission(player, perm);
            }
        };

		final CommandsManagerRegistration cmdRegister = new CommandsManagerRegistration(this, commands);
        if (lowPriorityCommandRegistration) {
            getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
                @Override
                public void run() {
                    cmdRegister.register(CommandBookCommands.CommandBookParentCommand.class);
                }
            }, 1L);
        } else {
            cmdRegister.register(CommandBookCommands.CommandBookParentCommand.class);
        }
    }

    public void registerComponentLoaders() {
        // -- Component loaders
        final File configDir = new File(getDataFolder(), "config/");
        componentManager.addComponentLoader(new StaticComponentLoader(getLogger(), configDir, new SessionComponent()) {
            @Override
            public ConfigurationFile createConfigurationNode(File file) {
                return new YAMLProcessorConfigurationFile(new YAMLProcessor(file, true, YAMLFormat.EXTENDED));
            }
        });
        final YAMLProcessor jarComponentAliases = new DefaultsFileYAMLProcessor("components.yml", false);
        try {
            jarComponentAliases.load();
        } catch (IOException e) {
            getLogger().severe("Error loading component aliases!");
            e.printStackTrace();
        } catch (YAMLException e) {
            getLogger().severe("Error loading component aliases!");
            e.printStackTrace();
        }
        componentManager.addComponentLoader(new ConfigListedComponentLoader(getLogger(),
                new YAMLNodeConfigurationNode(config),
                new YAMLNodeConfigurationNode(jarComponentAliases), configDir));

        for (String dir : config.getStringList("component-class-dirs", Arrays.asList("component-classes"))) {
            final File classesDir = new File(getDataFolder(), dir);
            if (!classesDir.exists() || !classesDir.isDirectory()) {
                classesDir.mkdirs();
            }
            componentManager.addComponentLoader(new ClassLoaderComponentLoader(getLogger(), classesDir, configDir) {
                @Override
                public ConfigurationFile createConfigurationNode(File file) {
                    return new YAMLProcessorConfigurationFile(new YAMLProcessor(file, true, YAMLFormat.EXTENDED));
                }
            });
        }

        for (String dir : config.getStringList("component-jar-dirs", Arrays.asList("component-jars"))) {
            final File classesDir = new File(getDataFolder(), dir);
            if (!classesDir.exists() || !classesDir.isDirectory()) {
                classesDir.mkdirs();
            }
            componentManager.addComponentLoader(new JarFilesComponentLoader(getLogger(), classesDir, configDir) {
                @Override
                public ConfigurationFile createConfigurationNode(File file) {
                    return new YAMLProcessorConfigurationFile(new YAMLProcessor(file, true, YAMLFormat.EXTENDED));
                }
            });
        }

        // -- Annotation handlers
        componentManager.registerAnnotationHandler(InjectComponent.class, new InjectComponentAnnotationHandler(componentManager));
    }

    /**
     * Called on a command.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command cmd,
            String commandLabel, String[] args) {
        try {
            commands.execute(cmd.getName(), args, sender, sender);
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

        return true;
    }

    /**
     * Loads the configuration.
     */
    @Override
    public YAMLProcessor createConfiguration() {
        final File configFile = new File(getDataFolder(), "config.yml");
        YAMLProcessor config = new YAMLProcessor(configFile, true, YAMLFormat.EXTENDED);
        YAMLProcessor comments = new DefaultsFileYAMLProcessor("config-comments.yml", false);
        try {
            if (!configFile.exists()) {
                configFile.getParentFile().mkdirs();
                configFile.createNewFile();
            }
            config.load();
            comments.load();
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Error loading configuration: ", e);
        }

        for (Map.Entry<String, Object> e : comments.getMap().entrySet()) {
            if (e.getValue() != null) {
                config.setComment(e.getKey(), e.getValue().toString());
            }
        }

        // Migrate the old configuration, if we need to
        final String result = new LegacyCommandBookConfigurationMigrator(configFile, config).migrate();
        if (result != null) {
            logger().severe("Error migrating CommandBook configuration: " + result);
        }

        return config;
    }


    @Override
    public void populateConfiguration(YAMLProcessor config) {
        loadItemList(config);

        useDisplayNames = config.getBoolean("use-display-names", true);
        lookupWithDisplayNames = config.getBoolean("lookup-with-display-names", true);
        broadcastChanges = config.getBoolean("broadcast-changes", true);

        crappyWrapperCompat = config.getBoolean("crappy-wrapper-compat", true);

        if (crappyWrapperCompat) {
            getLogger().info("Maximum wrapper compatibility is enabled. " +
                    "Some features have been disabled to be compatible with " +
                    "poorly written server wrappers.");
        }
    }

    /**
     * Loads the item list.
     *
     * @param config The {@link YAMLProcessor} to load from
     */
    protected void loadItemList(YAMLProcessor config) {

        // Load item names aliases list
        Object itemNamesTemp = config.getProperty("item-names");
        if (itemNamesTemp != null && itemNamesTemp instanceof Map) {
            itemNames = new HashMap<String, Integer>();

            try {
                Map<?, ?> temp = (Map<?, ?>) itemNamesTemp;

                for (Map.Entry<?, ?> entry : temp.entrySet()) {
                    String name = entry.getKey().toString().toLowerCase();

                    // Check if the item ID is a number
                    if (entry.getValue() instanceof Integer) {
                        itemNames.put(name, (Integer) entry.getValue());
                    }
                }
            } catch (ClassCastException ignore) {
            }
        } else {
            itemNames = new HashMap<String, Integer>();
        }
    }

    public Map<String, Integer> getItemNames() {

        return Collections.unmodifiableMap(itemNames);
    }


    /**
     * This is an API method another Plugin can use to add an ItemProvider to CommandBook.
     *
     * This will allow admins to add custom items from other plugins in a CommandBook kit.
     *
     * TODO: Is it necessary to track the owning Plugin instances, and perhaps handle plugin enable/disable?
     *
     * @param plugin The Plugin instance that owns the provider.
     * @param provider The ItemProvider that handles creating items for this Plugin
     */
    @Override
    public void addItemProvider(Plugin plugin, ItemProvider provider) {
        ItemUtil.addItemProvider(provider);
    }

    /**
     * Retrieve a warp location by name
     *
     * @param warpName The name of the warp to lookup
     * @return The Location of the warp, or null if unknown
     */
    @Override
    public Location getWarp(String warpName) {
        ComponentManager<BukkitComponent> componentManager = getComponentManager();
        if (componentManager == null) return null;
        WarpsComponent warpsComponent = componentManager.getComponent(WarpsComponent.class);
        if (warpsComponent == null) return null;
        RootLocationManager<NamedLocation> locationManager = warpsComponent.getManager();
        if (locationManager == null) return null;
        NamedLocation location = locationManager.get(null, warpName);
        if (location == null) return null;
        return location.getLocation();
    }
}
