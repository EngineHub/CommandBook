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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.CreatureType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.world.WorldListener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;
import com.sk89q.bukkit.migration.PermissionsResolverManager;
import com.sk89q.bukkit.migration.PermissionsResolverServerListener;
import com.sk89q.commandbook.bans.BanDatabase;
import com.sk89q.commandbook.bans.FlatFileBanDatabase;
import com.sk89q.commandbook.commands.*;
import com.sk89q.commandbook.kits.FlatFileKitsManager;
import com.sk89q.commandbook.kits.KitManager;
import com.sk89q.commandbook.locations.FlatFileLocationsManager;
import com.sk89q.commandbook.locations.LocationManager;
import com.sk89q.commandbook.locations.LocationManagerFactory;
import com.sk89q.commandbook.locations.RootLocationManager;
import com.sk89q.commandbook.locations.NamedLocation;
import com.sk89q.jinglenote.JingleNoteManager;
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
     * Pattern for 12-hour time.
     */
    private static final Pattern twelveHourTime
        = Pattern.compile("^([0-9]+(?::[0-9]+)?)([apmAPM\\.]+)$");
    
    /**
     * Pattern for macros.
     */
    protected static final Pattern macroPattern =
            Pattern.compile("%([^% ]+)%");
    
    /**
     * The permissions resolver in use.
     */
    private PermissionsResolverManager perms;
    
    /**
     * List of commands.
     */
    protected CommandsManager<CommandSender> commands;
    
    /**
     * Bans database.
     */
    protected BanDatabase bans;
    
    /**
     * Warps database.
     */
    protected RootLocationManager<NamedLocation> warps;
    
    /**
     * Homes database.
     */
    protected RootLocationManager<NamedLocation> homes;
    
    /**
     * Time lock manager.
     */
    protected TimeLockManager timeLockManager;
    
    /**
     * Jingle note manager.
     */
    protected JingleNoteManager jingleNoteManager;
    
    public boolean disableMidi;
    public boolean verifyNameFormat;
    public boolean broadcastChanges;
    public boolean broadcastKicks;
    public boolean broadcastBans;
    public boolean useItemPermissionsOnly;
    public Set<Integer> allowedItems;
    public Set<Integer> disallowedItems;
    public Map<String, Integer> itemNames;
    public Set<Integer> thorItems;
    public KitManager kits;
    public String banMessage;
    public boolean opPermissions;
    public boolean useDisplayNames;
    public String consoleSayFormat;
    public String broadcastFormat;
    public int defaultItemStackSize;
    public boolean exactSpawn;
    public boolean playersListColoredNames;
    public boolean playersListGroupedNames;
    public boolean playersListMaxPlayers;
    public boolean crappyWrapperCompat;

    protected Map<String, String> messages = new HashMap<String, String>();
    protected Map<String, UserSession> sessions =
        new HashMap<String, UserSession>();
    protected Map<String, AdministrativeSession> adminSessions =
        new HashMap<String, AdministrativeSession>();;
    protected Map<String, Integer> lockedTimes =
        new HashMap<String, Integer>();

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
        
        // Setup the time locker
        timeLockManager = new TimeLockManager(this);
        
        // Load configuration
        populateConfiguration();
        
        // Setup the ban database
        bans = new FlatFileBanDatabase(getDataFolder(), this);
        bans.load();
        
        // Setup kits
        kits = new FlatFileKitsManager(new File(getDataFolder(), "kits.txt"), this);
        kits.load();
        
        // Jingle note manager
        jingleNoteManager = new JingleNoteManager(this);
        
        // Prepare permissions
        perms = new PermissionsResolverManager(
                getConfiguration(), getServer(), getDescription().getName(), logger);
        perms.load();
        
        // Register the commands that we want to use
        final CommandBookPlugin plugin = this;
        commands = new CommandsManager<CommandSender>() {
            @Override
            public boolean hasPermission(CommandSender player, String perm) {
                return plugin.hasPermission(player, perm);
            }
        };
        
        commands.register(GeneralCommands.class);
        commands.register(FunCommands.class);
        commands.register(TeleportCommands.class);
        commands.register(MessageCommands.class);
        commands.register(DebuggingCommands.class);
        commands.register(ModerationCommands.class);
        commands.register(KitCommands.class);
        commands.register(WarpCommands.class);
        commands.register(HomeCommands.class);
        
        // Register events
        registerEvents();
        
        // Cleanup
        getServer().getScheduler().scheduleAsyncRepeatingTask(
                this, new SessionChecker(this),
                SessionChecker.CHECK_FREQUENCY, SessionChecker.CHECK_FREQUENCY);
        getServer().getScheduler().scheduleAsyncRepeatingTask(
                this, new GarbageCollector(this),
                GarbageCollector.CHECK_FREQUENCY, GarbageCollector.CHECK_FREQUENCY);

        // The permissions resolver has some hooks of its own
        (new PermissionsResolverServerListener(perms)).register(this);
    }
    
    /**
     * Register the events that are used.
     */
    protected void registerEvents() {
        PlayerListener playerListener = new CommandBookPlayerListener(this);
        WorldListener worldListener = new CommandBookWorldListener(this);

        registerEvent(Event.Type.PLAYER_LOGIN, playerListener);
        registerEvent(Event.Type.PLAYER_JOIN, playerListener);
        registerEvent(Event.Type.PLAYER_INTERACT, playerListener);
        registerEvent(Event.Type.PLAYER_QUIT, playerListener);
        registerEvent(Event.Type.PLAYER_CHAT, playerListener);
        registerEvent(Event.Type.PLAYER_RESPAWN, playerListener);
        registerEvent(Event.Type.WORLD_LOAD, worldListener);
    }

    /**
     * Called when the plugin is disabled. Shutdown and clearing of any
     * temporary data occurs here.
     */
    public void onDisable() {
        jingleNoteManager.stopAll();
        this.getServer().getScheduler().cancelTasks(this);
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
    public void populateConfiguration() {
        Configuration config = getConfiguration();
        config.load();
        
        // Load item disallow/allow lists
        useItemPermissionsOnly = config.getBoolean("item-permissions-only", false);
        allowedItems = new HashSet<Integer>(
                config.getIntList("allowed-items", null));
        disallowedItems = new HashSet<Integer>(
                config.getIntList("disallowed-items", null));
        
        loadItemList();
        
        // Load messages
        messages.put("motd", config.getString("motd", null));
        messages.put("rules", config.getString("rules", null));

        playersListColoredNames = config.getBoolean("online-list.colored-names", false);
        playersListGroupedNames = config.getBoolean("online-list.grouped-names", false);
        playersListMaxPlayers = config.getBoolean("online-list.show-max-players", true);
        
        opPermissions = config.getBoolean("op-permissions", true);
        useDisplayNames = config.getBoolean("use-display-names", true);
        banMessage = config.getString("bans.message", "You were banned.");
        disableMidi = config.getBoolean("disable-midi", false);
        verifyNameFormat = config.getBoolean("verify-name-format", true);
        broadcastChanges = config.getBoolean("broadcast-changes", true);
        broadcastBans = config.getBoolean("broadcast-bans", false);
        broadcastKicks = config.getBoolean("broadcast-kicks", false);
        consoleSayFormat = config.getString("console-say-format", "<`r*Console`w> %s");
        broadcastFormat = config.getString("broadcast-format", "`r[Broadcast] %s");
        defaultItemStackSize = config.getInt("default-item-stack-size", 1);
        exactSpawn = config.getBoolean("exact-spawn", false);
        crappyWrapperCompat = config.getBoolean("crappy-wrapper-compat", true);
        thorItems = new HashSet<Integer>(config.getIntList(
                "thor-hammer-items", Arrays.asList(new Integer[]{278, 285, 257, 270})));

        LocationManagerFactory<LocationManager<NamedLocation>> warpsFactory =
                new FlatFileLocationsManager.WarpsFactory(getDataFolder(), this);
        warps = new RootLocationManager<NamedLocation>(warpsFactory,
                config.getBoolean("per-world-warps", false));

        LocationManagerFactory<LocationManager<NamedLocation>> homesFactory =
                new FlatFileLocationsManager.HomesFactory(getDataFolder(), this);
        homes = new RootLocationManager<NamedLocation>(homesFactory,
                config.getBoolean("per-world-homes", false));
        
        if (disableMidi) {
            logger.info("CommandBook: MIDI support is disabled.");
        }
        
        if (crappyWrapperCompat) {
            logger.info("CommandBook: Maximum wrapper compatibility is enabled. " +
                    "Some features have been disabled to be compatible with " +
                    "poorly written server wrappers.");
        }
        
        Object timeLocks = config.getProperty("time-lock");
        
        if (timeLocks != null && timeLocks instanceof Map) {
            for (Map.Entry<String, Object> entry : ((Map<String, Object>) timeLocks).entrySet()) {
                int time = 0;
                
                try {
                    time = matchTime(String.valueOf(entry.getValue()));
                } catch (CommandException e) {
                    logger.warning("CommandBook: Time lock: Failed to parse time '"
                            + entry.getValue() + "'");
                }
                
                lockedTimes.put(entry.getKey(), time);
                
                World world = getServer().getWorld(entry.getKey());
                
                if (world == null) {
                    logger.info("CommandBook: Could not time-lock unknown world '"
                            + entry.getKey() + "'");
                    continue;
                }
                
                world.setTime(time);
                timeLockManager.lock(world);
                logger.info("CommandBook: Time locked to '"
                        + CommandBookUtil.getTimeString(time) + "' for world '"
                        + world.getName() + "'");
            }
        }
    }
    
    /**
     * Loads the item list.
     */
    @SuppressWarnings("unchecked")
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
        if (!(sender instanceof Player)) {
            return true;
        }
        
        if (sender.isOp() && opPermissions) {
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
        
        if (id < 1 || (id > 96 && id < 256)
                || (id > 359 && id < 2256)
                || id > 2257) {
            if (Material.getMaterial(id) == null || id == 0) {
                throw new CommandException("Non-existent item specified.");
            }
        }

        // Check if the user has an override
        if (hasPermission(sender, "commandbook.override.any-item")) {
            return;
        }

        boolean hasPermissions = hasPermission(sender, "commandbook.items." + id);
        
        // Also check the permissions system
        if (hasPermissions) {
            return;
        }
        
        if (useItemPermissionsOnly) {
            if (!hasPermissions) {
                throw new CommandException("That item is not allowed.");
            }
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
            throw new CommandException("A player context is required. (Specify a world or player if the command supports it.)");
        }
    }
    
    /**
     * Attempts to match a creature type.
     * 
     * @param sender
     * @param filter
     * @return
     * @throws CommandException
     */
    public CreatureType matchCreatureType(CommandSender sender,
            String filter) throws CommandException {
        CreatureType type = CreatureType.fromName(filter);
        if (type != null) {
            return type;
        }
        
        for (CreatureType testType : CreatureType.values()) {
            if (testType.getName().toLowerCase().startsWith(filter.toLowerCase())) {
                return testType;
            }
        }

        throw new CommandException("Unknown mob specified! You can "
                + "choose from the list of: "
                + CommandBookUtil.getCreatureTypeNameList());
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
     * Match a single player exactly.
     * 
     * @param sender
     * @param filter
     * @return
     * @throws CommandException
     */
    public Player matchPlayerExactly(CommandSender sender, String filter)
            throws CommandException {
        Player[] players = getServer().getOnlinePlayers();
        for (Player player : players) {
            if (player.getName().equalsIgnoreCase(filter)) {
                return player;
            }
        }
    
        throw new CommandException("No player found!");
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
        if (filter.matches("^[\\-0-9\\.]+,[\\-0-9\\.]+,[\\-0-9\\.]+(?:.+)?$")) {
            checkPermission(source, "commandbook.locations.coords");
            
            String[] args = filter.split(":");
            String[] parts = args[0].split(",");
            double x, y, z;
            
            try {
                x = Double.parseDouble(parts[0]);
                y = Double.parseDouble(parts[1]);
                z = Double.parseDouble(parts[2]);
            } catch (NumberFormatException e) {
                throw new CommandException("Coordinates expected numbers!");
            }

            if (args.length > 1) {
                return new Location(matchWorld(source, args[1]), x, y, z);
            } else {
                Player player = checkPlayer(source);
                return new Location(player.getWorld(), x, y, z);
            }
            
        // Handle special hash tag groups
        } else if (filter.charAt(0) == '#') {
            checkPermission(source, "commandbook.spawn");
            
            String[] args = filter.split(":");

            // Handle #world, which matches player of the same world as the
            // calling source
            if (args[0].equalsIgnoreCase("#spawn")) {
                if (args.length > 1) {
                    return matchWorld(source, args[1]).getSpawnLocation();
                } else {
                    Player sourcePlayer = checkPlayer(source);
                    return sourcePlayer.getLocation().getWorld().getSpawnLocation();
                }

            // Handle #target, which matches the player's target position
            } else if (args[0].equalsIgnoreCase("#target")) {
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
            // Handle #me, which is for when a location argument is required
            } else if (args[0].equalsIgnoreCase("#me")) {
                return checkPlayer(source).getLocation();
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

            // #skylands for the first skylands world
            } else if (filter.equalsIgnoreCase("#skylands")) {
                for (World world : worlds) {
                    if (world.getEnvironment() == Environment.SKYLANDS) {
                        return world;
                    }
                }

                throw new CommandException("No skylands world found.");
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
     * Parse a time string.
     * 
     * @param timeStr
     * @return
     * @throws CommandException
     */
    public int matchTime(String timeStr) throws CommandException {
        Matcher matcher;
        
        try {
            int time = Integer.parseInt(timeStr);
            
            // People tend to enter just a number of the hour
            if (time <= 24) {
                return ((time - 8) % 24) * 1000;
            }
            
            return time;
        } catch (NumberFormatException e) {
            // Not an integer!
        }
        
        // Tick time
        if (timeStr.matches("^*[0-9]+$")) {
            return Integer.parseInt(timeStr.substring(1));
        
        // Allow 24-hour time
        } else if (timeStr.matches("^[0-9]+:[0-9]+$")) {
            String[] parts = timeStr.split(":");
            int hours = Integer.parseInt(parts[0]);
            int mins = Integer.parseInt(parts[1]);
            int n = (int) (((hours - 8) % 24) * 1000
                + Math.round((mins % 60) / 60.0 * 1000));
            return n;
        
        // Or perhaps 12-hour time
        } else if ((matcher = twelveHourTime.matcher(timeStr)).matches()) {
            String time = matcher.group(1);
            String period = matcher.group(2);
            int shift = 0;
            
            if (period.equalsIgnoreCase("am")
                    || period.equalsIgnoreCase("a.m.")) {
                shift = 0;
            } else if (period.equalsIgnoreCase("pm")
                    || period.equalsIgnoreCase("p.m.")) {
                shift = 12;
            } else {
                throw new CommandException("'am' or 'pm' expected, got '"
                        + period + "'.");
            }
            
            String[] parts = time.split(":");
            int hours = Integer.parseInt(parts[0]);
            int mins = parts.length >= 2 ? Integer.parseInt(parts[1]) : 0;
            int n = (int) ((((hours % 12) + shift - 8) % 24) * 1000
                + (mins % 60) / 60.0 * 1000);
            return n;
        
        // Or some shortcuts
        } else if (timeStr.equalsIgnoreCase("dawn")) {
            return (6 - 8 + 24) * 1000;
        } else if (timeStr.equalsIgnoreCase("sunrise")) {
            return (7 - 8 + 24) * 1000;
        } else if (timeStr.equalsIgnoreCase("morning")) {
            return (8 - 8 + 24) * 1000;
        } else if (timeStr.equalsIgnoreCase("day")) {
            return (8 - 8 + 24) * 1000;
        } else if (timeStr.equalsIgnoreCase("midday")
                || timeStr.equalsIgnoreCase("noon")) {
            return (12 - 8 + 24) * 1000;
        } else if (timeStr.equalsIgnoreCase("afternoon")) {
            return (14 - 8 + 24) * 1000;
        } else if (timeStr.equalsIgnoreCase("evening")) {
            return (16 - 8 + 24) * 1000;
        } else if (timeStr.equalsIgnoreCase("sunset")) {
            return (21 - 8 + 24) * 1000;
        } else if (timeStr.equalsIgnoreCase("dusk")) {
            return (21 - 8 + 24) * 1000 + (int) (30 / 60.0 * 1000);
        } else if (timeStr.equalsIgnoreCase("night")) {
            return (22 - 8 + 24) * 1000;
        } else if (timeStr.equalsIgnoreCase("midnight")) {
            return (0 - 8 + 24) * 1000;
        }
        
        throw new CommandException("Time input format unknown.");
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
            String name = useDisplayNames
                    ? ((Player) sender).getDisplayName()
                    : ((Player) sender).getName();
            return ChatColor.stripColor(name);
        } else {
            return "*Console*";
        }
    }
    
    /**
     * Gets the name of a command sender. This play be a display name.
     * 
     * @param sender
     * @param endColor 
     * @return
     */
    public String toColoredName(CommandSender sender, ChatColor endColor) {
        if (sender instanceof Player) {
            String name = useDisplayNames
                    ? ((Player) sender).getDisplayName()
                    : ((Player) sender).getName();
            if (endColor != null && name.contains("\u00A7")) {
                name = name + endColor;
            }
            return name;
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
        
        return new ItemStack(id, 1, (short)dmg, (byte)dmg);
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
     * Attempt to match a dye color for sheep wool.
     *
     * @param filter
     * @return
     * @throws CommandException
     */
    public DyeColor matchDyeColor(String filter) throws CommandException {
        if (filter.equalsIgnoreCase("random")) {
            return DyeColor.getByData((byte) new Random().nextInt(15));
        }
        try {
            DyeColor match = DyeColor.valueOf(filter.toUpperCase());
            if (match != null) {
                return match;
            }
        } catch (IllegalArgumentException e) {}
        throw new CommandException("Unknown dye color name of '" + filter + "'.");
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
     * Get the ban database.
     * 
     * @return
     */
    public BanDatabase getBanDatabase() {
        return bans;
    }
    
    /**
     * Get the root warps manager.
     * 
     * @return
     */
    public RootLocationManager<NamedLocation> getWarpsManager() {
        return warps;
    }
    
    /**
     * Get the root homes manager.
     * 
     * @return
     */
    public RootLocationManager<NamedLocation> getHomesManager() {
        return homes;
    }
    
    /**
     * Return the kit manager.
     * 
     * @return
     */
    public KitManager getKitManager() {
        return kits;
    }
    
    /**
     * Get the ban message.
     * 
     * @return
     */
    public String getBanMessage() {
        return banMessage;
    }
    
    /**
     * Get the time lock manager.
     * 
     * @return
     */
    public TimeLockManager getTimeLockManager() {
        return timeLockManager;
    }
    
    /**
     * Get locked times.
     * 
     * @return
     */
    public Map<String, Integer> getLockedTimes() {
        return lockedTimes;
    }
    
    /**
     * Get the jingle note manager.
     * 
     * @return
     */
    public JingleNoteManager getJingleNoteManager() {
        return jingleNoteManager;
    }
    
    /**
     * Replace macros in the text.
     * 
     * @param sender 
     * @param message
     * @return
     */
    public String replaceMacros(CommandSender sender, String message) {
        Player[] online = getServer().getOnlinePlayers();
        
        message = message.replace("%name%", toName(sender));
        message = message.replace("%cname%", toColoredName(sender, null));
        message = message.replace("%id%", toUniqueName(sender));
        message = message.replace("%online%", String.valueOf(online.length));
        
        // Don't want to build the list unless we need to
        if (message.contains("%players%")) {
            message = message.replace("%players%",
                    CommandBookUtil.getOnlineList(online));
        }
        
        if (sender instanceof Player) {
            Player player = (Player) sender;
            World world = player.getWorld();

            message = message.replace("%time%", getTimeString(world.getTime()));
            message = message.replace("%world%", world.getName());
        }
        
        Pattern cmdPattern = Pattern.compile("%cmd:([^%]+)%");
        Matcher matcher = cmdPattern.matcher(message);
        try {
            StringBuffer buff = new StringBuffer();
            while (matcher.find()) {
                Process p = new ProcessBuilder(matcher.group(1).split(" ")).start();
                BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String s;
                StringBuilder build = new StringBuilder();
                while ((s = stdInput.readLine()) != null) {
                    build.append(s + " ");
                }
                stdInput.close();
                build.delete(build.length() - 1, build.length());
                matcher.appendReplacement(buff, build.toString());
                p.destroy();
            }
            matcher.appendTail(buff);
            message = buff.toString();
        } catch (IOException e) {
            sender.sendMessage(ChatColor.RED + "Error replacing macros: " + e.getMessage());
        }
        return message;
    }
    
    /**
     * Get a session.
     * 
     * @param user 
     * @return
     */
    public UserSession getSession(CommandSender user) {
        synchronized (sessions) {
            String key;
            
            if (user instanceof Player) {
                key = ((Player) user).getName();
            } else {
                key = UserSession.CONSOLE_NAME;
            }
            
            UserSession session = sessions.get(key);
            if (session != null) {
                return session;
            }
            session = new UserSession();
            sessions.put(key, session);
            return session;
        }
    }
    
    /**
     * Get sessions.
     * 
     * @return
     */
    public Map<String, UserSession> getSessions() {
        return sessions;
    }
    
    /**
     * Get a session.
     * 
     * @param user 
     * @return
     */
    public AdministrativeSession getAdminSession(Player user) {
        synchronized (adminSessions) {
            String key = user.getName();
            
            AdministrativeSession session = adminSessions.get(key);
            if (session != null) {
                return session;
            }
            session = new AdministrativeSession();
            adminSessions.put(key, session);
            return session;
        }
    }
    
    /**
     * Get sessions.
     * 
     * @return
     */
    public Map<String, AdministrativeSession> getAdminSessions() {
        return adminSessions;
    }

    /**
     * Get the permissions resolver.
     * 
     * @return
     */
    public PermissionsResolverManager getPermissionsResolver() {
        return perms;
    }
}
