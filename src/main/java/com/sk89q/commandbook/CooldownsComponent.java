/*
 * CommandBook
 * Copyright (C) 2012 sk89q <http://www.sk89q.com>
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

import com.sk89q.commandbook.commands.PaginatedResult;
import com.sk89q.commandbook.session.PersistentSession;
import com.sk89q.commandbook.session.SessionComponent;
import com.sk89q.commandbook.util.InputUtil;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.NestedCommand;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

import static com.sk89q.commandbook.util.NestUtil.getNestedMap;

/**
 * This component provides command warmups and cooldowns, measured in seconds, by using
 * a repeating scheduler task that increases the value for each entry in each CooldownState
 * by one each second if the value is less than the number of seconds specified in the
 * configuration, removing the entry if the warmup/cooldown has been removed from the configuration
 */
@Depend(components = SessionComponent.class)
@ComponentInformation(friendlyName = "Warmups and Cooldowns", desc = "Allows warmups and cooldowns for commands, specified in seconds.")
public class CooldownsComponent extends BukkitComponent implements Listener, Runnable {
    @InjectComponent private SessionComponent sessions;
    private LocalConfiguration config;
    private static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);

    @Override
    public void enable() {
        config = configure(new LocalConfiguration());
        CommandBook.registerEvents(this);
        registerCommands(Commands.class);
        scheduler.scheduleAtFixedRate(this, 1, 1, TimeUnit.SECONDS);
    }

    @Override
    public void reload() {
        super.reload();
        configure(config);
    }

    @Override
    public void disable() {
        scheduler.shutdown();
    }

    private static String firstWord(final String str) {
        int spaceIndex = str.indexOf(" ");
        if (spaceIndex == -1) {
            return str;
        }
        return str.substring(0, spaceIndex);
    }

    private static String titleCase(String input) {
        StringBuilder ret = new StringBuilder();
        for (String word : input.split(" ")) {
            if (ret.length() > 0) {
                ret.append(" ");
            }
            if (ret.length() == 0 || word.length() > 2) {
                ret.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
            } else {
                ret.append(word);
            }
        }
        return ret.toString();
    }

    private static final SimpleDateFormat timeFormat = new SimpleDateFormat("mm:ss");
    private static String formatTime(int timeInSeconds, TimeUnit unit) {
        synchronized (timeFormat) {
            return timeFormat.format(new Date(unit.toMillis(timeInSeconds)));
        }
    }

    public void run() {
        for (final CooldownState state : sessions.getSessions(CooldownState.class).values()) {
            final HashSet<String> visitedCooldowns = new HashSet<String>();
            for (Iterator<Map.Entry<String, Integer>> i = state.cooldownCommands.entrySet().iterator(); i.hasNext();) {
                final Map.Entry<String, Integer> entry = i.next();
                final Integer cooldownTime = getNestedMap(config.registeredActions, entry.getKey()).get("cooldown");
                if (cooldownTime == null) {
                    i.remove(); // The cooldown has been removed, so we can get rid of it.
                    continue;
                }
                if (entry.getValue() <= cooldownTime) { // Increment the time if it isn't already at the required time
                    entry.setValue(entry.getValue() + 1);
                }
                visitedCooldowns.add(entry.getKey());
            }

            for (Iterator<Map.Entry<String, WarmupInfo>> i = state.warmupCommands.entrySet().iterator(); i.hasNext();) {
                final Map.Entry<String, WarmupInfo> entry = i.next();
                final Integer warmupTime = getNestedMap(config.registeredActions, entry.getKey()).get("warmup");
                if (warmupTime == null) {
                    i.remove(); // The warmup has been removed, so we can get rid of it.
                    continue;
                } else if (visitedCooldowns.contains(entry.getKey())) {
                    continue;
                }

                if (entry.getValue().remainingTime < warmupTime) {
                    entry.getValue().remainingTime++;
                } else if (entry.getValue().remainingTime == warmupTime) {
                    // Reached the needed time, run a scheduler task to execute the command
                    // back on the main thread.
                    final CommandSender owner = state.getOwner();
                    if (owner != null) {
                        CommandBook.server().getScheduler().callSyncMethod(CommandBook.inst(), new Callable<Boolean>() {
                            @Override
                            public Boolean call() {
                                return CommandBook.server().dispatchCommand(owner, entry.getValue().fullCommand);
                            }
                        });
                    }
                    i.remove();
                }
            }
        }
    }

    private static class LocalConfiguration extends ConfigurationBase {
        @Setting("commands"/*, help = "A mapping of commands to their cooldown values." +
                "For each command there are both warmup and cooldown values, with numbers " +
                "measured in seconds"*/)
        public Map<String, Map<String, Integer>> registeredActions = createDefaultStructure();

        public Map<String, Map<String, Integer>> createDefaultStructure() {
            Map<String, Map<String, Integer>> result = new HashMap<String, Map<String, Integer>>();
            getNestedMap(result, "command-with-warmup").put("warmup", 50);
            getNestedMap(result, "command-with-cooldown").put("cooldown", 50);
            getNestedMap(result, "command-with-warmup-and-cooldown").put("cooldown", 50);
            getNestedMap(result, "command-with-warmup-and-cooldown").put("warmup", 50);
            return result;
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void playerHandler(PlayerCommandPreprocessEvent event) {
        if (!checkCooldown(event.getPlayer(), event.getMessage().substring(1))
                || !checkWarmup(event.getPlayer(), event.getMessage().substring(1))) {
            event.setCancelled(true);
        }
    }

    /*@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true) // TODO: Make ServerCommandEvent Cancellable
    public void serverCommandHandler(ServerCommandEvent event) {
        if (!checkCooldown(event.getSender(), event.getCommand())
                || !checkWarmup(event.getSender(), event.getCommand())) {
            event.setCancelled(true);
        }
    }*/

    public boolean checkCooldown(CommandSender sender, String command) {
        CooldownState state = sessions.getSession(CooldownState.class, sender);
        command = firstWord(command);
        synchronized (state.cooldownCommands) {
            Map<String, Integer> storedTimes = config.registeredActions.get(command);
            if (storedTimes == null) { // Nothing in the config for this command
                return true;
            }

            Integer requiredCooldownTime = storedTimes.get("cooldown");
            if (requiredCooldownTime == null) { // No cooldown for this command
                return true;
            }

            Integer passedCooldownTime = state.cooldownCommands.get(command);
            if (passedCooldownTime == null) { // There isn't an in-progress cooldown for this command
                passedCooldownTime = 0;
                state.cooldownCommands.put(command, passedCooldownTime);
            }

            if (passedCooldownTime >= requiredCooldownTime
                    || CommandBook.inst().hasPermission(sender, "commandbook.cooldown.override." + command)) {
                state.cooldownCommands.remove(command);
                return true;
            } else {
                sender.sendMessage(ChatColor.YELLOW + "The command '" + command +
                        "' has a remaining cooldown of " +
                        formatTime(requiredCooldownTime - passedCooldownTime, TimeUnit.SECONDS)
                        + " seconds.");
                return false;
            }
        }
    }

    public boolean checkWarmup(CommandSender sender, String command) {
        CooldownState state = sessions.getSession(CooldownState.class, sender);
        synchronized (state.warmupCommands) {
            Map<String, Integer> storedTimes = config.registeredActions.get(firstWord(command));
            if (storedTimes == null) { // Nothing in the config for this command
                return true;
            }

            Integer requiredWarmupTime = storedTimes.get("warmup");
            if (requiredWarmupTime == null || CommandBook.inst().hasPermission(sender,
                    "commandbook.warmup.override." + firstWord(command))) { // No warmup for this command
                return true;
            }

            WarmupInfo warmupInfo = state.warmupCommands.get(firstWord(command));
            if (warmupInfo == null) { // There isn't an in-progress warmup for this command
                warmupInfo = new WarmupInfo(command);
                state.warmupCommands.put(command, warmupInfo);
                sender.sendMessage(ChatColor.YELLOW + "Warmup started for command '"
                        + firstWord(command) + "', time remaining: " +
                        formatTime(requiredWarmupTime, TimeUnit.SECONDS) + " seconds");
                return false;
            }

            if (!command.equals(warmupInfo.fullCommand)) {
                sender.sendMessage(ChatColor.RED + "You are trying to use the command '"
                        + command + "', which already has a warmup in progress. Type /warmup cancel "
                        + firstWord(command) + " to cancel the existing warmup" );
            }
            return false;
        }
    }

    private static class WarmupInfo {
        public final String fullCommand;
        public int remainingTime;

        public WarmupInfo(String fullCommand) {
            this.fullCommand = fullCommand;
        }
    }

    private static class CooldownState extends PersistentSession {
        public static final long MAX_AGE = TimeUnit.MINUTES.toMillis(30);

        public final Map<String, WarmupInfo> warmupCommands = new ConcurrentHashMap<String, WarmupInfo>();
        @Setting("cooldown-commands") public final Map<String, Integer> cooldownCommands = new ConcurrentHashMap<String, Integer>();

        protected CooldownState() {
            super(MAX_AGE);
        }
    }

    public class Commands {
        @Command(aliases = {"warmup", "warmups"}, desc = "Provides information about command warmups")
        @NestedCommand(WarmupCommands.class)
        public void warmup() {}

        @Command(aliases = {"cooldown", "cooldowns"}, desc = "Provides information about command cooldowns")
        @NestedCommand(CooldownCommands.class)
        public void cooldown() {}
    }

    public class CooldownCommands extends SubCommands<Map.Entry<String, Integer>> {

        @Command(aliases = {"list", "ls"}, desc = "List active command limitations", usage = "[-p page] [player]", flags = "p:", min = 0, max = 1)
        public void list(CommandContext args, CommandSender sender) throws CommandException {
            CommandSender target;
            if (args.argsLength() == 0) {
                target = sender;
            } else {
                target = InputUtil.PlayerParser.matchPlayerOrConsole(sender, args.getString(0));
            }
            getListOutput().display(sender, getActive(target), args.getFlagInteger('p', 1));
        }

        @Command(aliases = {"cancel", "c"}, desc = "Cancel a command limitation", usage = "<cmd>", min = 1)
        public void cancel(CommandContext args, CommandSender sender) throws CommandException {
            String item = args.getJoinedStrings(0);
            if (remove(sender, item)) {
                sender.sendMessage(ChatColor.YELLOW + titleCase(getTypeName()) +
                        " for command '" + item + "' removed.");
            } else {
                throw new CommandException("No " + getTypeName() + " for input " + item);
            }
        }

        @Override
        public String getTypeName() {
            return "cooldown";
        }

        @Override
        public PaginatedResult<Map.Entry<String, Integer>> getListOutput() {
            return new PaginatedResult<Map.Entry<String, Integer>>("Command - Time remaining") {

                @Override
                public String format(Map.Entry<String, Integer> entry) {
                    Map<String, Integer> storedTimes = config.registeredActions.get(entry.getKey());
                    if (storedTimes == null) { // Nothing in the config for this command
                        return "Invalid cooldown: " + entry.getKey();
                    }

                    Integer requiredCooldownTime = storedTimes.get("cooldown");
                    if (requiredCooldownTime == null) { // No cooldown for this command
                        return "Invalid cooldown: " + entry.getKey();
                    }
                    return entry.getKey() + " - " +
                            formatTime(requiredCooldownTime - entry.getValue(), TimeUnit.SECONDS);
                }
            };
        }

        @Override
        public Collection<Map.Entry<String, Integer>> getActive(CommandSender sender) {
            return sessions.getSession(CooldownState.class, sender).cooldownCommands.entrySet();
        }

        @Override
        public boolean remove(CommandSender sender, String name) {
            return sessions.getSession(CooldownState.class, sender).cooldownCommands.remove(name.toLowerCase()) != null;
        }
    }

    public class WarmupCommands extends SubCommands<WarmupInfo> {

        @Command(aliases = {"list", "ls"}, desc = "List active command limitations", usage = "[-p page] [player]", flags = "p:", min = 0, max = 1)
        public void list(CommandContext args, CommandSender sender) throws CommandException {
            CommandSender target;
            if (args.argsLength() == 0) {
                target = sender;
            } else {
                target = InputUtil.PlayerParser.matchPlayerOrConsole(sender, args.getString(0));
            }
            getListOutput().display(sender, getActive(target), args.getFlagInteger('p', 1));
        }

        @Command(aliases = {"cancel", "c"}, desc = "Cancel a command limitation", usage = "<cmd>", min = 1)
        public void cancel(CommandContext args, CommandSender sender) throws CommandException {
            String item = args.getJoinedStrings(0);
            if (remove(sender, item)) {
                sender.sendMessage(ChatColor.YELLOW + titleCase(getTypeName()) +
                        " for command '" + item + "' removed.");
            } else {
                throw new CommandException("No " + getTypeName() + " for input " + item);
            }
        }

        @Override
        public String getTypeName() {
            return "warmup";
        }

        @Override
        public PaginatedResult<WarmupInfo> getListOutput() {
            return new PaginatedResult<WarmupInfo>("Command - Remaining time") {
                @Override
                public String format(WarmupInfo entry) {
                    Map<String, Integer> storedTimes = config.registeredActions
                            .get(firstWord(entry.fullCommand));
                    if (storedTimes == null) { // Nothing in the config for this command
                        return "Invalid warmup: " + entry.fullCommand;
                    }

                    Integer requiredWarmupTime = storedTimes.get("warmup");
                    if (requiredWarmupTime == null) { // No cooldown for this command
                        return "Invalid warmup: " + entry.fullCommand;
                    }
                    return "/" + entry.fullCommand + " - " +
                            formatTime(requiredWarmupTime - entry.remainingTime, TimeUnit.SECONDS);
                }
            };
        }

        @Override
        public Collection<WarmupInfo> getActive(CommandSender sender) {
            return sessions.getSession(CooldownState.class, sender).warmupCommands.values();
        }

        @Override
        public boolean remove(CommandSender sender, String name) {
            return sessions.getSession(CooldownState.class, sender).warmupCommands.remove(name.toLowerCase()) != null;
        }
    }

    private abstract class SubCommands<CommandType> {
        public abstract String getTypeName();

        public abstract PaginatedResult<CommandType> getListOutput();

        public abstract Collection<CommandType> getActive(CommandSender sender);

        public abstract boolean remove(CommandSender sender, String name);

        /*@Command(aliases = {"list", "ls"}, desc = "List active command limitations", usage = "[-p page] [player]", flags = "p:", min = 0, max = 1)
        public void list(CommandContext args, CommandSender sender) throws CommandException {
            CommandSender target;
            if (args.argsLength() == 0) {
                target = sender;
            } else {
                target = InputUtil.PlayerParser.matchPlayerOrConsole(sender, args.getString(0));
            }
            getListOutput().display(sender, getActive(target), args.getFlagInteger('p', 1));
        }

        @Command(aliases = {"cancel", "c"}, desc = "Cancel a command limitation", usage = "<cmd>", min = 1)
        public void cancel(CommandContext args, CommandSender sender) throws CommandException {
            String item = args.getJoinedStrings(0);
            if (remove(sender, item)) {
                sender.sendMessage(ChatColor.YELLOW + titleCase(getTypeName()) +
                        " for command '" + item + "' removed.");
            } else {
                throw new CommandException("No " + getTypeName() + " for input " + item);
            }
        }*/
    }
}
