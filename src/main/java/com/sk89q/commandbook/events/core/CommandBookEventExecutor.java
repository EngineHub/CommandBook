package com.sk89q.commandbook.events.core;

import com.sk89q.commandbook.CommandBook;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author zml2008
 */
public class CommandBookEventExecutor implements EventExecutor {
    private final Method eventMethod;
    public CommandBookEventExecutor(Method method) {
        eventMethod = method;
    }
    public void execute(Listener listener, Event event) {
        try {
            eventMethod.invoke(listener, event);
        } catch (IllegalAccessException e) {
            CommandBook.logger().severe("Error calling event " + event + ": " + e);
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            CommandBook.logger().severe("Error calling event " + event + ": " + e);
            e.printStackTrace();
        }
    }
}
