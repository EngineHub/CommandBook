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
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;

import com.sk89q.bukkit.util.CommandsManagerRegistration;
import com.sk89q.commandbook.components.*;
import com.sk89q.commandbook.components.loader.ClassLoaderComponentLoader;
import com.sk89q.commandbook.components.loader.ConfigListedComponentLoader;
import com.sk89q.commandbook.components.loader.JarFilesComponentLoader;
import com.sk89q.commandbook.components.loader.StaticComponentLoader;
import com.sk89q.commandbook.config.DefaultsFileYAMLProcessor;
import com.sk89q.commandbook.config.LegacyCommandBookConfigurationMigrator;
import com.sk89q.commandbook.events.ComponentManagerInitEvent;
import com.sk89q.commandbook.session.SessionComponent;
import com.sk89q.util.yaml.YAMLFormat;
import com.sk89q.util.yaml.YAMLProcessor;
import com.sk89q.wepif.PermissionsResolverManager;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import com.sk89q.commandbook.commands.*;
import com.sk89q.minecraft.util.commands.*;
import com.sk89q.worldedit.blocks.ItemType;

import static com.sk89q.commandbook.util.ItemUtil.matchItemData;

/**
 * Base plugin class for CommandBook.
 * 
 * @author sk89q
 */
public final class CommandBook extends JavaPlugin {
    
    private static CommandBook instance;

    private CommandsManager<CommandSender> commands;
    
    protected Map<String, Integer> itemNames;
    public boolean broadcastChanges;
    public boolean opPermissions;
    public boolean useDisplayNames;
    public boolean lookupWithDisplayNames;
    public boolean crappyWrapperCompat;
    public boolean lowPriorityCommandRegistration;

    protected YAMLProcessor config;
    protected ComponentManager componentManager;

    
    public CommandBook() {
        super();
        instance = this;
    }
    
    public static CommandBook inst() {
        return instance;
    }
    
    public static Server server() {
        return Bukkit.getServer();
    }
    
    public static Logger logger() {
        return inst().getLogger();
    }
    
    public static void registerEvents(Listener listener) {
        server().getPluginManager().registerEvents(listener, inst());
    }
    
    public static <T extends Event> T callEvent(T event) {
        server().getPluginManager().callEvent(event);
        return event;
    }

