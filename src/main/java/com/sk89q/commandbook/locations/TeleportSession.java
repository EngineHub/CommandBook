/*
 * CommandBook
 * Copyright (C) 2012 sk89q <http://www.sk89q.com>
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

package com.sk89q.commandbook.locations;

import com.sk89q.commandbook.session.PersistentSession;
import com.sk89q.minecraft.util.commands.CommandException;
import com.zachsthings.libcomponents.config.Setting;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author zml2008
 */
public class TeleportSession extends PersistentSession {
    private static final long MAX_AGE = TimeUnit.DAYS.toMillis(10);
    private static final long BRINGABLE_TIME = TimeUnit.MINUTES.toMillis(5);
    private static final long TP_REQUEST_WAIT_TIME = TimeUnit.SECONDS.toMillis(30);
    private static final long RECONNECT_GRACE = TimeUnit.MINUTES.toMillis(1);
    private static final int LOCATION_HISTORY_SIZE = 10;
    private final TeleportComponent component;

    @Setting("teleport.bringable") private Map<String, Long> bringable = new HashMap<String, Long>();
    @Setting("teleport.requests") private final Map<String, Long> teleportRequests = new HashMap<String, Long>();
    private final LinkedList<Location> locationHistory = new LinkedList<Location>();
    private Location ignoreTeleportLocation;

    public TeleportSession(TeleportComponent component) {
        super(MAX_AGE);
        this.component = component;
    }

    @Override
    public void handleReconnect(CommandSender player) {
        if (getGoneTime() >= RECONNECT_GRACE) {
            bringable = new HashMap<String, Long>();
        }
    }

    public void addBringable(Player player) {
        bringable.put(player.getName(), System.currentTimeMillis());
    }

    public void removeBringable(Player player) {
        bringable.put(player.getName(), System.currentTimeMillis());
    }

    public boolean isBringable(Player player) {
        long now = System.currentTimeMillis();
        Long time = bringable.remove(player.getName());
        return (time != null && (now - time) < BRINGABLE_TIME);
    }

    public void checkLastTeleportRequest(Player target) throws CommandException {
        long now = System.currentTimeMillis();
        Long time = teleportRequests.remove(target.getName());
        if (time != null && (now - time) < TP_REQUEST_WAIT_TIME) {
            throw new CommandException(component.getConfig().callMessageTooSoon);
        }
        teleportRequests.put(target.getName(), now);
    }

    public void rememberLocation(Location location) {
        if (locationHistory.size() > 0 && locationHistory.peek().equals(location)) {
            return;
        }

        locationHistory.add(0, location);
        while (locationHistory.size() > LOCATION_HISTORY_SIZE) {
            locationHistory.pollLast();
        }
    }

    public void rememberLocation(Player player) {
        rememberLocation(player.getLocation());
    }

    public Location popLastLocation() {
        return locationHistory.poll();
    }

    public void setIgnoreLocation(Location loc) {
        this.ignoreTeleportLocation = loc;
    }

    public Location getIgnoreLocation() {
        return ignoreTeleportLocation;
    }
}
