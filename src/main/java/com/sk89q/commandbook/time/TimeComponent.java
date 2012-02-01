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
import com.sk89q.commandbook.CommandBookUtil;
import com.sk89q.commandbook.components.AbstractComponent;
import com.sk89q.commandbook.components.ComponentInformation;
import com.sk89q.commandbook.config.ConfigurationBase;
import com.sk89q.commandbook.config.Setting;
import com.sk89q.commandbook.util.LocationUtil;
import com.sk89q.commandbook.util.PlayerUtil;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TimeComponent contains commands and functions related to time management. These include
 */
@ComponentInformation(friendlyName = "Time Control", desc = "Commands to manage and lock time for a world.")
public class TimeComponent extends AbstractComponent implements Listener {

    /**
     * A pattern that matches time given in 12-hour form (xx:xx(am|pm))
     */
    protected static final Pattern TWELVE_HOUR_TIME = Pattern.compile("^([0-9]+(?::[0-9]+)?)([apmAPM\\.]+)$");

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
        configureWorldLocks();
        registerCommands(Commands.class);
        CommandBook.registerEvents(this);
    }

    private void configureWorldLocks() {
        config = configure(new LocalConfiguration());

        if (config.timeLocks != null) {
            for (Map.Entry<String, Object> entry : config.timeLocks.entrySet()) {
                int time = 0;

                try {
                    time = matchTime(String.valueOf(entry.getValue()));
                } catch (CommandException e) {
                    CommandBook.logger().warning("Time lock: Failed to parse time '"
                            + entry.getValue() + "'");
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
                        + CommandBookUtil.getTimeString(time) + "' for world '"
                        + world.getName() + "'");
            }
        }
    }

    @Override
    public void reload() {
        super.reload();
        disable();
        configureWorldLocks();
    }

    @Override
    public void disable() {
        saveConfig(config);
    }
    
    private static class LocalConfiguration extends ConfigurationBase {
        @Setting("time-lock") public Map<String, Object> timeLocks;
        @Setting("time-lock-delay") public int timeLockDelay;
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
                    + CommandBookUtil.getTimeString(lockedTime) + "' for world '"
                    + world.getName() + "'");
        }
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
    public int matchTime(String timeStr) throws CommandException {
        Matcher matcher;

        try {
            int time = Integer.parseInt(timeStr);

            // People tend to enter just a number of the hour
            if (time <= 24) {
                return ((time - 8) % 24) * 1000;
            }

            return time;
        } catch (NumberFormatException e) {
            // Not an integer!
        }

        // Tick time
        if (timeStr.matches("^*[0-9]+$")) {
            return Integer.parseInt(timeStr.substring(1));

            // Allow 24-hour time
        } else if (timeStr.matches("^[0-9]+:[0-9]+$")) {
            String[] parts = timeStr.split(":");
            int hours = Integer.parseInt(parts[0]);
            int mins = Integer.parseInt(parts[1]);
            return (int) (((hours - 8) % 24) * 1000
                    + Math.round((mins % 60) / 60.0 * 1000));

            // Or perhaps 12-hour time
        } else if ((matcher = TWELVE_HOUR_TIME.matcher(timeStr)).matches()) {
            String time = matcher.group(1);
            String period = matcher.group(2);
            int shift;

            if (period.equalsIgnoreCase("am")
                    || period.equalsIgnoreCase("a.m.")) {
                shift = 0;
            } else if (period.equalsIgnoreCase("pm")
                    || period.equalsIgnoreCase("p.m.")) {
                shift = 12;
            } else {
                throw new CommandException("'am' or 'pm' expected, got '"
                        + period + "'.");
            }

            String[] parts = time.split(":");
            int hours = Integer.parseInt(parts[0]);
            int mins = parts.length >= 2 ? Integer.parseInt(parts[1]) : 0;
            return (int) ((((hours % 12) + shift - 8) % 24) * 1000
                    + (mins % 60) / 60.0 * 1000);

            // Or some shortcuts
        } else if (timeStr.equalsIgnoreCase("dawn")) {
            return (6 - 8 + 24) * 1000;
        } else if (timeStr.equalsIgnoreCase("sunrise")) {
            return (7 - 8 + 24) * 1000;
        } else if (timeStr.equalsIgnoreCase("morning")) {
            return (24) * 1000;
        } else if (timeStr.equalsIgnoreCase("day")) {
            return (24) * 1000;
        } else if (timeStr.equalsIgnoreCase("midday")
                || timeStr.equalsIgnoreCase("noon")) {
            return (12 - 8 + 24) * 1000;
        } else if (timeStr.equalsIgnoreCase("afternoon")) {
            return (14 - 8 + 24) * 1000;
        } else if (timeStr.equalsIgnoreCase("evening")) {
            return (16 - 8 + 24) * 1000;
        } else if (timeStr.equalsIgnoreCase("sunset")) {
            return (21 - 8 + 24) * 1000;
        } else if (timeStr.equalsIgnoreCase("dusk")) {
            return (21 - 8 + 24) * 1000 + (int) (30 / 60.0 * 1000);
        } else if (timeStr.equalsIgnoreCase("night")) {
            return (22 - 8 + 24) * 1000;
        } else if (timeStr.equalsIgnoreCase("midnight")) {
            return (0 - 8 + 24) * 1000;
        }

        throw new CommandException("Time input format unknown.");
    }

    public class Commands {
        @Command(aliases = {"time"},
                usage = "[world] <time|\"current\">", desc = "Get/change the world time",
                flags = "l", min = 0, max = 2)
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
                world = LocationUtil.matchWorld(sender, args.getString(0));
                timeStr = args.getString(1);
            }

            // Let the player get the time
            if (timeStr.equalsIgnoreCase("current")
                    || timeStr.equalsIgnoreCase("cur")
                    || timeStr.equalsIgnoreCase("now")) {

                // We want to lock to the current time
                if (!args.hasFlag('l')) {
                    CommandBook.inst().checkPermission(sender, "commandbook.time.check");
                    sender.sendMessage(ChatColor.YELLOW
                            + "Time: " + CommandBookUtil.getTimeString(world.getTime()));
                    return;
                }

                onlyLock = true;
            }

            CommandBook.inst().checkPermission(sender, "commandbook.time");

            if (!onlyLock) {
                unlock(world);
                world.setTime(matchTime(timeStr));
            }

            String verb = "set";

            // Locking
            if (args.hasFlag('l')) {
                CommandBook.inst().checkPermission(sender, "commandbook.time.lock");
                lock(world);
                verb = "locked";
            }

            if (CommandBook.inst().broadcastChanges) {
                CommandBook.server().broadcastMessage(ChatColor.YELLOW
                        + PlayerUtil.toName(sender) + " " + verb + " the time of '"
                        + world.getName() + "' to "
                        + CommandBookUtil.getTimeString(world.getTime()) + ".");
            } else {
                sender.sendMessage(ChatColor.YELLOW + "Time " + verb + " to "
                        + CommandBookUtil.getTimeString(world.getTime()) + ".");
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
                    if (!reset) {
                        players = PlayerUtil.matchPlayers(sender, timeStr);
                    } else {
                        timeStr = args.getString(0);
                    }
                }
                if (players == null)
                players = PlayerUtil.matchPlayers(PlayerUtil.checkPlayer(sender));
            } else {
                players = PlayerUtil.matchPlayers(sender, args.getString(0));
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
                        + "Player Time: " + CommandBookUtil.getTimeString(PlayerUtil.matchSinglePlayer(sender,
                        args.getString(0, PlayerUtil.checkPlayer(sender).getName())).getPlayerTime()));
                return;
            }

            for (Player player : players) {
                if (!player.equals(sender)) {
                    player.sendMessage(ChatColor.YELLOW + "Your time set to " + CommandBookUtil.getTimeString(player.getPlayerTime()));
                } else {
                    player.sendMessage(ChatColor.YELLOW + "Your time set to " + CommandBookUtil.getTimeString(player.getPlayerTime()));
                    included = true;
                }
                player.setPlayerTime(args.hasFlag('w') ? Integer.parseInt(timeStr) : matchTime(timeStr), args.hasFlag('w'));
            }
            if (!included) {
                sender.sendMessage(ChatColor.YELLOW + "Player times set to " + CommandBookUtil.getTimeString(matchTime(timeStr)));
            }
        }
    }
}
