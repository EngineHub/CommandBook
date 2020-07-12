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

package com.sk89q.commandbook.component.locations;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.ComponentCommandRegistrar;
import com.sk89q.commandbook.component.session.SessionComponent;
import com.sk89q.commandbook.component.session.SessionFactory;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

@ComponentInformation(friendlyName = "Teleports", desc = "Teleport-related commands")
@Depend(components = SessionComponent.class)
public class TeleportComponent extends BukkitComponent implements Listener {

    @InjectComponent private SessionComponent sessions;
    private LocalConfiguration config;

    @Override
    public void enable() {
        CommandBook.registerEvents(this);

        ComponentCommandRegistrar registrar = CommandBook.getComponentRegistrar();
        registrar.registerTopLevelCommands((commandManager, registration) -> {
            LocationTargetConverter.register(commandManager);
            registration.register(commandManager, TeleportCommandsRegistration.builder(), new TeleportCommands(this));
        });

        config = configure(new LocalConfiguration());
        sessions.registerSessionFactory(TeleportSession.class, new SessionFactory<TeleportSession>() {
            @Override
            public TeleportSession createSession(CommandSender user) {
                return new TeleportSession(TeleportComponent.this);
            }
        });
    }

    @Override
    public void reload() {
        configure(config);
    }

    public LocalConfiguration getConfig() {
        return config;
    }

    protected TeleportSession getSession(Player player) {
        return sessions.getSession(TeleportSession.class, player);
    }

    public static class LocalConfiguration extends ConfigurationBase {
        @Setting("call-message.sender") public String callMessageSender = "`yTeleport request sent.";
        @Setting("call-message.target") public String callMessageTarget =
                "`c**TELEPORT** %cname%`c requests a teleport! Use /bring <name> to accept.";
        @Setting("call-message.too-soon") public String callMessageTooSoon = "Wait a bit before asking again.";
        @Setting("bring-message.sender") public String bringMessageSender = "`yPlayer teleported.";
        @Setting("bring-message.target") public String bringMessageTarget =
                "`yYour teleport request to %cname%`y was accepted.";
        @Setting("bring-message.no-perm") public String bringMessageNoPerm = "That person didn't request a " +
                "teleport (recently) and you don't have permission to teleport anyone.";
    }

    // -- Event handlers

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        sessions.getSession(TeleportSession.class, event.getPlayer()).rememberLocation(event.getPlayer());
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {

        Location loc = event.getTo();
        Player player = event.getPlayer();
        if (event.isCancelled()) {
            return;
        }

        Location ignored = sessions.getSession(TeleportSession.class, player).getIgnoreLocation();
        if (ignored != null) {
            if (ignored.getWorld().equals(loc.getWorld()) && ignored.distanceSquared(loc) <= 2) {
                return;
            } else {
                sessions.getSession(TeleportSession.class, player).setIgnoreLocation(null);
            }
        }
        sessions.getSession(TeleportSession.class, player).rememberLocation(event.getPlayer());
    }

    public class Commands {

    }
}
