package com.sk89q.commandbook.events.core;

import org.bukkit.event.Event;

/**
 * @author zml2008
 */
public abstract class CommandBookEvent extends Event {

    protected CommandBookEvent() {
        super("NarwhalEvent");
    }

    public abstract HandlerList getHandlers();
}
