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

import org.bukkit.event.Event.Priority;
import org.bukkit.plugin.RegisteredListener;

import java.util.*;
import java.util.Map.Entry;

/**
 * A list of event handlers, stored per-event. Based on lahwran's fevents.
 */
@SuppressWarnings("unchecked")
public class HandlerList {

    public static Map<Priority, Integer> eventSlots = new HashMap<Priority, Integer>();
    static {
        for (int i = 0; i < Priority.values().length; i++) {
            eventSlots.put(Priority.values()[i], i);
        }
    }
    /**
     * Handler array. This field being an array is the key to this system's speed.
     *
     */
    private RegisteredListener[][] handlers = new RegisteredListener[Priority.values().length][];

    /**
     * Dynamic handler lists. These are changed using register() and
     * unregister() and are automatically baked to the handlers array any
     * time they have changed.
     */
    private final EnumMap<Priority, ArrayList<CommandBookRegisteredListener>> handlerSlots;

    /**
     * Whether the current HandlerList has been fully baked. When this is set
     * to false, the Map<EventPriority, List<RegisteredListener>> will be baked to RegisteredListener[][]
     * next time the event is called.
     *
     * @see EventManager.callEvent(org.bukkit.event.Event)
     */
    private boolean baked = false;

    /**
     * List of all HandlerLists which have been created, for use in bakeAll()
     */
    private static ArrayList<HandlerList> allLists = new ArrayList<HandlerList>();

    /**
     * Bake all handler lists. Best used just after all normal event
     * registration is complete, ie just after all plugins are loaded if
     * you're using fevents in a plugin system.
     */
    public static void bakeAll() {
        for (HandlerList h : allLists) {
            h.bake();
        }
    }

    public static void unregisterAll() {
        for (HandlerList h : allLists) {
            h.handlerSlots.clear();
            h.baked = false;
        }
    }
    
    public static void unregisterAll(Object owner) {
        for (HandlerList h : allLists) {
            h.unregister(owner);
        }
    }

    /**
     * Create a new handler list and initialize using EventPriority
     * The HandlerList is then added to meta-list for use in bakeAll()
     */
    public HandlerList() {
        handlerSlots = new EnumMap<Priority, ArrayList<CommandBookRegisteredListener>>(Priority.class);
        for (Priority o : Priority.values()) {
            handlerSlots.put(o, new ArrayList<CommandBookRegisteredListener>());
        }
        allLists.add(this);
    }

    /**
     * Register a new listener in this handler list
     * @param listener listener to register
     */
    void register(CommandBookRegisteredListener listener) {
        if (handlerSlots.get(listener.getPriority()).contains(listener))
            throw new IllegalStateException("This listener is already registered to priority " + listener.getPriority().toString());
        baked = false;
        handlerSlots.get(listener.getPriority()).add(listener);
    }

    void registerAll(Collection<CommandBookRegisteredListener> listeners) {
        for (CommandBookRegisteredListener listener : listeners) {
            register(listener);
        }
    }

    /**
     * Remove a listener from a specific order slot
     * @param listener listener to remove
     */
    void unregister(RegisteredListener listener) {
        if (handlerSlots.get(listener.getPriority()).contains(listener)) {
            baked = false;
            handlerSlots.get(listener.getPriority()).remove(listener);
        }
    }
    
    void unregister(Object owner) {
        boolean changed = false;
        for (Entry<Priority, ArrayList<CommandBookRegisteredListener>> entry : handlerSlots.entrySet()) {
            for (Iterator<CommandBookRegisteredListener> i = entry.getValue().iterator(); i.hasNext();) {
                if (i.next().getOwner().equals(owner)) {
                    i.remove();
                    changed = true;
                }
            }
        }
        if (changed) baked = false;
    }

    /**
     * Bake HashMap and ArrayLists to 2d array - does nothing if not necessary
     */
    void bake() {
        if (baked) return; // don't re-bake when still valid
        for (Entry<Priority, ArrayList<CommandBookRegisteredListener>> entry : handlerSlots.entrySet()) {
            handlers[eventSlots.get(entry.getKey())] = (entry.getValue().toArray(new RegisteredListener[entry.getValue().size()]));
        }
        baked = true;
    }

    RegisteredListener[][] getRegisteredListeners() {
        return handlers;
    }
}