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

package com.sk89q.commandbook.component.fun;

import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;

@ComponentInformation(friendlyName = "Fun", desc = "Provides some fun commands to toy with users. (/rocket and /pong are two fun ones)")
public class FunComponent extends BukkitComponent {
    private FunComponentConfiguration config;

    @Override
    public void enable() {
        config = configure(new FunComponentConfiguration());

        CommandBook.getComponentRegistrar().registerTopLevelCommands((registrar) -> {
            registrar.register(FunCommandsRegistration.builder(), new FunCommands(this));
        });
    }

    @Override
    public void reload() {
        super.reload();
        configure(config);
    }

    protected FunComponentConfiguration getConfig() {
        return config;
    }
}
