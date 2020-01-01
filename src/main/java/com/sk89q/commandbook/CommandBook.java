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

import com.google.common.base.Joiner;
import com.sk89q.commandbook.component.session.SessionComponent;
import com.sk89q.commandbook.config.LegacyCommandBookConfigurationMigrator;
import com.sk89q.util.yaml.YAMLFormat;
import com.sk89q.util.yaml.YAMLProcessor;
import com.sk89q.worldedit.internal.command.CommandUtil;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.InjectComponentAnnotationHandler;
import com.zachsthings.libcomponents.bukkit.BasePlugin;
import com.zachsthings.libcomponents.bukkit.DefaultsFileYAMLProcessor;
import com.zachsthings.libcomponents.bukkit.YAMLNodeConfigurationNode;
import com.zachsthings.libcomponents.bukkit.YAMLProcessorConfigurationFile;
import com.zachsthings.libcomponents.config.ConfigurationFile;
import com.zachsthings.libcomponents.loader.ClassLoaderComponentLoader;
import com.zachsthings.libcomponents.loader.ConfigListedComponentLoader;
import com.zachsthings.libcomponents.loader.JarFilesComponentLoader;
import com.zachsthings.libcomponents.loader.StaticComponentLoader;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base plugin class for CommandBook.
 *
 * @author sk89q
 */
public final class CommandBook extends BasePlugin {

    private static CommandBook instance;

    private PlatformCommandManager commandManager = new PlatformCommandManager(this);

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

    private void publishPistonCommands() {
        commandManager.registerCommandsWith(this);
    }

    @Override
    public void onEnable() {
        super.onEnable();

        publishPistonCommands();
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

    // FIXME: Backport to WorldEdit/common lib
    private String rebuildArguments(String commandLabel, String[] args) {
        int plSep = commandLabel.indexOf(":");
        if (plSep >= 0 && plSep < commandLabel.length() + 1) {
            commandLabel = commandLabel.substring(plSep + 1);
        }

        StringBuilder sb = new StringBuilder("/").append(commandLabel);
        if (args.length > 0) {
            sb.append(" ");
        }

        return Joiner.on(" ").appendTo(sb, args).toString();
    }

    /**
     * Called on a command.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        commandManager.handleCommand(sender, rebuildArguments(commandLabel, args));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        String arguments = rebuildArguments(commandLabel, args);
        return CommandUtil.fixSuggestions(arguments, commandManager.handleCommandSuggestion(sender, arguments));
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


}
