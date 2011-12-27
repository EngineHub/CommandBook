package com.sk89q.commandbook.events.core;

import org.bukkit.event.Event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Because Bukkit is stupid, events that are from Bukkit need to be registered with
 * Event.Type. Therefore, this second annotation.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface BukkitEvent {
    public Event.Type type();

    public Event.Priority priority() default Event.Priority.Normal;
}
