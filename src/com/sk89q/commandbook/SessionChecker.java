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

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import org.bukkit.entity.Player;

public class SessionChecker implements Runnable {
    
    public static final long CHECK_FREQUENCY = 1200;
    
    private CommandBookPlugin plugin;
    
    public SessionChecker(CommandBookPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        Map<String, CommandBookSession> sessions = plugin.getSessions();
        synchronized (sessions) {
            Iterator<Entry<String, CommandBookSession>> it =
                    plugin.getSessions().entrySet().iterator();
            
            while (it.hasNext()) {
                Entry<String, CommandBookSession> entry = it.next();
                if (entry.getKey().equals(CommandBookSession.CONSOLE_NAME)) continue;
                Player player = plugin.getServer().getPlayer(entry.getKey());
                if (player != null && player.isOnline()) continue;
                
                if (!entry.getValue().isRecent()) {
                    it.remove();
                }
            }
        }
    }

}
