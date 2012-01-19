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

package com.sk89q.commandbook;

import com.sk89q.commandbook.components.AbstractComponent;
import com.sk89q.commandbook.components.ComponentInformation;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

import java.util.regex.Pattern;

@ComponentInformation(friendlyName = "Name Verification", desc = "This component verifies that player names are valid when they join.")
public class NameVerificationComponent extends AbstractComponent implements Listener {
    protected final static Pattern namePattern = Pattern.compile(
            "^[a-zA-Z0-9_]{1,16}$");

    @Override
    public void initialize() {
        CommandBook.registerEvents(this);
    }

    @EventHandler
    public void onLogin(PlayerLoginEvent event) {
        if (!namePattern.matcher(event.getPlayer().getName()).matches()) {
            CommandBook.logger().info("Name verification: " + event.getPlayer().getName() + " was kicked " +
                    "for having an invalid name (to disable, turn off the name-verification component in CommandBook)");
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, "Invalid player name detected!");
        }
    }
}
