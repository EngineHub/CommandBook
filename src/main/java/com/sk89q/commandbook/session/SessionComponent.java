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

package com.sk89q.commandbook.session;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.util.entity.player.UUIDUtil;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.util.yaml.YAMLFormat;
import com.sk89q.util.yaml.YAMLNode;
import com.sk89q.util.yaml.YAMLProcessor;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.bukkit.YAMLNodeConfigurationNode;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

@ComponentInformation(friendlyName = "Sessions", desc = "Handles player sessions")
public class SessionComponent extends BukkitComponent implements Runnable, Listener {
    public static final long CHECK_FREQUENCY = 60 * 20;

    private final Map<Class<? extends CommandSender>, Map<String, Map<Class<? extends PersistentSession>, PersistentSession>>>
            sessions = new ConcurrentHashMap<Class<? extends CommandSender>, Map<String, Map<Class<? extends PersistentSession>, PersistentSession>>>();
    private final Map<Class<? extends PersistentSession>, SessionFactory<?>>
            sessionFactories = new ConcurrentHashMap<Class<? extends PersistentSession>, SessionFactory<?>>();
    private File sessionsDir;
    private final Map<String, Map<String, YAMLProcessor>> sessionDataStores = new HashMap<String, Map<String, YAMLProcessor>>();

    @Override
    public void enable() {
        CommandBook.server().getScheduler().scheduleSyncRepeatingTask(CommandBook.inst(), this, CHECK_FREQUENCY, CHECK_FREQUENCY);
        CommandBook.registerEvents(this);
        registerCommands(Commands.class);
        sessionsDir = new File(CommandBook.inst().getDataFolder(), "sessions");
        if (!sessionsDir.exists()) {
            sessionsDir.mkdirs();
        }
    }

    @Override
    public void disable() {
        for (Player player : CommandBook.server().getOnlinePlayers()) {
            String type = getType(player.getClass());
            for (PersistentSession session : getSessions(player)) {
                session.handleDisconnect();
                session.save(new YAMLNodeConfigurationNode(getSessionConfiguration(type, UUIDUtil.toUniqueString(player), session.getClass())));
            }
            YAMLProcessor proc = getUserConfiguration(type, UUIDUtil.toUniqueString(player), false);
            if (proc != null) {
                proc.save();
            }
        }
    }

    // -- Getting sessions

    /**
     * Get a session.
     *
     * @param user The user to get a session for
     * @return The user's session
     * @deprecated see {@link #getSession(Class, org.bukkit.command.CommandSender)} with args (UserSession.class, user)
     */
    @Deprecated
    public UserSession getSession(CommandSender user) {
        return getSession(UserSession.class, user);
    }

    /**
     * Get sessions.
     *
     * @return UserSessions
     * @deprecated use {@link #getSessions(Class)} with UserSession.class
     */
    @Deprecated
    public Map<String, UserSession> getSessions() {
        return getSessions(UserSession.class);
    }

    /**
     * Get a session.
     *
     * @param user The player to get this session for
     * @return The user's session
     * @deprecated see {@link #getSession(Class, org.bukkit.command.CommandSender)} with args (AdministrativeSession.class, user)
     */
    @Deprecated
    public AdministrativeSession getAdminSession(Player user) {
        return getSession(AdministrativeSession.class, user);
    }

    /**
     * Get sessions.
     *
     * @return Administrative sessions which currently exist
     * @deprecated use {@link #getSessions(Class)} with UserSession.class
     */
    @Deprecated
    public Map<String, AdministrativeSession> getAdminSessions() {
        return getSessions(AdministrativeSession.class);
    }

    /**
     * Return all the currently registered sessions of the given type
     * @param type The type of session to get
     * @param <T> The type parameter for the session
     * @return The currently registered session types
     */
    public <T extends PersistentSession> Map<String, T> getSessions(Class<T> type) {
        Map<String, T> ret = new HashMap<String, T>();
        synchronized (sessions) {
            for (Map<String, Map<Class<? extends PersistentSession>, PersistentSession>> parent : sessions.values()) {
                for (Map.Entry<String, Map<Class<? extends PersistentSession>, PersistentSession>> entry : parent.entrySet()) {
                    PersistentSession session = entry.getValue().get(type);
                    if (session != null) {
                        ret.put(entry.getKey(), type.cast(session));
                    }
                }
            }
        }
        return ret;
    }

    /**
     * Return the sessions which currently exist for the specified user
     * @param user The user to get a session for
     * @return The sessions which currently exist for this user
     */
    public Collection<PersistentSession> getSessions(CommandSender user) {
        Map<Class<? extends PersistentSession>, PersistentSession> ret = getSessionM(user.getClass()).get(UUIDUtil.toUniqueString(user));
        if (ret == null) {
            ret = Collections.emptyMap();
        }
        return Collections.unmodifiableCollection(ret.values());
    }

