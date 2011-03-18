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
import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;
import com.sk89q.bukkit.migration.PermissionsResolverManager;
import com.sk89q.bukkit.migration.PermissionsResolverServerListener;
import com.sk89q.minecraft.util.commands.*;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.blocks.BlockType;
import com.sk89q.worldedit.blocks.ClothColor;
import com.sk89q.worldedit.blocks.ItemType;
import static com.sk89q.commandbook.CommandBookUtil.*;

/**
 * Base plugin class for CommandBook.
 * 
 * @author sk89q
 */
public class CommandBookPlugin extends JavaPlugin {
    
    /**
     * Logger for messages.
     */
    protected static final Logger logger = Logger.getLogger("Minecraft.CommandBook");
    /**
     * The permissions resolver in use.
     */
    private PermissionsResolverManager perms;
    
    /**
     * List of commands.
     */
    protected CommandsManager<CommandSender> commands;
    
    /**
     * Allowed list of items IDs.
     */
    protected Set<Integer> allowedItems;
    
    /**
     * Disallowed list of items IDs.
     */
    protected Set<Integer> disallowedItems;
    
    /**
     * List of configurable item names.
     */
    protected Map<String, Integer> itemNames;
    
    /**
     * Holds various preprogrammed messages such as rules and help.
     */
    protected Map<String, String> messages = new HashMap<String, String>();
    
    /**
     * Stores the last user that was messaged per-user, which is used for
     * the /r command for quick replying.
     */
    protected Map<String, String> msgTargets = new HashMap<String, String>();

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
        
        // Load configuration
        populateConfiguration();
        
        // Prepare permissions
        perms = new PermissionsResolverManager(
                getConfiguration(), getServer(), getDescription().getName(), logger);
        
        // Register the commands that we want to use
        final CommandBookPlugin plugin = this;
        commands = new CommandsManager<CommandSender>() {
            @Override
            public boolean hasPermission(CommandSender player, String perm) {
                return plugin.hasPermission(player, perm);
            }
        };
        
        commands.register(GeneralCommands.class);
        commands.register(TeleportCommands.class);
        commands.register(MessageCommands.class);
        commands.register(DebuggingCommands.class);
        
        // Register events
        registerEvents();

