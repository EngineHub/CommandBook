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
import com.zachsthings.libcomponents.spout.SpoutComponent;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.sk89q.commandbook.session.SessionComponent;
import com.sk89q.commandbook.util.LocationUtil;
import com.sk89q.commandbook.util.PlayerUtil;
import com.sk89q.commandbook.util.TeleportPlayerIterator;
import org.spout.api.ChatColor;
import org.spout.api.command.CommandContext;
import org.spout.api.command.CommandSource;
import org.spout.api.command.annotated.Command;
import org.spout.api.command.annotated.CommandPermissions;
import org.spout.api.entity.PlayerController;
import org.spout.api.event.EventHandler;
import org.spout.api.event.Listener;
import org.spout.api.event.entity.EntityTeleportEvent;
import org.spout.api.exception.CommandException;
import org.spout.api.geo.discrete.Point;
import org.spout.api.geo.discrete.atomic.Transform;
import org.spout.api.player.Player;

@ComponentInformation(friendlyName = "Teleports", desc = "Teleport-related commands")
@Depend(components = SessionComponent.class)
public class TeleportComponent extends SpoutComponent implements Listener {
    
    @InjectComponent private SessionComponent sessions;

    @Override
    public void enable() {
        CommandBook.game().getEventManager().registerEvents(this, this);
        registerCommands(Commands.class);
    }

    // -- Event handlers
    
    /*@EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        sessions.getSession(event.getPlayer()).rememberLocation(event.getPlayer());
    }*/
    
    @EventHandler
    public void onTeleport(EntityTeleportEvent event) {
        if (event.getEntity().is(PlayerController.class)) {
            Player player = ((PlayerController) event.getEntity().getController()).getPlayer();
            Point loc = event.getTo();
            if (event.isCancelled()) {
                return;
            }
            if (loc.equals(sessions.getSession(player).getIgnoreLocation().getPosition())) {
                sessions.getSession(player).setIgnoreLocation(null);
                return;
            }
            sessions.getSession(player).rememberLocation(player);
        }
    }

    public class Commands {

        @Command(aliases = {"teleport", "tp"}, usage = "[target] <destination>",
                desc = "Teleport to a location",
                flags = "s", min = 1, max = 2)
        @CommandPermissions({"commandbook.teleport"})
        public void teleport(CommandContext args, CommandSource sender) throws CommandException {

            Iterable<Player> targets;
            final Transform loc;

            // Detect arguments based on the number of arguments provided
            if (args.length() == 1) {
                targets = PlayerUtil.matchPlayers(PlayerUtil.checkPlayer(sender));
                loc = LocationUtil.matchLocation(sender, args.getString(0));
                if (sender instanceof Player) {
                    CommandBook.inst().checkPermission(sender, loc.getPosition().getWorld(), "commandbook.teleport");
                }
            } else {
                targets = PlayerUtil.matchPlayers(sender, args.getString(0));
                loc = LocationUtil.matchLocation(sender, args.getString(1));

                // Check permissions!
                for (Player target : targets) {
                    if (target != sender) {
                        CommandBook.inst().checkPermission(sender, "commandbook.teleport.other");
                        if (sender instanceof Player) {
                            CommandBook.inst().checkPermission(sender, loc.getPosition().getWorld(), "commandbook.teleport.other");
                        }
                        break;
                    }
                }
            }
            
            (new TeleportPlayerIterator(sender, loc, args.hasFlag('s'))).iterate(targets);
        }

