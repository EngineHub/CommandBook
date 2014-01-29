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

package com.sk89q.commandbook.time;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.util.ChatUtil;
import com.sk89q.commandbook.util.InputUtil;
import com.sk89q.commandbook.util.entity.player.PlayerUtil;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * TimeComponent contains commands and functions related to time management. These include
 */
@ComponentInformation(friendlyName = "Time Control", desc = "Commands to manage and lock time for a world.")
public class TimeComponent extends BukkitComponent implements Listener {

    /**
     * A Map of time locker tasks for worlds.
     */
    protected final Map<String, Integer> tasks = new HashMap<String, Integer>();

    /**
     * A Map of world names to time values.
     */
    protected final Map<String, Integer> lockedTimes = new HashMap<String, Integer>();

    /**
     * This Component's configuration
     */
    protected LocalConfiguration config;

    @Override
    public void enable() {
        config = configure(new LocalConfiguration());
        if (config.timeLockDelay == 0) {
            config.timeLockDelay = 20;
            saveConfig(config);
        }
        configureWorldLocks();
        registerCommands(Commands.class);
        CommandBook.registerEvents(this);
    }

    private void configureWorldLocks() {
        if (config.timeLocks != null) {
            for (Map.Entry<String, Object> entry : config.timeLocks.entrySet()) {
                int time = 0;

                try {
                    time = InputUtil.TimeParser.matchMCWorldTime(String.valueOf(entry.getValue()));
                } catch (CommandException e) {
                    CommandBook.logger().warning("Time lock: Failed to parse time '"
                            + entry.getValue() + "'");
                    continue;
                }

                lockedTimes.put(entry.getKey(), time);

                World world = CommandBook.server().getWorld(entry.getKey());

                if (world == null) {
                    CommandBook.logger().info("Could not time-lock unknown world '"
                            + entry.getKey() + "'");
                    continue;
                }

                world.setTime(time);
                lock(world);
                CommandBook.logger().info("Time locked to '"
                        + ChatUtil.getTimeString(time) + "' for world '"
                        + world.getName() + "'");
            }
        }
    }

    @Override
    public void reload() {
        super.reload();
        configureWorldLocks();
    }

    private static class LocalConfiguration extends ConfigurationBase {
        @Setting("time-lock") public Map<String, Object> timeLocks;
        @Setting("time-lock-delay") public int timeLockDelay = 20;
    }

    /**
     * Called when a World is loaded.
     */
    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        World world = event.getWorld();
        Integer lockedTime = getLockedTimes().get(world.getName());