        // The permissions resolver has some hooks of its own
        (new PermissionsResolverServerListener(perms)).register(this);
    }
    
    /**
     * Register the events that are used.
     */
    protected void registerEvents() {
        PlayerListener playerListener = new CommandBookPlayerListener(this);

        registerEvent(Event.Type.PLAYER_JOIN, playerListener);
        registerEvent(Event.Type.PLAYER_QUIT, playerListener);
    }

    /**
     * Called when the plugin is disabled. Shutdown and clearing of any
     * temporary data occurs here.
     */
    public void onDisable() {
    }
    
    /**
     * Called on a command.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command cmd,
            String commandLabel, String[] args) {
        try {
            commands.execute(cmd.getName(), args, sender, this, sender);
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
     * Register an event.
     * 
     * @param type
     * @param listener
     * @param priority
     */
    protected void registerEvent(Event.Type type, Listener listener, Priority priority) {
        getServer().getPluginManager()
                .registerEvent(type, listener, priority, this);
    }
    
    /**
     * Register an event at normal priority.
     * 
     * @param type
     * @param listener
     */
    protected void registerEvent(Event.Type type, Listener listener) {
        getServer().getPluginManager()
                .registerEvent(type, listener, Priority.Normal, this);
    }
    
    /**
     * Loads the configuration.
     */
    @SuppressWarnings("unchecked")
    protected void populateConfiguration() {
        Configuration config = getConfiguration();
        config.load();
        
        // Load item disallow/allow lists
        allowedItems = new HashSet<Integer>(
                config.getIntList("allowed-items", null));
        disallowedItems = new HashSet<Integer>(
                config.getIntList("disallowed-items", null));
        
        // Load item names aliases list
        Object itemNamesTemp = config.getProperty("item-names");
        if (itemNamesTemp != null && itemNamesTemp instanceof Map) {
            try {
                itemNames = (Map<String, Integer>) itemNamesTemp;
            } catch (ClassCastException e) {
                itemNames = new HashMap<String, Integer>();
            }
        } else {
            itemNames = new HashMap<String, Integer>();
        }
        
        // Load messages
        messages.put("motd", config.getString("motd", null));
        messages.put("rules", config.getString("rules", null));
    }
    
    /**
     * Create a default configuration file from the .jar.
     * 
     * @param name
     */
    protected void createDefaultConfiguration(String name) {
        File actual = new File(getDataFolder(), name);
        if (!actual.exists()) {
            
            InputStream input =
                    this.getClass().getResourceAsStream("/defaults/" + name);
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
        if (sender.isOp()) {
            return true;
        }
        
        // Invoke the permissions resolver
        if (sender instanceof Player) {
            return perms.hasPermission(((Player) sender).getName(), perm);
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
    
    /**
     * Checks to see if a user can use an item.
     * 
     * @param sender
     * @param id
     * @throws CommandException 
     */
    public void checkAllowedItem(CommandSender sender, int id)
            throws CommandException {
        
        if (id < 1 || (id > 27 && id < 35) || id == 36
                || (id > 94 && id < 256)
                || (id > 356 && id < 2256)
                || id > 2257) {
            throw new CommandException("Non-existent item specified.");
        }

        // Check if the user has an override
        if (hasPermission(sender, "commandbook.override.any-item")) {
            return;
        }

        // Also check the permissions system
        if (hasPermission(sender, "commandbook.items." + id)) {
            return;
        }
        
        if (allowedItems.size() > 0) {
            if (!allowedItems.contains(id)) {
                throw new CommandException("That item is not allowed.");
            }
        }
        
        if (disallowedItems.contains((id))) {
            throw new CommandException("That item is disallowed.");
        }
    }
    
    /**
     * Checks to see if the sender is a player, otherwise throw an exception.
     * 
     * @param sender
     * @return 
     * @throws CommandException 
     */
    public Player checkPlayer(CommandSender sender)
            throws CommandException {
        if (sender instanceof Player) {
            return (Player) sender;
        } else {
            throw new CommandException("A player is expected.");
        }
    }
    
    /**
     * Match player names.
     * 
     * @param filter
     * @return
     */
    public List<Player> matchPlayerNames(String filter) {
        Player[] players = getServer().getOnlinePlayers();

        filter = filter.toLowerCase();
        
        // Allow exact name matching
        if (filter.charAt(0) == '@' && filter.length() >= 2) {
            filter = filter.substring(1);
            
            for (Player player : players) {
                if (player.getName().equalsIgnoreCase(filter)) {
                    List<Player> list = new ArrayList<Player>();
                    list.add(player);
                    return list;
                }
            }
            
            return new ArrayList<Player>();
        // Allow partial name matching
        } else if (filter.charAt(0) == '*' && filter.length() >= 2) {
            filter = filter.substring(1);
            
            List<Player> list = new ArrayList<Player>();
            
            for (Player player : players) {
                if (player.getName().toLowerCase().contains(filter)) {
                    list.add(player);
                }
            }
            
            return list;
        
        // Start with name matching
        } else {
            List<Player> list = new ArrayList<Player>();
            
            for (Player player : players) {
                if (player.getName().toLowerCase().startsWith(filter)) {
                    list.add(player);
                }
            }
            
            return list;
        }
    }
    
    /**
     * Checks if the given list of players is greater than size 0, otherwise
     * throw an exception.
     * 
     * @param players
     * @return 
     * @throws CommandException
     */
    protected Iterable<Player> checkPlayerMatch(List<Player> players)
            throws CommandException {
        // Check to see if there were any matches
        if (players.size() == 0) {
            throw new CommandException("No players matched query.");
        }
        
        return players;
    }
    
    /**
     * Checks permissions and throws an exception if permission is not met.
     * 
     * @param source 
     * @param filter
     * @return iterator for players
     * @throws CommandException no matches found
     */
    public Iterable<Player> matchPlayers(CommandSender source, String filter)
            throws CommandException {
        
        if (getServer().getOnlinePlayers().length == 0) {
            throw new CommandException("No players matched query.");
        }
        
        if (filter.equals("*")) {
            return checkPlayerMatch(Arrays.asList(getServer().getOnlinePlayers()));
        }

        // Handle special hash tag groups
        if (filter.charAt(0) == '#') {
            // Handle #world, which matches player of the same world as the
            // calling source
            if (filter.equalsIgnoreCase("#world")) {
                List<Player> players = new ArrayList<Player>();
                Player sourcePlayer = checkPlayer(source);
                World sourceWorld = sourcePlayer.getWorld();
                
                for (Player player : getServer().getOnlinePlayers()) {
                    if (player.getWorld().equals(sourceWorld)) {
                        players.add(player);
                    }
                }

                return checkPlayerMatch(players);
            
            // Handle #near, which is for nearby players.
            } else if (filter.equalsIgnoreCase("#near")) {
                List<Player> players = new ArrayList<Player>();
                Player sourcePlayer = checkPlayer(source);
                World sourceWorld = sourcePlayer.getWorld();
                org.bukkit.util.Vector sourceVector
                        = sourcePlayer.getLocation().toVector();
                
                for (Player player : getServer().getOnlinePlayers()) {
                    if (player.getWorld().equals(sourceWorld)
                            && player.getLocation().toVector().distanceSquared(
                                    sourceVector) < 900) {
                        players.add(player);
                    }
                }

                return checkPlayerMatch(players);
            
            } else {
                throw new CommandException("Invalid group '" + filter + "'.");
            }
        }
        
        List<Player> players = matchPlayerNames(filter);
        
        return checkPlayerMatch(players);
    }
    
    /**
     * Match only a single player.
     * 
     * @param sender
     * @param filter
     * @return
     * @throws CommandException
     */
    public Player matchSinglePlayer(CommandSender sender, String filter)
            throws CommandException {
        // This will throw an exception if there are no matches
        Iterator<Player> players = matchPlayers(sender, filter).iterator();
        
        Player match = players.next();
        
        // We don't want to match the wrong person, so fail if if multiple
        // players were found (we don't want to just pick off the first one,
        // as that may be the wrong player)
        if (players.hasNext()) {
            throw new CommandException("More than one player found! " +
            		"Use @<name> for exact matching.");
        }
        
        return match;
    }
    
    /**
     * Match only a single player or console.
     * 
     * @param sender
     * @param filter
     * @return
     * @throws CommandException
     */
    public CommandSender matchPlayerOrConsole(CommandSender sender, String filter)
            throws CommandException {
        
        // Let's see if console is wanted
        if (filter.equalsIgnoreCase("#console")
                || filter.equalsIgnoreCase("*console*")
                || filter.equalsIgnoreCase("!")) {
            return new ConsoleCommandSender(getServer());
        }
        
        return matchSinglePlayer(sender, filter);
    }
    
    /**
     * Get a single player as an iterator for players.
     * 
     * @param player
     * @return iterator for players
     */
    public Iterable<Player> matchPlayers(Player player) {
        return Arrays.asList(new Player[] {player});
    }
    
    /**
     * Match a target.
     * 
     * @param source 
     * @param filter
     * @return iterator for players
     * @throws CommandException no matches found
     */
    public Location matchLocation(CommandSender source, String filter)
            throws CommandException {

        // Handle coordinates
        if (filter.matches("^[\\-0-9\\.]+,[\\-0-9\\.]+,[\\-0-9\\.]+$")) {
            checkPermission(source, "commandbook.locations.coords");
            
            String[] parts = filter.split(",");
            double x, y, z;
            
            try {
                x = Double.parseDouble(parts[0]);
                y = Double.parseDouble(parts[1]);
                z = Double.parseDouble(parts[2]);
            } catch (NumberFormatException e) {
                throw new CommandException("Coordinates expected numbers!");
            }

            Player player = checkPlayer(source);
            return new Location(player.getWorld(), x, y, z);
            
        // Handle special hash tag groups
        } else if (filter.charAt(0) == '#') {
            checkPermission(source, "commandbook.spawn");

            // Handle #world, which matches player of the same world as the
            // calling source
            if (filter.equalsIgnoreCase("#spawn")) {
                Player sourcePlayer = checkPlayer(source);

                return sourcePlayer.getLocation().getWorld().getSpawnLocation();

            // Handle #target, which matches the player's target position
            } else if (filter.equalsIgnoreCase("#target")) {
                Player player = checkPlayer(source);
                Location playerLoc = player.getLocation();
                Block targetBlock = player.getTargetBlock(null, 100);
                
                if (targetBlock == null) {
                    throw new CommandException("Failed to find a block in your target!");
                } else {
                    Location loc = targetBlock.getLocation();
                    playerLoc.setX(loc.getX());
                    playerLoc.setY(loc.getY());
                    playerLoc.setZ(loc.getZ());
                    return CommandBookUtil.findFreePosition(playerLoc);
                }
            
            } else {
                throw new CommandException("Invalid group '" + filter + "'.");
            }
        }
        
        List<Player> players = matchPlayerNames(filter);
        
        // Check to see if there were any matches
        if (players.size() == 0) {
            throw new CommandException("No players matched query.");
        }
        
        return players.get(0).getLocation();
    }
    
    /**
     * Match a world.
     * @param sender 
     * 
     * @param filter
     * @return
     * @throws CommandException 
     */
    public World matchWorld(CommandSender sender, String filter) throws CommandException {
        List<World> worlds = getServer().getWorlds();

        // Handle special hash tag groups
        if (filter.charAt(0) == '#') {
            // #main for the main world
            if (filter.equalsIgnoreCase("#main")) {
                return worlds.get(0);
            
            // #normal for the first normal world
            } else if (filter.equalsIgnoreCase("#normal")) {
                for (World world : worlds) {
                    if (world.getEnvironment() == Environment.NORMAL) {
                        return world;
                    }
                }

                throw new CommandException("No normal world found.");
            
            // #nether for the first nether world
            } else if (filter.equalsIgnoreCase("#nether")) {
                for (World world : worlds) {
                    if (world.getEnvironment() == Environment.NETHER) {
                        return world;
                    }
                }

                throw new CommandException("No nether world found.");

            // Handle getting a world from a player
            } else if (filter.matches("^#player$")) {
                String parts[] = filter.split(":", 2);
                
                // They didn't specify an argument for the player!
                if (parts.length == 1) {
                    throw new CommandException("Argument expected for #player.");
                }
                
                return matchPlayers(sender, parts[1]).iterator().next().getWorld();
            } else {
                throw new CommandException("Invalid identifier '" + filter + "'.");
            }
        }
        
        for (World world : worlds) {
            if (world.getName().equals(filter)) {
                return world;
            }
        }
        
        throw new CommandException("No world by that exact name found.");
    }
    
    /**
     * Gets the name of a command sender. This is a unique name and this
     * method should never return a "display name".
     * 
     * @param sender
     * @return
     */
    public String toUniqueName(CommandSender sender) {
        if (sender instanceof Player) {
            return ((Player) sender).getName();
        } else {
            return "*Console*";
        }
    }
    
    /**
     * Gets the name of a command sender. This play be a display name.
     * 
     * @param sender
     * @return
     */
    public String toName(CommandSender sender) {
        if (sender instanceof Player) {
            return ((Player) sender).getName();
        } else {
            return "*Console*";
        }
    }
    
    /**
     * Gets the name of an item.
     * 
     * @param id
     * @return
     */
    public String toItemName(int id) {
        ItemType type = ItemType.fromID(id);
        
        if (type != null) {
            return type.getName();
        } else {
            return "#" + id;
        }
    }
    
    /**
     * Matches an item and gets the appropriate item stack.
     * 
     * @param source 
     * @param name
     * @return iterator for players
     * @throws CommandException 
     */
    public ItemStack matchItem(CommandSender source, String name)
            throws CommandException {

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
            }
            
            // Then check WorldEdit
            ItemType type = ItemType.lookup(name);
            
            if (type == null) {
                throw new CommandException("No item type known by '" + name + "'");
            }
            
            id = type.getID();
        }
        
        // If the user specified an item data or damage value, let's try
        // to parse it!
        if (dataName != null) {            
            dmg = matchItemData(id, dataName);
        }
        
        return new ItemStack(id, 1, (short)dmg, (byte)dmg);
    }
    
    /**
     * Attempt to match item data values.
     * 
     * @param id
     * @param filter
     * @return
     * @throws CommandException 
     */
    public int matchItemData(int id, String filter) throws CommandException {
        try {
            // First let's try the filter as if it was a number
            return Integer.parseInt(filter);
        } catch (NumberFormatException e) {
        }

        // So the value isn't a number, but it may be an alias!
        switch (id) {
            case BlockID.WOOD:
                if (filter.equalsIgnoreCase("redwood")) {
                    return 1;
                } else if (filter.equalsIgnoreCase("birch")) {
                    return 2;
                }
                
                throw new CommandException("Unknown wood type name of '" + filter + "'.");
            case BlockID.STEP:
            case BlockID.DOUBLE_STEP:
                BlockType dataType = BlockType.lookup(filter);
                
                if (dataType != null) {
                    if (dataType == BlockType.STONE) {
                        return 0;
                    } else if (dataType == BlockType.SANDSTONE) {
                        return 1;
                    } else if (dataType == BlockType.WOOD) {
                        return 2;
                    } else if (dataType == BlockType.COBBLESTONE) {
                        return 3;
                    } else {
                        throw new CommandException("Invalid slab material of '" + filter + "'.");
                    }
                } else {
                    throw new CommandException("Unknown slab material of '" + filter + "'.");
                }
            case BlockID.CLOTH:
                ClothColor col = ClothColor.lookup(filter);
                if (col != null) {
                    return col.getID();
                }
                
                throw new CommandException("Unknown wool color name of '" + filter + "'.");
            case 351: // Dye
                ClothColor dyeCol = ClothColor.lookup(filter);
                if (dyeCol != null) {
                    return 15 - dyeCol.getID();
                }
                
                throw new CommandException("Unknown dye color name of '" + filter + "'.");
            default: 
                throw new CommandException("Invalid data value of '" + filter + "'.");
        }
    }
    
    /**
     * Get preprogrammed messages.
     * 
     * @param id 
     * @return may return null
     */
    public String getMessage(String id) {
        return messages.get(id);
    }
    
    /**
     * Get a map of message targets.
     * 
     * @return
     */
    public Map<String, String> getMessageTargets() {
        return msgTargets;
    }
    
    /**
     * Replace macros in the text.
     * 
     * @param sender 
     * @param message
     * @return
     */
    public String replaceMacros(CommandSender sender, String message) {
        message = message.replace("%name%", toName(sender));
        
        if (sender instanceof Player) {
            Player player = (Player) sender;
            World world = player.getWorld();

            message = message.replace("%time%", getTimeString(world.getTime()));
            message = message.replace("%world%", world.getName());
        }
        
        return message;
    }
}
