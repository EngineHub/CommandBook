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
import com.sk89q.commandbook.session.UserSession;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.sk89q.commandbook.session.SessionComponent;
import com.sk89q.commandbook.util.LocationUtil;
import com.sk89q.commandbook.util.PlayerUtil;
import com.sk89q.commandbook.util.TeleportPlayerIterator;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import org.bukkit.ChatColor;
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

    @Override
    public void enable() {
        CommandBook.registerEvents(this);
        registerCommands(Commands.class);
    }

    // -- Event handlers

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        sessions.getSession(UserSession.class, event.getPlayer()).rememberLocation(event.getPlayer());
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {

        Location loc = event.getTo();
        Player player = event.getPlayer();
        if (event.isCancelled()) {
            return;
        }
        if (loc == sessions.getSession(UserSession.class, player).getIgnoreLocation()) {
            sessions.getSession(UserSession.class, player).setIgnoreLocation(null);
            return;
        }
        sessions.getSession(UserSession.class, player).rememberLocation(event.getPlayer());
    }

    public class Commands {

        @Command(aliases = {"teleport", "tp"}, usage = "[target] <destination>",
                desc = "Teleport to a location",
                flags = "s", min = 1, max = 2)
        @CommandPermissions({"commandbook.teleport"})
        public void teleport(CommandContext args, CommandSender sender) throws CommandException {

            Iterable<Player> targets;
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
                for (Player target : targets) {
                    if (target != sender) {
                        CommandBook.inst().checkPermission(sender, "commandbook.teleport.other");
                        if (sender instanceof Player) {
                            CommandBook.inst().checkPermission(sender, loc.getWorld(), "commandbook.teleport.other");
                        }
                        break;
                    }
                }
            }

            (new TeleportPlayerIterator(sender, loc, args.hasFlag('s'))).iterate(targets);
        }

        @Command(aliases = {"call"}, usage = "<target>", desc = "Request a teleport", min = 1, max = 1)
        @CommandPermissions({"commandbook.call"})
        public void requestTeleport(CommandContext args, CommandSender sender) throws CommandException {
            Player player = PlayerUtil.checkPlayer(sender);
            Player target = PlayerUtil.matchSinglePlayer(sender, args.getString(0));

            CommandBook.inst().checkPermission(sender, target.getWorld(), "commandbook.call");

            sessions.getSession(UserSession.class, player).checkLastTeleportRequest(target);
            sessions.getSession(UserSession.class, target).addBringable(player);

            sender.sendMessage(ChatColor.YELLOW.toString() + "Teleport request sent.");
            target.sendMessage(ChatColor.AQUA + "**TELEPORT** " + PlayerUtil.toName(sender)
                    + " requests a teleport! Use /bring <name> to accept.");
        }

        @Command(aliases = {"bring", "tphere", "summon", "s"}, usage = "<target>", desc = "Bring a player to you", min = 1, max = 1)
        public void bring(CommandContext args, CommandSender sender) throws CommandException {
            Player player = PlayerUtil.checkPlayer(sender);
            if (!CommandBook.inst().hasPermission(sender, "commandbook.teleport.other")) {
                Player target = PlayerUtil.matchSinglePlayer(sender, args.getString(0));

                if (sessions.getSession(UserSession.class, player).isBringable(target)) {
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

        @Command(aliases = {"put", "place"}, usage = "<target>",
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

        @Command(aliases = {"return", "ret"}, usage = "[player]", desc = "Teleport back to your last location", min = 0, max = 1)
        @CommandPermissions({"commandbook.return"})
        public void ret(CommandContext args, CommandSender sender) throws CommandException {
            Player player;
            if (args.argsLength() > 0) {
                player = PlayerUtil.matchSinglePlayer(sender, args.getString(0));
                if (player != sender) {
                    CommandBook.inst().checkPermission(sender, "commandbook.return.other");
                }
            } else {
                player = PlayerUtil.checkPlayer(sender);
            }
            Location lastLoc = sessions.getSession(UserSession.class, player).popLastLocation();

            if (lastLoc != null) {
                sessions.getSession(UserSession.class, player).setIgnoreLocation(lastLoc);
                player.teleport(lastLoc);
                sender.sendMessage(ChatColor.YELLOW + "You've been returned.");
            } else {
                sender.sendMessage(ChatColor.RED + "There's no past location in your history.");
            }
        }
    }
}
