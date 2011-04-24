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

package com.sk89q.commandbook;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandBookSession {

    public static final String CONSOLE_NAME = "#console";
    private static final int MAX_AGE = 3600000;
    private static final int RECONNECT_GRACE = 60000;

    private long lastUpdate = 0;
    private String lastRecipient = null;
    private long lastRecipientTime = 0;
    private boolean hasThor = false;
    
    public boolean isRecent() {
        return (System.currentTimeMillis() - lastUpdate) < MAX_AGE;
    }
    
    public void handleReconnect() {
        if ((System.currentTimeMillis() - lastUpdate) >= RECONNECT_GRACE) {
            lastRecipient = null;
        }
    }
    
    public void handleDisconnect() {
        lastUpdate = System.currentTimeMillis();
        
        hasThor = false;
    }
    
    public String getLastRecipient() {
        return lastRecipient;
    }

    public void setLastRecipient(CommandSender target) {
        if (target instanceof Player) {
            lastRecipient = ((Player) target).getName();
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

}
