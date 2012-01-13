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

package com.sk89q.commandbook.locations;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.components.AbstractComponent;
import com.sk89q.commandbook.components.ComponentInformation;
import com.sk89q.commandbook.config.ConfigurationBase;
import com.sk89q.commandbook.config.Setting;
import com.sk89q.commandbook.events.core.BukkitEvent;
import com.sk89q.commandbook.util.LocationUtil;
import com.sk89q.commandbook.util.PlayerIteratorAction;
import com.sk89q.commandbook.util.PlayerUtil;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.io.File;

@ComponentInformation(friendlyName = "Spawn Locations", desc = "Allows exact spawn locations for players, " +
        "as well as getting and setting the world's spawn.")
public class SpawnLocationsComponent extends AbstractComponent implements Listener {

    private WrappedSpawnManager spawns;

    private LocalConfiguration config;

    @Override
    public void initialize() {
        spawns = new WrappedSpawnManager(new File(CommandBook.inst().getDataFolder(), "spawns.yml"));
        config = configure(new LocalConfiguration());
        registerCommands(Commands.class);
    }

    @Override
    public void reload() {
        super.reload();
        spawns.load();
    }

    public WrappedSpawnManager getSpawnManager() {
        return spawns;
    }

    private static class LocalConfiguration extends ConfigurationBase {
        @Setting("exact-spawn") public boolean exactSpawn;
    }

    @BukkitEvent(type = Event.Type.PLAYER_RESPAWN)
    public void onRespawn(PlayerRespawnEvent event) {
        if (config.exactSpawn && !event.isBedSpawn()) {
            event.setRespawnLocation(spawns.getWorldSpawn(event.getPlayer().getWorld()));
        }
    }

    @BukkitEvent(type = Event.Type.PLAYER_TELEPORT)
    public void onTeleport(PlayerTeleportEvent event) {

        Location loc = event.getTo();
        if (event.isCancelled()) {
            return;
        }
        if (loc.equals(loc.getWorld().getSpawnLocation())) {
            event.setTo(spawns.getWorldSpawn(loc.getWorld()));
        }
    }

    @BukkitEvent(type = Event.Type.PLAYER_JOIN)
    public void onJoin(PlayerJoinEvent event) {
        if (!event.getPlayer().hasPlayedBefore() && config.exactSpawn) {
            event.getPlayer().teleport(spawns.getWorldSpawn(event.getPlayer().getWorld()));
        }
    }

    public class Commands {
        @Command(aliases = {"spawn"}, usage = "[player]", desc = "Teleport to spawn", min = 0, max = 1)
        @CommandPermissions({"commandbook.spawn"})
        public void spawn(CommandContext args, CommandSender sender) throws CommandException {
            Iterable<Player> targets = null;

            // Detect arguments based on the number of arguments provided
            if (args.argsLength() == 1) {
                targets = PlayerUtil.matchPlayers(sender, args.getString(0));

                // Check permissions!
                for (Player target : targets) {
                    if (target != sender) {
                        CommandBook.inst().checkPermission(sender, "commandbook.spawn.other");
                        break;
                    }
                }
            } else {
                targets = PlayerUtil.matchPlayers(PlayerUtil.checkPlayer(sender));
            }

            (new PlayerIteratorAction(sender) {

                @Override
                public void perform(Player player) {
                    player.teleport(getSpawnManager().getWorldSpawn(player.getWorld()));
                }

                @Override
                public void onCaller(Player player) {
                    player.sendMessage(ChatColor.YELLOW + "Teleported to spawn.");
                }

                @Override
                public void onVictim(CommandSender sender, Player player) {
                    player.sendMessage(ChatColor.YELLOW + "Teleported to spawn by "
                            + PlayerUtil.toName(sender) + ".");
                }

                @Override
                public void onInformMany(CommandSender sender, int affected) {
                    sender.sendMessage(ChatColor.YELLOW.toString()
                            + affected + " teleported to spawn.");
                }

            }).iterate(targets);
        }


        @Command(aliases = {"setspawn"},
                usage = "[location]", desc = "Change spawn location",
                flags = "", min = 0, max = 1)
        @CommandPermissions({"commandbook.setspawn"})
        public void setspawn(CommandContext args, CommandSender sender) throws CommandException {

            World world;
            Location loc;

            if (args.argsLength() == 0) {
                Player player = PlayerUtil.checkPlayer(sender);
                world = player.getWorld();
                loc = player.getLocation();
            } else {
                loc = LocationUtil.matchLocation(sender, args.getString(0));
                world = loc.getWorld();
            }

            getSpawnManager().setWorldSpawn(loc);

            sender.sendMessage(ChatColor.YELLOW +
                    "Spawn location of '" + world.getName() + "' set!");
        }
    }
}
