package com.sk89q.commandbook.events.core;

import org.bukkit.event.Event;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author zml2008
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface EventListener {
    public Class<? extends CommandBookEvent> event();

    public Event.Priority priority() default Event.Priority.Normal;
}
