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

public class CommandBookSession {

    private String lastRecipient = null;
    private long lastRecipientTime = 0;
    private boolean hasThor = false;
    
    public String getLastRecipient() {
        return lastRecipient;
    }
    
    public void setLastRecipient(String target) {
        lastRecipient = target;
    }
    
    public void setNewLastRecipient(String target) {
        long now = System.currentTimeMillis();
        
        if (lastRecipient == null || (now - lastRecipientTime) > 1000) {
            lastRecipient = target;
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
