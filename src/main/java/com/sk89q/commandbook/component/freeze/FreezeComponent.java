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

package com.sk89q.commandbook.component.freeze;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.component.session.PersistentSession;
import com.sk89q.commandbook.component.session.SessionComponent;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.concurrent.TimeUnit;

@Depend(components = SessionComponent.class)
@ComponentInformation(friendlyName = "Freeze", desc = "Blocks a specified player's movement on command")
public class FreezeComponent extends BukkitComponent implements Listener, Runnable {
    public static final int MOVE_THRESHOLD = 2;
    private static final int MOVE_THRESHOLD_SQ = MOVE_THRESHOLD * MOVE_THRESHOLD;

    @InjectComponent private SessionComponent sessions;

    @Override
    public void enable() {
        CommandBook.getComponentRegistrar().registerTopLevelCommands((registrar) -> {
            registrar.register(FreezeCommandsRegistration.builder(), new FreezeCommands(this));
        });

        CommandBook.registerEvents(this);
        CommandBook.server().getScheduler().scheduleSyncRepeatingTask(CommandBook.inst(), this, 20 * 2, 20 * 2);
    }

    public boolean freezePlayer(Player player) {
        FreezeState session = sessions.getSession(FreezeState.class, player);
        final boolean previous = session.isFrozen();
        session.freeze(player.getLocation());
        return previous;
    }

    public boolean isFrozen(Player player) {
        return sessions.getSession(FreezeState.class, player).isFrozen();
    }

    public boolean unfreezePlayer(Player player) {
        FreezeState session = sessions.getSession(FreezeState.class, player);
        final boolean previous = session.isFrozen();
        session.freeze(null);
        return previous;
    }

    @Override
    public void run() {
        for (FreezeState frozenState : sessions.getSessions(FreezeState.class).values()) {
            if (!frozenState.isFrozen()) {
                continue;
            }

            Player player = frozenState.getOwner();
            if (player == null || !player.isOnline()) {
                continue;
            }

            Location loc = player.getLocation();
            if (loc.distanceSquared(frozenState.getFreezeLocation()) >= MOVE_THRESHOLD_SQ) {
                loc.setX(frozenState.getFreezeLocation().getX());
                loc.setY(frozenState.getFreezeLocation().getY());
                loc.setZ(frozenState.getFreezeLocation().getZ());
                player.sendMessage(ChatColor.RED + "You are frozen.");
                player.teleport(loc);
            }

        }
    }

    private static class FreezeState extends PersistentSession {
        public static final long MAX_AGE = TimeUnit.MINUTES.toMillis(30);

        private Location freezeLocation;

        protected FreezeState() {
            super(MAX_AGE);
        }

        public boolean isFrozen() {
            return freezeLocation != null;
        }

        public Location getFreezeLocation() {
            return freezeLocation;
        }

        public void freeze(Location loc) {
            freezeLocation = loc == null ? null : loc.clone();
        }

        public Player getOwner() {
            CommandSender sender = super.getOwner();
            return sender instanceof Player ? (Player) sender : null;
        }
    }
}
