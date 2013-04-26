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

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.CommandBookUtil;
import com.sk89q.commandbook.session.SessionComponent;
import com.sk89q.commandbook.session.SessionFactory;
import com.sk89q.commandbook.util.LegacyBukkitCompat;
import com.sk89q.commandbook.util.LocationUtil;
import com.sk89q.commandbook.util.PlayerUtil;
import com.sk89q.commandbook.util.TeleportPlayerIterator;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BasePlugin;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;

@ComponentInformation(friendlyName = "Teleports", desc = "Teleport-related commands")
@Depend(components = SessionComponent.class)
public class TeleportComponent extends BukkitComponent implements Listener {

    @InjectComponent private SessionComponent sessions;
    private LocalConfiguration config;

    @Override
    public void enable() {
        CommandBook.registerEvents(this);
        registerCommands(Commands.class);
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
        if (loc == sessions.getSession(TeleportSession.class, player).getIgnoreLocation()) {
            sessions.getSession(TeleportSession.class, player).setIgnoreLocation(null);
            return;
        }
        sessions.getSession(TeleportSession.class, player).rememberLocation(event.getPlayer());
    }

    public class Commands {

        @Command(aliases = {"teleport", "tp"}, usage = "[target] <destination>",
                desc = "Teleport to a location",
                flags = "s", min = 1, max = 4)
        @CommandPermissions({"commandbook.teleport"})
        public void teleport(CommandContext args, CommandSender sender) throws CommandException {

            Iterable<Player> targets;
            final Location loc;
            boolean[] relative = new boolean[]{false, false, false};

            /*
            1. /tp playerTarget x y z (4 args) - VANILLA

            2. /tp x y z (3 args)
            3. /tp playerTarget x z (3 args) - NOT SUPPORTED

            4. /tp playerTarget x,y,z (2 args)
            5. /tp playerTarget playerDest (2 args) - VANILLA
            6. /tp x z (2 args) - NOT SUPPORTED

            7. /tp x,y,z (1 arg)
            8. /tp playerDest (1 arg) - VANILLA
             */
            // TODO: reduce code duplication, currently just trying to catch every case
            if (args.argsLength() == 1) {
                loc = LocationUtil.matchLocation(sender, args.getString(0)); // matches both #7 and #8
                // go to the center of the block if we're on the edge
                if (loc.getX() == loc.getBlockX()) loc.add(0.5, 0, 0);
                if (loc.getZ() == loc.getBlockZ()) loc.add(0, 0, 0.5);
                targets = PlayerUtil.matchPlayers(PlayerUtil.checkPlayer(sender));
                // check target location (CommandPermissions annotation only checks sender's location)
                CommandBook.inst().checkPermission(sender, loc.getWorld(), "commandbook.teleport");
            } else if (args.argsLength() == 2) {
                targets = PlayerUtil.matchPlayers(sender, args.getString(0));
                loc = LocationUtil.matchLocation(sender, args.getString(1)); // matches both #4 and #5
                if (loc.getX() == loc.getBlockX()) loc.add(0.5, 0, 0);
                if (loc.getZ() == loc.getBlockZ()) loc.add(0, 0, 0.5);

                // Check permissions!
                CommandBook.inst().checkPermission(sender, loc.getWorld(), "commandbook.teleport");
                for (Player target : targets) {
                    if (target != sender) { // if any of the targets is not the sender, we need to check .other
                        CommandBook.inst().checkPermission(sender, "commandbook.teleport.other");
                        if (sender instanceof Player) {
                            CommandBook.inst().checkPermission(sender, loc.getWorld(), "commandbook.teleport.other");
                        }
                        break;
                    }
                }
            } else if (args.argsLength() == 3) {
                // matches #2 - can only be used by a player
                targets = PlayerUtil.matchPlayers(PlayerUtil.checkPlayer(sender));
                double x = args.getDouble(0);
                double y = args.getDouble(1);
                double z = args.getDouble(2);
                loc = new Location((PlayerUtil.checkPlayer(sender)).getWorld(), x, y, z);
                if (loc.getX() == loc.getBlockX()) loc.add(0.5, 0, 0);
                if (loc.getZ() == loc.getBlockZ()) loc.add(0, 0, 0.5);
                // check location permission
                CommandBook.inst().checkPermission(sender, loc.getWorld(), "commandbook.teleport");
                CommandBook.inst().checkPermission(sender, loc.getWorld(), "commandbook.locations.coords");
            } else if (args.argsLength() == 4) {
                targets = PlayerUtil.matchPlayers(sender, args.getString(0)); // matches #1
                // support relative location (~5 -> current coord + 5)
                String xArg = args.getString(1);
                String yArg = args.getString(2);
                String zArg = args.getString(3);
                CommandBook.inst().checkPermission(sender, "commandbook.locations.coords");
                if (xArg.startsWith("~")) relative[0] = true;
                if (yArg.startsWith("~")) relative[1] = true;
                if (zArg.startsWith("~")) relative[2] = true;
                if (relative[0] || relative[1] || relative[2]) {
                    CommandBook.inst().checkPermission(sender, "commandbook.locations.coords.relative");
                }
                double x = Double.parseDouble(xArg.replace("~", ""));
                double y = Double.parseDouble(yArg.replace("~", ""));
                double z = Double.parseDouble(zArg.replace("~", ""));
                World world;
                try { // for CommandBlock support
                    world = LegacyBukkitCompat.extractWorld(sender);
                } catch (Throwable t) {
                    if (sender instanceof Player) {
                        world = ((Player) sender).getWorld();
                    } else {
                        world = BasePlugin.server().getWorlds().get(0);
                    }
                }
                loc = new Location(world, x, y, z);
                CommandBook.inst().checkPermission(sender, loc.getWorld(), "commandbook.teleport");
                for (Player target : targets) {
                    if (target != sender) { // if any of the targets is not the sender, we need to check .other
                        CommandBook.inst().checkPermission(sender, "commandbook.teleport.other");
                        if (sender instanceof Player) {
                            CommandBook.inst().checkPermission(sender, loc.getWorld(), "commandbook.teleport.other");
                        }
                        break;
                    }
                }
            } else { // this can't actually happen unless someone constructs their own CommandContext
                throw new CommandException("Invalid number of args.");
            }

            (new TeleportPlayerIterator(sender, loc, args.hasFlag('s'), relative)).iterate(targets);
        }

