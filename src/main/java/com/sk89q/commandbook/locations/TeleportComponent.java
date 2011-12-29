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
import com.sk89q.commandbook.components.InjectComponent;
import com.sk89q.commandbook.config.ConfigurationBase;
import com.sk89q.commandbook.config.Setting;
import com.sk89q.commandbook.events.core.BukkitEvent;
import com.sk89q.commandbook.session.SessionComponent;
import com.sk89q.commandbook.util.LocationUtil;
import com.sk89q.commandbook.util.PlayerIteratorAction;
import com.sk89q.commandbook.util.PlayerUtil;
import com.sk89q.commandbook.util.TeleportPlayerIterator;
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

/**
 * @author zml2008
 */
public class TeleportComponent extends AbstractComponent implements Listener {
    
    @InjectComponent private SessionComponent sessions;

    private WrappedSpawnManager spawns;

    private LocalConfiguration config;

    @Override
    public void initialize() {
        spawns = new WrappedSpawnManager(new File(CommandBook.inst().getDataFolder(), "spawns.yml"));
        config = configure(new LocalConfiguration());
        CommandBook.inst().getEventManager().registerEvents(this, this);
        registerCommands(Commands.class);
    }

    @Override
    public void reload() {
        spawns.load();
        config = configure(new LocalConfiguration());
    }

    public WrappedSpawnManager getSpawnManager() {
        return spawns;
    }

    private static class LocalConfiguration extends ConfigurationBase {
        @Setting("exact-spawn") public boolean exactSpawn;
    }

    // -- Event handlers
    
    @BukkitEvent(type = Event.Type.PLAYER_RESPAWN)
    public void onRespawn(PlayerRespawnEvent event) {
        sessions.getSession(event.getPlayer()).rememberLocation(event.getPlayer());
        Player player = event.getPlayer();
        if (config.exactSpawn && !event.isBedSpawn()) {
            event.setRespawnLocation(spawns.getWorldSpawn(player.getWorld()));
        }
    }
    