    /**
     * Gets the session of type for user, creating a new instance if none currently exists
     *
     * @see #getSessionFactory(Class)
     * @param type The type of session to get
     * @param user The user to get the session for
     * @param <T> The type of session
     * @return The player's session, or null if the session could not be correctly created
     */
    public <T extends PersistentSession> T getSession(Class<T> type, CommandSender user) {
        synchronized (sessions) {
            Map<String, Map<Class<? extends PersistentSession>, PersistentSession>> typeMap = getSessionM(user.getClass());
            Map<Class<? extends PersistentSession>, PersistentSession> userSessions = typeMap.get(UUIDUtil.toUniqueString(user));
            if (userSessions == null) {
                userSessions = new HashMap<Class<? extends PersistentSession>, PersistentSession>();
                typeMap.put(UUIDUtil.toUniqueString(user), userSessions);
            }

            // Do we have an existing session?
            T session = type.cast(userSessions.get(type));
            if (session == null) {
                session = getSessionFactory(type).createSession(user);
                if (session != null) {
                    YAMLNode node = getSessionConfiguration(getType(user.getClass()), UUIDUtil.toUniqueString(user), type, false);
                    if (node != null) {
                       session.load(new YAMLNodeConfigurationNode(node));
                    }
                    session.handleReconnect(user);
                    userSessions.put(type, session);
                }
            }
            return session;
        }
    }

    /**
     * Return a SessionFactory used to create new instances of a certain type of session.
     * If no SessionFactory has been previously registered with {@link #registerSessionFactory(Class, SessionFactory)},
     * a new {@link ReflectiveSessionFactory} will be instantiated. This will only create
     * sessions for PersistentSession subclasses with an empty constructor.
     * @param type The subclass of PersistentSession to get a SessionFactory for
     * @param <T> The type of PersistentSession
     * @return The required SessionFactory
     */
    @SuppressWarnings("unchecked")
    public <T extends PersistentSession> SessionFactory<T> getSessionFactory(Class<T> type) {
        synchronized (sessionFactories) {
            SessionFactory<?> factory = sessionFactories.get(type);
            if (factory == null) {
                factory = new ReflectiveSessionFactory(type);
                sessionFactories.put(type, factory);
            }
            return (SessionFactory<T>) factory;
        }
    }

    public <T extends PersistentSession> void registerSessionFactory(Class<T> type, SessionFactory<T> factory) {
        sessionFactories.put(type, factory);
    }

    /**
     * Add {@code session} to the sessions list for {@code user}, overwriting any sessions with the same class.
     * @param session The session to add
     * @param user The user to add the session to
     */
    public void addSession(PersistentSession session, CommandSender user) {
        Map<String, Map<Class<? extends PersistentSession>, PersistentSession>> typeMap = getSessionM(user.getClass());
        Map<Class<? extends PersistentSession>, PersistentSession> userSessions = typeMap.get(UUIDUtil.toUniqueString(user));
        if (userSessions == null) {
            userSessions = new HashMap<Class<? extends PersistentSession>, PersistentSession>();
            typeMap.put(UUIDUtil.toUniqueString(user), userSessions);
        }
        userSessions.put(session.getClass(), session);

    }

    // Persistence-related methods
    private YAMLProcessor getUserConfiguration(String type, String commander, boolean create) {
        Map<String, YAMLProcessor> typeMap = getDataStore(type);
        YAMLProcessor processor = typeMap.get(commander);
        if (processor == null) {
            File userFile = new File(sessionsDir.getPath() + File.separator + type + File.separator + commander + ".yml");
            if (!userFile.exists()) {
                File dir = userFile.getParentFile();
                if (!dir.exists()) {
                    dir.mkdirs();
                }

                if (!migrate(commander, userFile)) {
                    if (!create) {
                        return null;
                    }

                    try {
                        userFile.createNewFile();
                    } catch (IOException e) {
                        CommandBook.logger().log(Level.WARNING, "Could not create sessions persistence file for user " + commander, e);
                    }
                }
            }
            processor = new YAMLProcessor(userFile, false, YAMLFormat.COMPACT);
            try {
                processor.load();
            } catch (IOException e) {
                CommandBook.logger().log(Level.WARNING, "Error loading sessions persistence file for user " + commander, e);
            }
            typeMap.put(commander, processor);
        }
        return processor;
    }

    private YAMLNode getSessionConfiguration(String type, String commander, Class<? extends PersistentSession> sessType) {
        return getSessionConfiguration(type, commander, sessType, true);
    }

    private YAMLNode getSessionConfiguration(String type, String commander, Class<? extends PersistentSession> sessType, boolean create) {
        YAMLProcessor proc = getUserConfiguration(type, commander, create);
        if (proc == null) {
            return null;
        }

        String className = sessType.getCanonicalName().replaceAll("\\.", "/");
        YAMLNode sessionNode = proc.getNode(className);
        if (sessionNode == null && create) {
            sessionNode = proc.addNode(className);
        }
        return sessionNode;
    }

