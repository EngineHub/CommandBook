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
import com.sk89q.commandbook.components.AbstractComponent;
import com.sk89q.commandbook.components.ComponentInformation;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@ComponentInformation(friendlyName = "Sessions", desc = "Handles player sessions")
public class SessionComponent extends AbstractComponent implements Runnable, Listener {

    public static final long CHECK_FREQUENCY = 1200;

    protected final Map<String, UserSession> sessions =
            new HashMap<String, UserSession>();
    protected final Map<String, AdministrativeSession> adminSessions =
            new HashMap<String, AdministrativeSession>();

    @Override
    public void initialize() {
        CommandBook.server().getScheduler().scheduleSyncRepeatingTask(CommandBook.inst(), this, CHECK_FREQUENCY, CHECK_FREQUENCY);
        CommandBook.registerEvents(this);
        registerCommands(Commands.class);
    }

    // -- Getting sessions

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


    // -- Garbage collection

    public void run() {
        cleanUpSessions(getSessions());
        cleanUpSessions(getAdminSessions());
    }

    public <T extends PersistentSession> void cleanUpSessions(Map<String, T> map) {
        synchronized (map) {
            Iterator<Map.Entry<String, T>> it = map.entrySet().iterator();

            while (it.hasNext()) {
                Map.Entry<String, T> entry = it.next();
                if (entry.getKey().equals(UserSession.CONSOLE_NAME)) continue;
                Player player = CommandBook.server().getPlayerExact(entry.getKey());
                if (player != null && player.isOnline()) continue;

                if (!entry.getValue().isRecent()) {
                    it.remove();
                }
            }
        }
    }

    // -- Events

    @EventHandler(event = PlayerLoginEvent.class)
    public void onLoin(PlayerLoginEvent event) {
        // Trigger the session
        getSession(event.getPlayer()).handleReconnect();
    }

    /**
     * Called on player disconnect.
     */
    @EventHandler(event = PlayerQuitEvent.class)
    public void onPlayerQuit(PlayerQuitEvent event) {
        getSession(event.getPlayer()).handleDisconnect();
        getAdminSession(event.getPlayer()).handleDisconnect();
    }
    
    public class Commands {
        @Command(aliases = {"confirm", "conf"},
                desc = "Confirm an action",
                max = 0)
        @CommandPermissions({"commandbook.confirm"})
        public void confirm(CommandContext args, CommandSender sender) throws CommandException {
            UserSession session = getSession(sender);
            String cmd = session.getCommandToConfirm(false);
            if (cmd.indexOf(" ") < 0) {
                session.getCommandToConfirm(true);
                throw new CommandException("Invalid command set for " + sender.getName() + "'s session.");
            }
            CommandBook.server().dispatchCommand(sender, cmd);
            sender.sendMessage(ChatColor.YELLOW + "Action confirmed!");
        }
    }
}
