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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;

import com.sk89q.bukkit.util.CommandRegistration;
import com.sk89q.bukkit.util.CommandsManagerRegistration;
import com.sk89q.commandbook.components.*;
import com.sk89q.commandbook.events.ComponentManagerInitEvent;
import com.sk89q.commandbook.events.core.EventManager;
import com.sk89q.commandbook.session.SessionComponent;
import com.sk89q.util.yaml.YAMLFormat;
import com.sk89q.util.yaml.YAMLProcessor;
import com.sk89q.wepif.PermissionsResolverManager;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;
import com.sk89q.commandbook.commands.*;
import com.sk89q.minecraft.util.commands.*;
import com.sk89q.worldedit.blocks.ItemType;

import static com.sk89q.commandbook.util.ItemUtil.matchItemData;

/**
 * Base plugin class for CommandBook.
 * 
 * @author sk89q
 */
@SuppressWarnings("deprecation")
public final class CommandBook extends JavaPlugin {
    
    private static final Logger logger = Logger.getLogger("Minecraft.CommandBook");
    
    private static CommandBook instance;

    private CommandsManager<CommandSender> commands;
    
    protected Map<String, Integer> itemNames;
    public boolean broadcastChanges;
    public boolean opPermissions;
    public boolean useDisplayNames;
    public boolean crappyWrapperCompat;

    protected YAMLProcessor config;
    protected EventManager eventManager = new EventManager();
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
        return logger;
    }

    /**
     * Called when the plugin is enabled. This is where configuration is loaded,
     * and the plugin is setup.
     */
    public void onEnable() {
        logger.info(getDescription().getName() + " "
                + getDescription().getVersion() + " enabled.");
        
        // Make the data folder for the plugin where configuration files
        // and other data files will be stored
        getDataFolder().mkdirs();

        createDefaultConfiguration("config.yml");
        createDefaultConfiguration("kits.txt");

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
        
        componentManager = new ComponentManager();
        
        // -- Component loaders
        componentManager.addComponentLoader(new StaticComponentLoader(new SessionComponent()));
        componentManager.addComponentLoader(new ConfigListedComponentLoader(new YAMLProcessor(null, false) {
            @Override
            public InputStream getInputStream() {
                return CommandBook.inst().getClass().getResourceAsStream("/defaults/modules.yml");
            }
        }));
        
        // -- Annotation handlers
        componentManager.registerAnnotationHandler(InjectComponent.class, new InjectComponentAnnotationHandler());

        getEventManager().callEvent(new ComponentManagerInitEvent(componentManager));

        // Load configuration
        populateConfiguration();

        componentManager.loadComponents();
        
		final CommandsManagerRegistration cmdRegister = new CommandsManagerRegistration(this, commands);
        cmdRegister.register(GeneralCommands.class);

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
        YAMLProcessor config = new YAMLProcessor(new File(getDataFolder(), "config.yml"), true, YAMLFormat.EXTENDED);
        try {
            config.load();
        } catch (IOException e) {
            logger.log(Level.WARNING, "CommandBook: Error loading configuration: ", e);
        }
        this.config = config;

        loadItemList();

        opPermissions = config.getBoolean("op-permissions", true);
        useDisplayNames = config.getBoolean("use-display-names", true);
        broadcastChanges = config.getBoolean("broadcast-changes", true);

        crappyWrapperCompat = config.getBoolean("crappy-wrapper-compat", true);
        
        if (crappyWrapperCompat) {
            logger.info("CommandBook: Maximum wrapper compatibility is enabled. " +
                    "Some features have been disabled to be compatible with " +
                    "poorly written server wrappers.");
        }
    }

    /**
     * Loads the item list.
     */
    @SuppressWarnings({ "unchecked" })
    protected void loadItemList() {
        Configuration config = getConfiguration();

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
                logger.severe(getDescription().getName() + ": Unable to read default configuration: " + name);
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
                    
                    logger.info(getDescription().getName()
                            + ": Default configuration file written: " + name);
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
                    || PermissionsResolverManager.getInstance().hasPermission(sender.getName(), perm));
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
            return PermissionsResolverManager.getInstance().hasPermission(world.getName(), player.getName(), perm);
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

        int id = 0;
        int dmg = 0;
        String dataName = null;

        if (name.contains(":")) {
            String[] parts = name.split(":");
            dataName = parts[1];
            name = parts[0];
        }

        try {
            id = Integer.parseInt(name);
        } catch (NumberFormatException e) {
            // First check the configurable list of aliases
            Integer idTemp = itemNames.get(name.toLowerCase());

            if (idTemp != null) {
                id = (int) idTemp;
            } else {
                // Then check WorldEdit
                ItemType type = ItemType.lookup(name);

                if (type == null) {
                    return null;
                }

                id = type.getID();
            }
        }

        // If the user specified an item data or damage value, let's try
        // to parse it!
        if (dataName != null) {
            try {
                dmg = matchItemData(id, dataName);
            } catch (CommandException e) {
                return null;
            }
        }
        return new ItemStack(id, 1, (short)dmg);
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
    
    public EventManager getEventManager() {
        return eventManager;
    }

    public ComponentManager getComponentManager() {
        return componentManager;
    }
}