    @BukkitEvent(type = Event.Type.PLAYER_TELEPORT)
    public void onTeleport(PlayerTeleportEvent event) {

        Location loc = event.getTo();
        Player player = event.getPlayer();
        if (event.isCancelled()) {
            return;
        }
        if (loc == sessions.getSession(player).getIgnoreLocation()) {
            sessions.getSession(player).setIgnoreLocation(null);
            return;
        }
        sessions.getSession(event.getPlayer()).rememberLocation(event.getPlayer());
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
    
    private class Commands {
        @Command(aliases = {"spawn"}, usage = "[player]", desc = "Teleport to spawn", min = 0, max = 1)
        @CommandPermissions({"commandbook.spawn"})
        public void spawn(CommandContext args, CommandSender sender) throws CommandException {
            Iterable<Player> targets = null;

            // Detect arguments based on the number of arguments provided
            if (args.argsLength() == 1) {
                targets = PlayerUtil.matchPlayers(sender, args.getString(0));

                // Check permissions!
                CommandBook.inst().checkPermission(sender, "commandbook.spawn.other");
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

        @Command(aliases = {"teleport"}, usage = "[target] <destination>",
                desc = "Teleport to a location", min = 1, max = 2)
        @CommandPermissions({"commandbook.teleport"})
        public void teleport(CommandContext args, CommandSender sender) throws CommandException {

            Iterable<Player> targets = null;
            final Location loc;

            // Detect arguments based on the number of arguments provided
            if (args.argsLength() == 1) {
                targets = PlayerUtil.matchPlayers(PlayerUtil.checkPlayer(sender));
                loc = LocationUtil.matchLocation(sender, args.getString(0));
                if (sender instanceof Player) {
                    CommandBook.inst().checkPermission(sender, loc.getWorld(), "commandbook.teleport");
                }
            } else {
                targets = PlayerUtil.matchPlayers(sender, args.getString(0));
                loc = LocationUtil.matchLocation(sender, args.getString(1));

                // Check permissions!
                CommandBook.inst().checkPermission(sender, "commandbook.teleport.other");
                if (sender instanceof Player) {
                    CommandBook.inst().checkPermission(sender, loc.getWorld(), "commandbook.teleport.other");
                }
            }

            (new TeleportPlayerIterator(sender, loc)).iterate(targets);
        }

        @Command(aliases = {"call"}, usage = "<target>", desc = "Request a teleport", min = 1, max = 1)
        @CommandPermissions({"commandbook.call"})
        public void requestTeleport(CommandContext args, CommandSender sender) throws CommandException {
            Player player = PlayerUtil.checkPlayer(sender);
            Player target = PlayerUtil.matchSinglePlayer(sender, args.getString(0));

            CommandBook.inst().checkPermission(sender, target.getWorld(), "commandbook.call");

            sessions.getSession(player).checkLastTeleportRequest(target);
            sessions.getSession(target).addBringable(player);

            sender.sendMessage(ChatColor.YELLOW.toString() + "Teleport request sent.");
            target.sendMessage(ChatColor.AQUA + "**TELEPORT** " + PlayerUtil.toName(sender)
                    + " requests a teleport! Use /bring <name> to accept.");
        }

        @Command(aliases = {"bring"}, usage = "<target>", desc = "Bring a player to you", min = 1, max = 1)
        public void bring(CommandContext args, CommandSender sender) throws CommandException {
            Player player = PlayerUtil.checkPlayer(sender);
            if (!CommandBook.inst().hasPermission(sender, "commandbook.teleport.other")) {
                Player target = PlayerUtil.matchSinglePlayer(sender, args.getString(0));

                if (sessions.getSession(player).isBringable(target)) {
                    sender.sendMessage(ChatColor.YELLOW + "Player teleported.");
                    target.sendMessage(ChatColor.YELLOW + "Your teleport request to "
                            + PlayerUtil.toName(sender) + " was accepted.");
                    target.teleport(player);
                } else {
                    throw new CommandException("That person didn't request a " +
                            "teleport (recently) and you don't have " +
                            "permission to teleport anyone.");
                }

                return;
            }

            Iterable<Player> targets = PlayerUtil.matchPlayers(sender, args.getString(0));
            Location loc = player.getLocation();

            (new TeleportPlayerIterator(sender, loc) {
                @Override
                public void perform(Player player) {
                    if (sender instanceof Player) {
                        if (!player.getWorld().getName().equals(((Player) sender).getWorld().getName())) {
                            if (!CommandBook.inst().hasPermission(sender, player.getWorld(), "commandbook.teleport.other")) {
                                return;
                            }
                        }
                    }
                    oldLoc = player.getLocation();
                    player.teleport(loc);
                }
            }).iterate(targets);
        }

        @Command(aliases = {"put"}, usage = "<target>",
                desc = "Put a player at where you are looking", min = 1, max = 1)
        @CommandPermissions({"commandbook.teleport.other"})
        public void put(CommandContext args, CommandSender sender) throws CommandException {
            Iterable<Player> targets = PlayerUtil.matchPlayers(sender, args.getString(0));
            Location loc = LocationUtil.matchLocation(sender, "#target");

            (new TeleportPlayerIterator(sender, loc) {
                @Override
                public void perform(Player player) {
                    oldLoc = player.getLocation();

                    Location playerLoc = player.getLocation();
                    loc.setPitch(playerLoc.getPitch());
                    loc.setYaw(playerLoc.getYaw());
                    player.teleport(loc);
                }

            }).iterate(targets);
        }

        @Command(aliases = {"return"}, usage = "", desc = "Teleport back to your last location", min = 0, max = 0)
        @CommandPermissions({"commandbook.return"})
        public void ret(CommandContext args, CommandSender sender) throws CommandException {
            Player player = PlayerUtil.checkPlayer(sender);
            Location lastLoc = sessions.getSession(player).popLastLocation();

            if (lastLoc != null) {
                sessions.getSession(player).setIgnoreLocation(lastLoc);
                player.teleport(lastLoc);
                sender.sendMessage(ChatColor.YELLOW + "You've been returned.");
            } else {
                sender.sendMessage(ChatColor.RED + "There's no past location in your history.");
            }
        }
    }
}
