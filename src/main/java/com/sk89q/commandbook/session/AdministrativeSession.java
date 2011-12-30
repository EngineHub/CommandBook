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

public class AdministrativeSession implements PersistentSession {
    
    private static final int MAX_AGE = 3600000;
    
    private long lastUpdate;
    private boolean isMute;
    private boolean isFrozen;
    
    public boolean isRecent() {
        return (System.currentTimeMillis() - lastUpdate) < MAX_AGE;
    }
    
    public void handleDisconnect() {
        lastUpdate = System.currentTimeMillis();
    }

    public boolean isMute() {
        return isMute;
    }

    public void setMute(boolean isMute) {
        this.isMute = isMute;
    }
    
    public boolean isFrozen() {
        return isFrozen;
    }
    
    public void setFrozen(boolean isFrozen) {
        this.isFrozen = isFrozen;
    }
}