        @Command(aliases = {"call"}, usage = "<target>", desc = "Request a teleport", min = 1, max = 1)
        @CommandPermissions({"commandbook.call"})
        public void requestTeleport(CommandContext args, CommandSender sender) throws CommandException {
            Player player = PlayerUtil.checkPlayer(sender);
            Player target = PlayerUtil.matchSinglePlayer(sender, args.getString(0));

            CommandBook.inst().checkPermission(sender, target.getWorld(), "commandbook.call");

            sessions.getSession(TeleportSession.class, player).checkLastTeleportRequest(target);
            sessions.getSession(TeleportSession.class, target).addBringable(player);

            String senderMessage = CommandBookUtil.replaceColorMacros(
                    CommandBookUtil.replaceMacros(sender, config.callMessageSender))
                    .replaceAll("%ctarget%", PlayerUtil.toColoredName(target, null))
                    .replaceAll("%target%", PlayerUtil.toName(target));
            String targetMessage = CommandBookUtil.replaceColorMacros(
                    CommandBookUtil.replaceMacros(sender, config.callMessageTarget))
                    .replaceAll("%ctarget%", PlayerUtil.toColoredName(target, null))
                    .replaceAll("%target%", PlayerUtil.toName(target));
            sender.sendMessage(senderMessage);
            target.sendMessage(targetMessage);
        }

        @Command(aliases = {"bring", "tphere", "summon", "s"}, usage = "<target>", desc = "Bring a player to you", min = 1, max = 1)
        public void bring(CommandContext args, CommandSender sender) throws CommandException {
            Player player = PlayerUtil.checkPlayer(sender);
            if (!CommandBook.inst().hasPermission(sender, "commandbook.teleport.other")) {
                Player target = PlayerUtil.matchSinglePlayer(sender, args.getString(0));

                if (sessions.getSession(TeleportSession.class, player).isBringable(target)) {
                    String senderMessage = CommandBookUtil.replaceColorMacros(
                            CommandBookUtil.replaceMacros(sender, config.bringMessageSender))
                            .replaceAll("%ctarget%", PlayerUtil.toColoredName(target, null))
                            .replaceAll("%target%", target.getName());
                    String targetMessage = CommandBookUtil.replaceColorMacros(
                            CommandBookUtil.replaceMacros(sender, config.bringMessageTarget))
                            .replaceAll("%ctarget%", PlayerUtil.toColoredName(target, null))
                            .replaceAll("%target%", target.getName());
                    sender.sendMessage(senderMessage);
                    target.sendMessage(targetMessage);
                    target.teleport(player);
                } else {
                    throw new CommandException(config.bringMessageNoPerm);
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
            Location lastLoc = sessions.getSession(TeleportSession.class, player).popLastLocation();

            if (lastLoc != null) {
                sessions.getSession(TeleportSession.class, player).setIgnoreLocation(lastLoc);
                lastLoc.getChunk().load(true);
                player.teleport(lastLoc);
                sender.sendMessage(ChatColor.YELLOW + "You've been returned.");
            } else {
                sender.sendMessage(ChatColor.RED + "There's no past location in your history.");
            }
        }
    }
}
