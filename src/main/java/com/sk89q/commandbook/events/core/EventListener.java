package com.sk89q.commandbook.events.core;

import org.bukkit.event.Event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Methods that handle CommandBook events are registered with this annotation.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EventListener {
    public Class<? extends CommandBookEvent> event();

    public Event.Priority priority() default Event.Priority.Normal;
}
