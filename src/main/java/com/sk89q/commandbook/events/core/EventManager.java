package com.sk89q.commandbook.events.core;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.CommandBookRuntimeException;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredListener;

import java.lang.reflect.Method;

/**
 * @author zml2008
 */
public class EventManager {

    public void registerEvents(Listener listener, Object owner) {
        for (final Method method : listener.getClass().getMethods()) {
            method.setAccessible(true);
            if (method.isAnnotationPresent(BukkitEvent.class)) {
                BukkitEvent event = method.getAnnotation(BukkitEvent.class);
                Bukkit.getServer().getPluginManager().registerEvent(event.type(),
                        listener, new CommandBookEventExecutor(method),
                        event.priority(), CommandBook.inst());
            } else if (method.isAnnotationPresent(EventListener.class)) {
                EventListener registration = method.getAnnotation(EventListener.class);
                getEventListeners(getRegistrationClass(registration.event())).
                        register(new CommandBookRegisteredListener(listener,
                                new CommandBookEventExecutor(method),
                                registration.priority(),
                                owner));
            }
        }
    }

    public <T extends Event> T callEvent(T event) {
        if (event instanceof CommandBookEvent) {
            RegisteredListener[][] handlers = ((CommandBookEvent) event).getHandlers().getRegisteredListeners();
            for (int i = 0; i < handlers.length; i++) {
                if (handlers[i] == null) continue;
                for (RegisteredListener listener : handlers[i]) {
                    listener.callEvent(event);
                }
            }
        } else {
            Bukkit.getServer().getPluginManager().callEvent(event);
        }
        return event;
    }

    private Class<? extends Event> getRegistrationClass(Class<? extends Event> clazz) {
        try {
            clazz.getDeclaredMethod("getHandlerList");
            return clazz;
        } catch (NoSuchMethodException e) {
            if (clazz.getSuperclass() != null
                    && !clazz.getSuperclass().equals(CommandBookEvent.class)
                    && clazz.getSuperclass().isAssignableFrom(CommandBookEvent.class)) {
                return getRegistrationClass(clazz.getSuperclass().asSubclass(Event.class));
            } else {
                throw new CommandBookRuntimeException("Unable to find handler list for event " + clazz.getName());
            }
        }
    }

    /**
     * Returns the specified event type's HandlerList
     *
     * @param type EventType to lookup
     * @return HandlerList The list of registered handlers for the event.
     */
    private HandlerList getEventListeners(Class<? extends Event> type) {
        try {
            Method method = getRegistrationClass(type).getDeclaredMethod("getHandlerList");
            method.setAccessible(true);
            return (HandlerList)method.invoke(null);
        } catch (Exception e) {
            throw new CommandBookRuntimeException(e.toString());
        }
    }
}
