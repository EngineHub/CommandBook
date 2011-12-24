package com.sk89q.commandbook.events.core;

import org.bukkit.event.Event;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author zml2008
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface BukkitEvent {
    public Event.Type type();

    public Event.Priority priority() default Event.Priority.Normal;
}