        @Command(aliases = {"call"}, usage = "<target>", desc = "Request a teleport", min = 1, max = 1)
        @CommandPermissions({"commandbook.call"})
        public void requestTeleport(CommandContext args, CommandSource sender) throws CommandException {
            Player player = PlayerUtil.checkPlayer(sender);
            Player target = PlayerUtil.matchSinglePlayer(sender, args.getString(0));

            CommandBook.inst().checkPermission(sender, target.getEntity().getWorld(), "commandbook.call");

            sessions.getSession(player).checkLastTeleportRequest(target);
            sessions.getSession(target).addBringable(player);

            sender.sendMessage(ChatColor.YELLOW.toString() + "Teleport request sent.");
            target.sendMessage(ChatColor.CYAN + "**TELEPORT** " + PlayerUtil.toName(sender)
                    + " requests a teleport! Use /bring <name> to accept.");
        }

        @Command(aliases = {"bring", "tphere", "summon", "s"}, usage = "<target>", desc = "Bring a player to you", min = 1, max = 1)
        public void bring(CommandContext args, CommandSource sender) throws CommandException {
            Player player = PlayerUtil.checkPlayer(sender);
            if (!sender.hasPermission("commandbook.teleport.other")) {
                Player target = PlayerUtil.matchSinglePlayer(sender, args.getString(0));

                if (sessions.getSession(player).isBringable(target)) {
                    sender.sendMessage(ChatColor.YELLOW + "Player teleported.");
                    target.sendMessage(ChatColor.YELLOW + "Your teleport request to "
                            + PlayerUtil.toName(sender) + " was accepted.");
                    target.getEntity().setTransform(player.getEntity().getTransform());
                } else {
                    throw new CommandException("That person didn't request a " +
                            "teleport (recently) and you don't have " +
                            "permission to teleport anyone.");
                }

                return;
            }

            Iterable<Player> targets = PlayerUtil.matchPlayers(sender, args.getString(0));
            Transform loc = player.getEntity().getTransform();

            (new TeleportPlayerIterator(sender, loc) {
                @Override
                public void perform(Player player) {
                    if (sender instanceof Player) {
                        if (!player.getEntity().getWorld().getName().equals(((Player) sender).getEntity().getWorld().getName())) {
                            if (!sender.hasPermission(player.getEntity().getWorld(), "commandbook.teleport.other")) {
                                return;
                            }
                        }
                    }
                    oldLoc = player.getEntity().getTransform();
                    player.getEntity().setTransform(loc);
                }
            }).iterate(targets);
        }

        @Command(aliases = {"put", "place"}, usage = "<target>",
                desc = "Put a player at where you are looking", min = 1, max = 1)
        @CommandPermissions({"commandbook.teleport.other"})
        public void put(CommandContext args, CommandSource sender) throws CommandException {
            Iterable<Player> targets = PlayerUtil.matchPlayers(sender, args.getString(0));
            Transform loc = LocationUtil.matchLocation(sender, "#target");

            (new TeleportPlayerIterator(sender, loc) {
                @Override
                public void perform(Player player) {
                    oldLoc = player.getEntity().getTransform();

                    Transform playerLoc = player.getEntity().getTransform();
                    loc.setRotation(playerLoc.getRotation());
                    player.getEntity().setTransform(loc);
                }

            }).iterate(targets);
        }

        @Command(aliases = {"return", "ret"}, usage = "[player]", desc = "Teleport back to your last location", min = 0, max = 1)
        @CommandPermissions({"commandbook.return"})
        public void ret(CommandContext args, CommandSource sender) throws CommandException {
            Player player;
            if (args.length() > 0) {
                player = PlayerUtil.matchSinglePlayer(sender, args.getString(0));
                if (player != sender) {
                    CommandBook.inst().checkPermission(sender, "commandbook.return.other");
                }
            } else {
                player = PlayerUtil.checkPlayer(sender);
            }
            Transform lastLoc = sessions.getSession(player).popLastLocation();

            if (lastLoc != null) {
                sessions.getSession(player).setIgnoreLocation(lastLoc);
                player.getEntity().setTransform(lastLoc);
                sender.sendMessage(ChatColor.YELLOW + "You've been returned.");
            } else {
                sender.sendMessage(ChatColor.RED + "There's no past location in your history.");
            }
        }
    }
}