        if (lockedTime != null) {
            world.setTime(lockedTime);
            lock(world);
            CommandBook.logger().info("Time locked to '"
                    + ChatUtil.getTimeString(lockedTime) + "' for world '"
                    + world.getName() + "'");
        }
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent event) {
        unlock(event.getWorld());
    }

    /**
     * Get locked times.
     *
     * @return
     */
    public Map<String, Integer> getLockedTimes() {
        return lockedTimes;
    }

    public synchronized void unlock(World world) {
        Integer id = tasks.get(world.getName());
        if (id != null) {
            CommandBook.server().getScheduler().cancelTask(id);
        }
    }

    public synchronized void lock(World world) {
        long time = world.getFullTime();
        unlock(world);
        int id = CommandBook.server().getScheduler().scheduleSyncRepeatingTask(
                CommandBook.inst(), new TimeLocker(world, time), 20, config.timeLockDelay);
        tasks.put(world.getName(), id);
    }

    /**
     * Parse a time string.
     *
     * @param timeStr
     * @return
     * @throws CommandException
     */
    @Deprecated
    public static int matchTime(String timeStr) throws CommandException {

        return InputUtil.TimeParser.matchMCWorldTime(timeStr);
    }

    public class Commands {
        @Command(aliases = {"time"},
                usage = "[world] <time|\"current\">", desc = "Get/change the world time",
                flags = "ls", min = 0, max = 2)
        public void time(CommandContext args, CommandSender sender) throws CommandException {

            World world;
            String timeStr;
            boolean onlyLock = false;

            // Easy way to get the time
            if (args.argsLength() == 0) {
                world = PlayerUtil.checkPlayer(sender).getWorld();
                timeStr = "current";
                // If no world was specified, get the world from the sender, but
                // fail if the sender isn't player
            } else if (args.argsLength() == 1) {
                world = PlayerUtil.checkPlayer(sender).getWorld();
                timeStr = args.getString(0);
            } else { // A world was specified!
                world = InputUtil.matchWorld(sender, args.getString(0));
                timeStr = args.getString(1);
            }

            boolean broadcastChanges = CommandBook.inst().broadcastChanges;

            if (broadcastChanges && args.hasFlag('s')) {
                CommandBook.inst().checkPermission(sender, "commandbook.time.silent");
                broadcastChanges = false;
            }

            // Let the player get the time
            if (timeStr.equalsIgnoreCase("current")
                    || timeStr.equalsIgnoreCase("cur")
                    || timeStr.equalsIgnoreCase("now")) {

                // We want to lock to the current time
                if (!args.hasFlag('l')) {
                    CommandBook.inst().checkPermission(sender, "commandbook.time.check");
                    sender.sendMessage(ChatColor.YELLOW
                            + "Time: " + ChatUtil.getTimeString(world.getTime()));
                    return;
                }

                onlyLock = true;
            }

            CommandBook.inst().checkPermission(sender, "commandbook.time");

            if (!onlyLock) {
                unlock(world);
                world.setTime(InputUtil.TimeParser.matchMCWorldTime(timeStr));
            }

            String verb = "set";

            // Locking
            if (args.hasFlag('l')) {
                CommandBook.inst().checkPermission(sender, "commandbook.time.lock");
                lock(world);
                verb = "locked";
            }

            if (broadcastChanges) {
                CommandBook.server().broadcastMessage(ChatColor.YELLOW
                        + ChatUtil.toColoredName(sender, ChatColor.YELLOW) + " " + verb + " the time of '"
                        + world.getName() + "' to "
                        + ChatUtil.getTimeString(world.getTime()) + ".");
            } else {
                sender.sendMessage(ChatColor.YELLOW + "Time " + verb + " to "
                        + ChatUtil.getTimeString(world.getTime()) + ".");
            }
        }

        @Command(aliases = {"playertime"},
                usage = "[filter] <time|\"current\">", desc = "Get/change a player's time",
                flags = "rsw", min = 0, max = 2)
        public void playertime(CommandContext args, CommandSender sender) throws CommandException {
            Iterable<Player> players = null;
            String timeStr = "current";
            boolean included = false;
            boolean reset = args.hasFlag('r');

            if (args.argsLength() < 2) {
                if (args.argsLength() == 1) {
                    timeStr = args.getString(0);
                    if (reset) {
                        players = InputUtil.PlayerParser.matchPlayers(sender, timeStr);
                    }
                }

                if (players == null) {
                    players = InputUtil.PlayerParser.matchPlayers(PlayerUtil.checkPlayer(sender));
                }
            } else {
                players = InputUtil.PlayerParser.matchPlayers(sender, args.getString(0));
                timeStr = args.getString(1);
            }

            for (Player player : players) {
                if (player != sender ) {
                    CommandBook.inst().checkPermission(sender, "commandbook.time.player.other");
                    break;
                } else {
                    CommandBook.inst().checkPermission(sender, "commandbook.time.player");
                }
            }

            if (args.hasFlag('r')) {
                for (Player player : players) {
                    player.resetPlayerTime();
                    if (!args.hasFlag('s')) {
                        player.sendMessage(ChatColor.YELLOW + "Your time was reset to world time");
                    }
                    if (sender instanceof Player && sender.equals(player)) {
                        included = true;
                    }
                }
                if (!included) {
                    sender.sendMessage(ChatColor.YELLOW + "Player times reset");
                }
                return;
            }

            if (timeStr.equalsIgnoreCase("current")
                    || timeStr.equalsIgnoreCase("cur")
                    || timeStr.equalsIgnoreCase("now")) {
                CommandBook.inst().checkPermission(sender, "commandbook.time.player.check");
                sender.sendMessage(ChatColor.YELLOW
                        + "Player Time: " + ChatUtil.getTimeString(InputUtil.PlayerParser.matchSinglePlayer(sender,
                        args.getString(0, PlayerUtil.checkPlayer(sender).getName())).getPlayerTime()));
                return;
            }

            int time = InputUtil.TimeParser.matchMCWorldTime(timeStr);

            for (Player player : players) {
                player.sendMessage(ChatColor.YELLOW + "Your time set to " + ChatUtil.getTimeString(player.getPlayerTime()));
                if (player.equals(sender)) {
                    included = true;
                }
                player.setPlayerTime(args.hasFlag('w') ? Integer.parseInt(timeStr) : time, args.hasFlag('w'));
            }
            if (!included) {
                sender.sendMessage(ChatColor.YELLOW + "Player times set to " + ChatUtil.getTimeString(time));
            }
        }
    }
}
