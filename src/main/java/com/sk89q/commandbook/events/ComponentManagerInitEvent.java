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

package com.sk89q.commandbook.events;

import com.sk89q.commandbook.components.ComponentManager;
import com.sk89q.commandbook.events.core.CommandBookEvent;
import com.sk89q.commandbook.events.core.HandlerList;

/**
 * This event is called after CommandBook has registered its component loaders
 * and annotation handlers. Other plugins can use this event to add their own
 * custom component loaders and annotation handlers.
 */
public class ComponentManagerInitEvent extends CommandBookEvent {
    private static final long serialVersionUID = -7563977474777221423L;

    private final ComponentManager componentManager;

    public ComponentManagerInitEvent(ComponentManager componentManager) {
        this.componentManager = componentManager;
    }

    public ComponentManager getComponentManager() {
        return componentManager;
    }

    private static final HandlerList handlers = new HandlerList();

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
