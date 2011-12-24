package com.sk89q.commandbook.events.core;

import com.zachsthings.narwhal.Narwhal;
import com.zachsthings.narwhal.NarwhalRuntimeException;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredListener;

import java.lang.reflect.Method;

/**
 * @author zml2008
 */
public class EventManager {

    public void registerEvents(Listener listener) {
        for (final Method method : listener.getClass().getMethods()) {
            method.setAccessible(true);
            BukkitEvent event = method.getAnnotation(BukkitEvent.class);
            EventListener registration = method.getAnnotation(EventListener.class);
            if (event != null) {
                Bukkit.getServer().getPluginManager().registerEvent(event.type(),
                        listener, new CommandBookEventExecutor(method),
                        event.priority(), Narwhal.inst());
            } else if (registration != null) {
                getEventListeners(getRegistrationClass(registration.event())).
                        register(new RegisteredListener(listener,
                                new CommandBookEventExecutor(method),
                                registration.priority(),
                                Narwhal.inst()));
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
                throw new NarwhalRuntimeException("Unable to find handler list for event " + clazz.getName());
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
            throw new NarwhalRuntimeException(e.toString());
        }
    }
}
