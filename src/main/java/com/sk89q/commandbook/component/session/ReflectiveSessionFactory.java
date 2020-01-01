package com.sk89q.commandbook.component.session;

import org.bukkit.command.CommandSender;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * @author zml2008
 */
public class ReflectiveSessionFactory implements SessionFactory<PersistentSession> {
    private Constructor<? extends PersistentSession> constructor;

    public ReflectiveSessionFactory(Class<? extends PersistentSession> type) {
        try {
            constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
        } catch (NoSuchMethodException e) {
            constructor = null;
        }

    }
    @Override
    public PersistentSession createSession(CommandSender user) {
        if (constructor == null) {
            return null;
        } else {
            try {
                return constructor.newInstance();
            } catch (InstantiationException e) {
                e.printStackTrace();
                return null;
            } catch (IllegalAccessException e) {
                return null;
            } catch (InvocationTargetException e) {
                e.printStackTrace();
                return null;
            }
        }
    }
}
