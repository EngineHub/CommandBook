/*
 * CommandBook
 * Copyright (C) 2011 sk89q <http://www.sk89q.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.commandbook.util;

import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandsManager;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

/**
 * @author zml2008
 */
public class CommandRegistration {
	private final Plugin plugin;
	private final CommandsManager<?> commands;
	
	public CommandRegistration(Plugin plugin, CommandsManager<?> commands) {
		this.plugin = plugin;
		this.commands = commands;
	}
    public boolean register(Class<?> clazz) {
        List<Command> registered = commands.registerAndReturn(clazz);
        SimpleCommandMap commandMap = ReflectionUtil.getField(plugin.getServer().getPluginManager(), "commandMap");
        if (registered == null || commandMap == null) {
            return false;
        }
        for (Command command : registered) {
            commandMap.register(plugin.getDescription().getName(), new DynamicPluginCommand(command, plugin));
        }
        return true;
    }

    public static class DynamicPluginCommand extends org.bukkit.command.Command {

		protected final Plugin plugin;

        public DynamicPluginCommand(Command command, Plugin plugin) {
            super(command.aliases()[0], command.desc(), command.usage(), Arrays.asList(command.aliases()));
			this.plugin = plugin;
        }

        @Override
        public boolean execute(CommandSender sender, String label, String[] args) {
            return plugin.onCommand(sender, this, label, args);
        }
    }
}
