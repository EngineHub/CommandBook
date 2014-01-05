// $Id$
/*
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

package com.sk89q.commandbook.session;

import com.zachsthings.libcomponents.config.Setting;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.concurrent.TimeUnit;

public class UserSession extends PersistentSession {

    public static final String CONSOLE_NAME = "#console";
    private static final long MAX_AGE = TimeUnit.DAYS.toMillis(10);
    private static final long RECONNECT_GRACE = TimeUnit.MINUTES.toMillis(1);

    @Setting("messaging.last-recipient") private String lastRecipient = null;
    @Setting("messaging.last-recipient-time") private long lastRecipientTime = 0;
    private boolean hasThor = false;
    @Setting("confirm-command") private String commandToConfirm;


    protected UserSession() {
        super(MAX_AGE);
    }

    public void handleReconnect(CommandSender player) {
        super.handleReconnect(player);
        if (getGoneTime() >= RECONNECT_GRACE) {
            lastRecipient = null;
        }
    }

    public void handleDisconnect() {
        super.handleDisconnect();
        hasThor = false;
    }

    public String getLastRecipient() {
        return lastRecipient;
    }

    public void setLastRecipient(CommandSender target) {
        if (target instanceof Player) {
            lastRecipient = target.getName();
        } else {
            lastRecipient = CONSOLE_NAME;
        }
    }

    public void setNewLastRecipient(CommandSender target) {
        long now = System.currentTimeMillis();

        if (lastRecipient == null || (now - lastRecipientTime) > 1000) {
            setLastRecipient(target);
            lastRecipientTime = now;
        }
    }

    public boolean hasThor() {
        return hasThor;
    }

    public void setHasThor(boolean hasThor) {
        this.hasThor = hasThor;
    }

    public boolean checkOrQueueConfirmed(String command) {
        if (commandToConfirm != null) {
            return true;
        } else {
            commandToConfirm = command;
            return false;
        }
    }

    public String getCommandToConfirm(boolean clear) {
        if (clear) {
            final String ret = commandToConfirm;
            commandToConfirm = null;
            return ret;
        }
        return commandToConfirm;
    }
}
