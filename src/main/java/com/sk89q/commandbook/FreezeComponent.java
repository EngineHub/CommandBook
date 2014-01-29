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

import com.sk89q.commandbook.session.PersistentSession;
import com.sk89q.commandbook.session.SessionComponent;
import com.sk89q.commandbook.util.ChatUtil;
import com.sk89q.commandbook.util.InputUtil;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.concurrent.TimeUnit;


/**
 *
 * @author Turtle9598
 */

@Depend(components = SessionComponent.class)
@ComponentInformation(friendlyName = "Freeze", desc = "Blocks a specified player's movement on command")
public class FreezeComponent extends BukkitComponent implements Listener, Runnable {
    public static final int MOVE_THRESHOLD = 2;
    private static final int MOVE_THRESHOLD_SQ = MOVE_THRESHOLD * MOVE_THRESHOLD;

    @InjectComponent private SessionComponent sessions;

    @Override
    public void enable() {
        registerCommands(Commands.class);
        CommandBook.registerEvents(this);
        CommandBook.server().getScheduler().scheduleSyncRepeatingTask(CommandBook.inst(), this, 20 * 2, 20 * 2);
    }

    public boolean freezePlayer(Player player) {
        FreezeState session = sessions.getSession(FreezeState.class, player);
        final boolean previous = session.isFrozen();
        session.freeze(player.getLocation());
        return previous;
    }

    public boolean isFrozen(Player player) {
        return sessions.getSession(FreezeState.class, player).isFrozen();
    }

    public boolean unfreezePlayer(Player player) {
        FreezeState session = sessions.getSession(FreezeState.class, player);
        final boolean previous = session.isFrozen();
        session.freeze(null);
        return previous;
    }

    @Override
    public void run() {
        for (FreezeState frozenState : sessions.getSessions(FreezeState.class).values()) {
            if (!frozenState.isFrozen()) {
                continue;
            }

            Player player = frozenState.getOwner();
            if (player == null || !player.isOnline()) {
                continue;
            }

            Location loc = player.getLocation();
            if (loc.distanceSquared(frozenState.getFreezeLocation()) >= MOVE_THRESHOLD_SQ) {
                loc.setX(frozenState.getFreezeLocation().getX());
                loc.setY(frozenState.getFreezeLocation().getY());
                loc.setZ(frozenState.getFreezeLocation().getZ());
                player.sendMessage(ChatColor.RED + "You are frozen.");
                player.teleport(loc);
            }

        }
    }

    private static class FreezeState extends PersistentSession {
        public static final long MAX_AGE = TimeUnit.MINUTES.toMillis(30);

        private Location freezeLocation;

        protected FreezeState() {
            super(MAX_AGE);
        }

        public boolean isFrozen() {
            return freezeLocation != null;
        }

        public Location getFreezeLocation() {
            return freezeLocation;
        }

        public void freeze(Location loc) {
            freezeLocation = loc == null ? null : loc.clone();
        }

        public Player getOwner() {
            CommandSender sender = super.getOwner();
            return sender instanceof Player ? (Player) sender : null;
        }
    }

    public class Commands {
        @Command(aliases = {"freeze"}, usage = "<target>", desc = "Freeze a player", min = 1, max = 1)
        @CommandPermissions({"commandbook.freeze"})
        public void freeze(CommandContext args, CommandSender sender) throws CommandException {
            Player player = InputUtil.PlayerParser.matchSinglePlayer(sender, args.getString(0));

            if (!freezePlayer(player)) {
                player.sendMessage(ChatColor.YELLOW + "You've been frozen by "
                        + ChatUtil.toColoredName(sender, ChatColor.YELLOW));
                sender.sendMessage(ChatColor.YELLOW + "You've frozen "
                        + ChatUtil.toColoredName(player, ChatColor.YELLOW));
            } else {
                player.sendMessage(ChatColor.YELLOW + "Your freeze location has been updated by "
                        + ChatUtil.toColoredName(sender, ChatColor.YELLOW));
                sender.sendMessage(ChatColor.YELLOW + "You have updated the freeze location of "
                        + ChatUtil.toColoredName(player, ChatColor.YELLOW));
            }
        }

        @Command(aliases = {"unfreeze"}, usage = "<target>", desc = "Unfreeze a player", min = 1, max = 1)
        @CommandPermissions({"commandbook.freeze"})
        public void unfreeze(CommandContext args, CommandSender sender) throws CommandException {
            Player player = InputUtil.PlayerParser.matchSinglePlayer(sender, args.getString(0));

            if (unfreezePlayer(player)) {

                player.sendMessage(ChatColor.YELLOW + "You've been unfrozen by "
                        + ChatUtil.toColoredName(sender, ChatColor.YELLOW));
                sender.sendMessage(ChatColor.YELLOW + "You've unfrozen "
                        + ChatUtil.toColoredName(player, ChatColor.YELLOW));
            } else {
                throw new CommandException(ChatUtil.toName(player) + " was not frozen");
            }
        }
    }
}