    // - Migration Functions
    private boolean migrate(String commander, File dest) {
        boolean result = false;
        try {
            // Try to parse the commander as a player's UUID
            OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(commander));
            if (player != null) {
                // A player was found, see if they have an old file based on their name to migrate
                File oldUserFile = new File(sessionsDir.getPath() + File.separator + player.getName() + ".yml");
                if (oldUserFile.exists()) {
                    // Move the file, and print an error if the move operation failed
                    result = oldUserFile.renameTo(dest);
                    if (!result) {
                        CommandBook.logger().warning("Could not update a player's session file to use UUID: " + commander);
                    }
                }
            }
        } catch (IllegalArgumentException ignored) { } // Wasn't a player
        return result;
    }

    // - Utility Functions
    private String getType(Class<? extends CommandSender> clazz) {
        String[] split = clazz.getName().split("\\.");
        return split[split.length - 1];
    }

    private Map<String, Map<Class<? extends PersistentSession>, PersistentSession>> getSessionM(Class<? extends CommandSender> clazz) {
        Map<String, Map<Class<? extends PersistentSession>, PersistentSession>> typeMapping = sessions.get(clazz);
        if (typeMapping == null) {
            typeMapping = new HashMap<String, Map<Class<? extends PersistentSession>, PersistentSession>>();
            sessions.put(clazz, typeMapping);
        }
        return typeMapping;
    }

    private Map<String, YAMLProcessor> getDataStore(String type) {
        Map<String, YAMLProcessor> typeMap = sessionDataStores.get(type);
        if (typeMap == null) {
            typeMap = new HashMap<String, YAMLProcessor>();
            sessionDataStores.put(type, typeMap);
        }
        return typeMap;
    }


    // -- Garbage collection
    public void run() {
        synchronized (sessions) {
            for (Map.Entry<Class<? extends CommandSender>, Map<String, Map<Class<? extends PersistentSession>, PersistentSession>>> parent : sessions.entrySet()) {
                outer:
                for (Iterator<Map.Entry<String, Map<Class<? extends PersistentSession>, PersistentSession>>>
                             i = parent.getValue().entrySet().iterator(); i.hasNext(); ) {
                    Map.Entry<String, Map<Class<? extends PersistentSession>, PersistentSession>> entry = i.next();

                    for (Iterator<PersistentSession> i2 = entry.getValue().values().iterator(); i2.hasNext(); ) {
                        PersistentSession sess = i2.next();
                        if (sess.getOwner() != null) {
                            continue outer;
                        }

                        if (!sess.isRecent()) {
                            i2.remove();
                            String sender = sess.getUniqueName();
                            if (sender != null) {
                                YAMLProcessor processor = getUserConfiguration(getType(parent.getKey()), sender, false);
                                if (processor != null) {
                                    processor.removeProperty(sess.getClass().getCanonicalName().replaceAll("\\.", "/"));
                                }
                            }
                        }
                    }

                    if (entry.getValue().size() == 0) {
                        i.remove();
                    }
                }
            }
        }
    }

    // -- Events
    @EventHandler
    public void onLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        String type = getType(player.getClass());
        // Trigger the session
        for (PersistentSession session : getSessions(player)) {
            session.load(new YAMLNodeConfigurationNode(getSessionConfiguration(type, UUIDUtil.toUniqueString(player), session.getClass())));
            session.handleReconnect(event.getPlayer());
        }
    }

    /**
     * Called on player disconnect.
     *
     * @param event Relevant event details
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String type = getType(player.getClass());
        for (PersistentSession session : getSessions(event.getPlayer())) {
            session.handleDisconnect();
            session.save(new YAMLNodeConfigurationNode(getSessionConfiguration(type, UUIDUtil.toUniqueString(player), session.getClass())));
        }
        YAMLProcessor proc = getUserConfiguration(type, UUIDUtil.toUniqueString(player), false);
        if (proc != null) {
            proc.save();
        }
    }

    public class Commands {
        @Command(aliases = {"confirm", "conf"},
                desc = "Confirm an action",
                max = 0, flags = "vc")
        public void confirm(CommandContext args, CommandSender sender) throws CommandException {
            UserSession session = getSession(UserSession.class, sender);
            final String cmd = session.getCommandToConfirm(false);
            if (cmd == null) throw new CommandException("No command to confirm!");
            if (args.hasFlag('v')) {
                sender.sendMessage(ChatColor.YELLOW + "Current command to confirm: " + cmd);
            } else if (args.hasFlag('c')) {
                session.getCommandToConfirm(true);
                sender.sendMessage(ChatColor.YELLOW + "Cleared command to confirm");
            } else {
                sender.sendMessage(ChatColor.YELLOW + "Command confirmed: " + cmd);
                CommandBook.server().dispatchCommand(sender, cmd);
                session.getCommandToConfirm(true);
            }
        }
    }
}
