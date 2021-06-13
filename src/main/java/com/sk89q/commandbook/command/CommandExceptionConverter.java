/*
 * CommandBook
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) CommandBook team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.commandbook.command;

import com.google.common.collect.ImmutableList;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import org.enginehub.piston.Command;
import org.enginehub.piston.exception.CommandException;

import java.util.List;
import java.util.function.Function;

public class CommandExceptionConverter {
    private static record Conversion(Class<? extends Throwable> target,
                                     Function<Throwable, TextComponent> applyFn) {
        public boolean matches(Throwable t) {
            return target.isInstance(t);
        }

        public CommandException convert(Throwable t, ImmutableList<Command> commands) {
            return new CommandException(applyFn.apply(t), commands);
        }
    };

    private static final List<Conversion> CONVERSIONS = List.of(
        new Conversion(
            com.sk89q.minecraft.util.commands.CommandException.class,
            (ex) -> TextComponent.of(ex.getMessage())
        ),
        new Conversion(IllegalArgumentException.class, (ex) -> TextComponent.of(ex.getMessage()))
    );

    public Throwable convert(Throwable t) {
        if (!(t instanceof CommandException wrapperException)) {
            return null;
        }

        Throwable underlyingException = wrapperException.getCause();
        if (underlyingException == null) {
            return null;
        }

        for (Conversion conversion : CONVERSIONS) {
            if (!conversion.matches(underlyingException)) {
                continue;
            }

            return conversion.convert(underlyingException, wrapperException.getCommands());
        }

        return null;
    }
}
