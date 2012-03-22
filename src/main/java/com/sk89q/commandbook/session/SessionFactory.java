package com.sk89q.commandbook.session;

import org.bukkit.command.CommandSender;

/**
 * @author zml2008
 */
public interface SessionFactory<T extends PersistentSession> {
    public T createSession(CommandSender user);
}
