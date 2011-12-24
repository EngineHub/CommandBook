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

package com.sk89q.commandbook.events.core;

import com.sk89q.commandbook.CommandBook;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.RegisteredListener;

/**
 * @author zml2008
 */
public class CommandBookRegisteredListener extends RegisteredListener {
    private final Object owner;
    
    public CommandBookRegisteredListener(Listener pluginListener,
                                         EventExecutor eventExecutor,
                                         Event.Priority eventPriority,
                                         Object owner) {
        super(pluginListener, eventExecutor, eventPriority, CommandBook.inst());
        this.owner = owner;
    }
    
    public Object getOwner() {
        return owner;
    }
}
