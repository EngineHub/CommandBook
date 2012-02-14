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
import com.zachsthings.libcomponents.spout.SpoutComponent;
import com.zachsthings.libcomponents.ComponentInformation;
import org.spout.api.ChatColor;
import org.spout.api.command.CommandContext;
import org.spout.api.command.CommandSource;
import org.spout.api.command.annotated.Command;
import org.spout.api.event.EventHandler;
import org.spout.api.event.Listener;
import org.spout.api.event.player.PlayerLeaveEvent;
import org.spout.api.event.player.PlayerLoginEvent;
import org.spout.api.exception.CommandException;
import org.spout.api.player.Player;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@ComponentInformation(friendlyName = "Sessions", desc = "Handles player sessions")
public class SessionComponent extends SpoutComponent implements Runnable, Listener {

    public static final long CHECK_FREQUENCY = 1200;

    protected final Map<String, UserSession> sessions =
            new HashMap<String, UserSession>();
    protected final Map<String, AdministrativeSession> adminSessions =
            new HashMap<String, AdministrativeSession>();

    @Override
    public void enable() {
        CommandBook.game().getScheduler().scheduleSyncRepeatingTask(CommandBook.inst(), this, CHECK_FREQUENCY, CHECK_FREQUENCY);
        CommandBook.game().getEventManager().registerEvents(this, this);
        registerCommands(Commands.class);
    }

    // -- Getting sessions

    /**
     * Get a session.
     *
     * @param user
     * @return
     */
    public UserSession getSession(CommandSource user) {
        synchronized (sessions) {
            String key;

            if (user instanceof Player) {
                key = user.getName();
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


    // -- Garbage collection

    public void run() {
        cleanUpSessions(getSessions());
        cleanUpSessions(getAdminSessions());
    }

    public <T extends PersistentSession> void cleanUpSessions(final Map<String, T> map) {
        Iterator<Map.Entry<String, T>> it = map.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<String, T> entry = it.next();
            if (entry.getKey().equals(UserSession.CONSOLE_NAME)) continue;
            Player player = CommandBook.game().getPlayer(entry.getKey(), true);
            if (player != null && player.isOnline()) continue;

            if (!entry.getValue().isRecent()) {
                it.remove();
            }
        }
    }

    // -- Events

    @EventHandler
    public void onLoin(PlayerLoginEvent event) {
        // Trigger the session
        getSession(event.getPlayer()).handleReconnect();
    }

    /**
     * Called on player disconnect.
     */
    @EventHandler
    public void onPlayerQuit(PlayerLeaveEvent event) {
        getSession(event.getPlayer()).handleDisconnect();
        getAdminSession(event.getPlayer()).handleDisconnect();
    }
    
    public class Commands {
        @Command(aliases = {"confirm", "conf"},
                desc = "Confirm an action",
                max = 0, flags = "vc")
        public void confirm(CommandContext args, CommandSource sender) throws CommandException {
            UserSession session = getSession(sender);
            final String cmd = session.getCommandToConfirm(false);
            if (cmd == null) throw new CommandException("No command to confirm!");
            if (args.hasFlag('v')) {
                sender.sendMessage(ChatColor.YELLOW + "Current command to confirm: " + cmd);
            } else if (args.hasFlag('c')) {
                session.getCommandToConfirm(true);
                sender.sendMessage(ChatColor.YELLOW + "Cleared command to confirm");
            } else {
                sender.sendMessage(ChatColor.YELLOW + "Command confirmed: " + cmd);
                CommandBook.game().processCommand(sender, cmd);
                session.getCommandToConfirm(true);
            }
        }
    }
}
