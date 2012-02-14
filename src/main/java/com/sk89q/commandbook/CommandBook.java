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

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sk89q.commandbook.config.LegacyCommandBookConfigurationMigrator;
import com.zachsthings.libcomponents.*;
import com.zachsthings.libcomponents.spout.BasePlugin;
import com.zachsthings.libcomponents.spout.YAMLNodeConfigurationNode;
import com.zachsthings.libcomponents.spout.YAMLProcessorConfigurationFile;
import com.zachsthings.libcomponents.config.ConfigurationFile;
import com.zachsthings.libcomponents.loader.ClassLoaderComponentLoader;
import com.zachsthings.libcomponents.loader.ConfigListedComponentLoader;
import com.zachsthings.libcomponents.loader.JarFilesComponentLoader;
import com.zachsthings.libcomponents.loader.StaticComponentLoader;
import com.zachsthings.libcomponents.spout.DefaultsFileYAMLProcessor;
import com.sk89q.commandbook.session.SessionComponent;
import com.sk89q.util.yaml.YAMLFormat;
import com.sk89q.util.yaml.YAMLProcessor;
import com.sk89q.commandbook.commands.*;
import com.sk89q.worldedit.blocks.ItemType;
import org.spout.api.command.CommandRegistrationsFactory;
import org.spout.api.command.CommandSource;
import org.spout.api.command.annotated.*;
import org.spout.api.exception.CommandException;
import org.spout.api.inventory.ItemStack;
import org.spout.api.material.Material;
import org.spout.api.material.MaterialData;
import org.spout.api.player.Player;
import org.yaml.snakeyaml.error.YAMLException;

import static com.sk89q.commandbook.util.ItemUtil.matchItemData;

/**
 * Base plugin class for CommandBook.
 * 
 * @author sk89q
 */
public final class CommandBook extends BasePlugin {
    
    private static CommandBook instance;
    
    protected Map<String, Integer> itemNames;
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

    /**
     * Called when the plugin is enabled. This is where configuration is loaded,
     * and the plugin is setup.
     */
    public void onEnable() {
        super.onEnable();

        final CommandRegistrationsFactory<Class<?>> commandRegistration = 
                new AnnotatedCommandRegistrationFactory(null, new SimpleAnnotatedCommandExecutorFactory());
        if (lowPriorityCommandRegistration) {
            getGame().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
                @Override
                public void run() {
                    getGame().getRootCommand().addSubCommands(CommandBook.this, CommandBookCommands.CommandBookParentCommand.class, commandRegistration);
                }
            }, 0L);
        } else {
            getGame().getRootCommand().addSubCommands(this, CommandBookCommands.CommandBookParentCommand.class, commandRegistration);
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
     * Loads the configuration.
     */
    @SuppressWarnings({"unchecked"})
    public YAMLProcessor populateConfiguration() {
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
        
        return config;
    }

    /**
     * Loads the item list.
     *
     * @param config The {@link YAMLProcessor} to load from
     */
    @SuppressWarnings({ "unchecked" })
    protected void loadItemList(YAMLProcessor config) {

        // Load item names aliases list
        Object itemNamesTemp = config.getProperty("item-names");
        if (itemNamesTemp != null && itemNamesTemp instanceof Map) {
            itemNames = new HashMap<String, Integer>();

            try {
                Map<Object, Object> temp = (Map<Object, Object>) itemNamesTemp;

                for (Map.Entry<Object, Object> entry : temp.entrySet()) {
                    String name = entry.getKey().toString().toLowerCase();

                    // Check if the item ID is a number
                    if (entry.getValue() instanceof Integer) {
                        itemNames.put(name, (Integer) entry.getValue());
                    }
                }
            } catch (ClassCastException e) {
            }
        } else {
            itemNames = new HashMap<String, Integer>();
        }
    }

    /**
     * Returns a matched item.
     *
     * @param name
     * @return item
     */
    public ItemStack getItem(String name) {
        try {
            return getCommandItem(name);
        } catch (CommandException e) {
            return null;
        }
    }
    
    
    public ItemStack getCommandItem(String name) throws CommandException {
        int id = -1;
        Material mat;
        int dmg = 0;
        String dataName = null;
        String enchantmentName = null;

        if (name.contains("|")) {
            String[] parts = name.split("\\|");
            name = parts[0];
            enchantmentName = parts[1];
        }

        if (name.contains(":")) {
            String[] parts = name.split(":", 2);
            dataName = parts[1];
            name = parts[0];
        }


        try {
            id = Integer.parseInt(name);
        } catch (NumberFormatException e) {
            // First check the configurable list of aliases
            Integer idTemp = CommandBook.inst().itemNames.get(name.toLowerCase());

            if (idTemp != null) {
                id = idTemp;
            } else {
                // Then check WorldEdit
                ItemType type = ItemType.lookup(name);

                if (type != null) {
                    id = type.getID();
                }

            }
        }

        if (id == -1) {
            mat = MaterialData.getMaterial(name);
        } else {
            mat = MaterialData.getMaterial((short)id);
        }
        if (mat == null) {
            throw new CommandException("No item type known by '" + name + "'");
        }


        // If the user specified an item data or damage value, let's try
        // to parse it!
        if (dataName != null) {
            dmg = matchItemData(id, dataName);
        }

        ItemStack stack = new ItemStack(mat, 1, (short)dmg);

        /*if (enchantmentName != null) {
            String[] enchantments = enchantmentName.split(",");
            for (String enchStr : enchantments) {
                int level = 1;
                if (enchStr.contains(":")) {
                    String[] parts = enchStr.split(":");
                    enchStr = parts[0];
                    try {
                        level = Integer.parseInt(parts[1]);
                    } catch (NumberFormatException ignore) {}
                }

                Enchantment ench = null;
                final String testName = enchStr.toLowerCase().replaceAll("[_\\-]", "");
                for (Enchantment possible : Enchantment.values()) {
                    if (possible.getName().toLowerCase().replaceAll("[_\\-]", "").equals(testName)) {
                        ench = possible;
                        break;
                    }
                }

                if (ench == null) {
                    throw new CommandException("Unknown enchantment '" + enchStr + "'");
                }

                if (!ench.canEnchantItem(stack)) {
                    throw new CommandException("Invalid enchantment '" +  ench.getName() + "' for item '" + name + "'");
                }

                if (ench.getMaxLevel() < level) {
                    throw new CommandException("Level '" + level +
                            "' is above the maximum level for enchantment '" + ench.getName() + "'");
                }

                stack.addEnchantment(ench, level);
            }

        }*/

        return stack;
    }
    
    /**
     * Gets the IP address of a command source.
     * 
     * @param source
     * @return
     */
    public String toInetAddressString(CommandSource source) {
        if (source instanceof Player) {
            return ((Player) source).getAddress().getHostAddress();
        } else {
            return "127.0.0.1";
        }
    }
}