    /**
     * Called when the plugin is enabled. This is where configuration is loaded,
     * and the plugin is setup.
     */
    public void onEnable() {
        
        // Make the data folder for the plugin where configuration files
        // and other data files will be stored
        getDataFolder().mkdirs();

        // Register the commands that we want to use
        final CommandBook plugin = this;
        commands = new CommandsManager<CommandSender>() {
            @Override
            public boolean hasPermission(CommandSender player, String perm) {
                return plugin.hasPermission(player, perm);
            }
        };

        // Prepare permissions
        PermissionsResolverManager.initialize(this);

        // Load configuration
        populateConfiguration();
        
        componentManager = new ComponentManager();

        
        // -- Component loaders
        final File configDir = new File(plugin.getDataFolder(), "config/");
        componentManager.addComponentLoader(new StaticComponentLoader(configDir, new SessionComponent()));
        componentManager.addComponentLoader(new ConfigListedComponentLoader(
                new DefaultsFileYAMLProcessor("components.yml", false), configDir));

        for (String dir : config.getStringList("component-class-dirs", Arrays.asList("component-classes"))) {
            final File classesDir = new File(plugin.getDataFolder(), dir);
            if (!classesDir.exists() || !classesDir.isDirectory()) {
                classesDir.mkdirs();
            }
            componentManager.addComponentLoader(new ClassLoaderComponentLoader(classesDir, configDir));
        }

        for (String dir : config.getStringList("component-jar-dirs", Arrays.asList("component-jars"))) {
            final File classesDir = new File(plugin.getDataFolder(), dir);
            if (!classesDir.exists() || !classesDir.isDirectory()) {
                classesDir.mkdirs();
            }
            componentManager.addComponentLoader(new JarFilesComponentLoader(classesDir, configDir));
        }
        
        // -- Annotation handlers
        componentManager.registerAnnotationHandler(InjectComponent.class, new InjectComponentAnnotationHandler());

        getServer().getPluginManager().callEvent(new ComponentManagerInitEvent(componentManager));

        componentManager.loadComponents();
        
		final CommandsManagerRegistration cmdRegister = new CommandsManagerRegistration(this, commands);
        if (lowPriorityCommandRegistration) {
            getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
                @Override
                public void run() {
                    cmdRegister.register(CommandBookCommands.CommandBookParentCommand.class);
                }
            }, 0L);
        } else {
            cmdRegister.register(CommandBookCommands.CommandBookParentCommand.class);
        }

        componentManager.enableComponents();

        config.save();
    }

    /**
     * Called when the plugin is disabled. Shutdown and clearing of any
     * temporary data occurs here.
     */
    public void onDisable() {
        this.getServer().getScheduler().cancelTasks(this);
        componentManager.unloadComponents();
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
    @SuppressWarnings({ "unchecked" })
    public void populateConfiguration() {
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
        this.config = config;

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

        loadItemList();

        opPermissions = config.getBoolean("op-permissions", true);
        useDisplayNames = config.getBoolean("use-display-names", true);
        lookupWithDisplayNames = config.getBoolean("lookup-with-display-names", true);
        broadcastChanges = config.getBoolean("broadcast-changes", true);

        crappyWrapperCompat = config.getBoolean("crappy-wrapper-compat", true);
        
        lowPriorityCommandRegistration = config.getBoolean("low-priority-command-registration", false);
        
        if (crappyWrapperCompat) {
            getLogger().info("Maximum wrapper compatibility is enabled. " +
                    "Some features have been disabled to be compatible with " +
                    "poorly written server wrappers.");
        }
    }

    /**
     * Loads the item list.
     */
    @SuppressWarnings({ "unchecked" })
    protected void loadItemList() {
        YAMLProcessor config = getGlobalConfiguration();

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
     * Create a default configuration file from the .jar.
     * 
     * @param name
     */
    public void createDefaultConfiguration(String name) {
        File actual = new File(getDataFolder(), name);
        if (!actual.exists()) {

            InputStream input = null;
            try {
                JarFile file = new JarFile(getFile());
                ZipEntry copy = file.getEntry("defaults/" + name);
                if (copy == null) throw new FileNotFoundException();
                input = file.getInputStream(copy);
            } catch (IOException e) {
                getLogger().severe("Unable to read default configuration: " + name);
            }
            if (input != null) {
                FileOutputStream output = null;

                try {
                    output = new FileOutputStream(actual);
                    byte[] buf = new byte[8192];
                    int length = 0;
                    while ((length = input.read(buf)) > 0) {
                        output.write(buf, 0, length);
                    }
                    
                    getLogger().info("Default configuration file written: " + name);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (input != null)
                            input.close();
                    } catch (IOException e) {}

                    try {
                        if (output != null)
                            output.close();
                    } catch (IOException e) {}
                }
            }
        }
    }
    
    /**
     * Checks permissions.
     * 
     * @param sender
     * @param perm
     * @return 
     */
    public boolean hasPermission(CommandSender sender, String perm) {
        if (!(sender instanceof Player)) {
            return ((sender.isOp() && (opPermissions || sender instanceof ConsoleCommandSender)) 
                    || getPermissionsResolver().hasPermission(sender.getName(), perm));
        } 
        return hasPermission(sender, ((Player) sender).getWorld(), perm);
    }

    public boolean hasPermission(CommandSender sender, World world, String perm) {
        if ((sender.isOp() && opPermissions) || sender instanceof ConsoleCommandSender) {
            return true;
        }

        // Invoke the permissions resolver
        if (sender instanceof Player) {
            Player player = (Player) sender;
            return getPermissionsResolver().hasPermission(world.getName(), player.getName(), perm);
        }

        return false;
    }

    /**
     * Checks permissions and throws an exception if permission is not met.
     * 
     * @param sender
     * @param perm
     * @throws CommandPermissionsException 
     */
    public void checkPermission(CommandSender sender, String perm)
            throws CommandPermissionsException {
        if (!hasPermission(sender, perm)) {
            throw new CommandPermissionsException();
        }
    }
    
    public void checkPermission(CommandSender sender, World world, String perm)
            throws CommandPermissionsException {
        if (!hasPermission(sender, world, perm)) {
            throw new CommandPermissionsException();
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
        int id = 0;
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
                id = (int) idTemp;
            } else {
                // Then check WorldEdit
                ItemType type = ItemType.lookup(name);

                if (type == null) {
                    throw new CommandException("No item type known by '" + name + "'");
                }

                id = type.getID();
            }
        }

        // If the user specified an item data or damage value, let's try
        // to parse it!
        if (dataName != null) {
            dmg = matchItemData(id, dataName);
        }

        ItemStack stack = new ItemStack(id, 1, (short)dmg);

        if (enchantmentName != null) {
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

        }

        return stack;
    }
    
    /**
     * Gets the IP address of a command sender.
     * 
     * @param sender
     * @return
     */
    public String toInetAddressString(CommandSender sender) {
        if (sender instanceof Player) {
            return ((Player) sender).getAddress().getAddress().getHostAddress();
        } else {
            return "127.0.0.1";
        }
    }

    /**
     * Get the permissions resolver.
     * 
     * @return
     */
    public PermissionsResolverManager getPermissionsResolver() {
        return PermissionsResolverManager.getInstance();
    }

    public YAMLProcessor getGlobalConfiguration() {
        return config;
    }

    public ComponentManager getComponentManager() {
        return componentManager;
    }
}
