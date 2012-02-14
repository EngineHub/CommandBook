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

import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.spout.SpoutComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import org.spout.api.ChatColor;
import org.spout.api.command.CommandSource;
import org.spout.api.event.EventHandler;
import org.spout.api.event.Listener;
import org.spout.api.event.Order;
import org.spout.api.event.server.PreCommandEvent;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.sk89q.commandbook.CommandBookUtil.getNestedMap;

/**
 * This component provides command warmups and cooldowns, measured in seconds, by using 
 * a repeating scheduler task that increases the value for each entry in each CooldownState 
 * by one each second if the value is less than the number of seconds specified in the 
 * configuration, removing the entry if the warmup/cooldown has been removed from the configuration
 */
@ComponentInformation(friendlyName = "Warmups and Cooldowns", desc = "Allows warmups and cooldowns for commands, specified in seconds.")
public class CooldownsComponent extends SpoutComponent implements Listener, Runnable {
    private LocalConfiguration config;
    private final Map<String, CooldownState> states = new ConcurrentHashMap<String, CooldownState>();
    
    @Override
    public void enable() {
        config = configure(new LocalConfiguration());
        CommandBook.game().getEventManager().registerEvents(this, this);
        CommandBook.game().getScheduler().scheduleAsyncRepeatingTask(CommandBook.inst(), this, 20L, 20L);
    }
    
    private static String firstWord(final String str) {
        int spaceIndex = str.indexOf(" ");
        if (spaceIndex == -1) {
            return str;
        }
        return str.substring(0, spaceIndex);
    }

    private static final SimpleDateFormat timeFormat = new SimpleDateFormat("MM:SS");
    private static String formatTime(int timeInSeconds) {
        return timeFormat.format(new Date(timeInSeconds * 1000));
    }
    
    public void run() {
        for (final CooldownState state : states.values()) {
            for (Iterator<Map.Entry<String, Integer>> i = state.cooldownCommands.entrySet().iterator(); i.hasNext();) {
                final Map.Entry<String, Integer> entry = i.next();
                final Integer cooldownTime = getNestedMap(config.registeredActions, entry.getKey()).get("cooldown");
                if (cooldownTime == null) {
                    i.remove(); // The cooldown has been removed, so we can get rid of it.
                    continue;
                }
                if (entry.getValue() < cooldownTime) { // Increment the time if it isn't already at the required time
                    entry.setValue(entry.getValue() + 1);
                }
            }

            for (Iterator<Map.Entry<String, WarmupInfo>> i = state.warmupCommands.entrySet().iterator(); i.hasNext();) {
                final Map.Entry<String, WarmupInfo> entry = i.next();
                final Integer warmupTime = getNestedMap(config.registeredActions, entry.getKey()).get("warmup");
                if (warmupTime == null) {
                    i.remove(); // The warmup has been removed, so we can get rid of it.
                    continue;
                }
                if (entry.getValue().remainingTime < warmupTime) {
                    entry.getValue().remainingTime++;
                } else if (entry.getValue().remainingTime == warmupTime) { 
                    // Reached the needed time, run a scheduler task to execute the command 
                    // back on the main thread.
                    CommandBook.game().getScheduler().scheduleSyncDelayedTask(CommandBook.inst(), new Runnable() {
                        @Override
                        public void run() {
                            CommandBook.game().processCommand(state.sender, entry.getValue().fullCommand);
                        }
                    }, 0L);
                }
            }
        }
    }
    
    private class LocalConfiguration extends ConfigurationBase {
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

    @EventHandler(order = Order.EARLIEST)
    public void playerHandler(PreCommandEvent event) {
        if (!checkCooldown(event.getCommandSource(), event.getMessage()) 
                || !checkWarmup(event.getCommandSource(), event.getMessage())) {
            event.setCancelled(true);
        }
    }
    
    public boolean checkCooldown(CommandSource sender, String command) {
        CooldownState state = getState(sender);
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
                    || sender.hasPermission("commandbook.cooldown.override." + command)) {
                state.cooldownCommands.remove(command);
                return true;
            } else {
                sender.sendMessage(ChatColor.YELLOW + "The command '" + command + "' has a remaining cooldown of " + formatTime(requiredCooldownTime - passedCooldownTime) + " seconds.");
                return false;
            }
        }
    }
    
    public boolean checkWarmup(CommandSource sender, String command) {
        CooldownState state = getState(sender);
        synchronized (state.warmupCommands) {
            Map<String, Integer> storedTimes = config.registeredActions.get(firstWord(command));
            if (storedTimes == null) { // Nothing in the config for this command
                return true;
            }

            Integer requiredWarmupTime = storedTimes.get("warmup");
            if (requiredWarmupTime == null) { // No warmup for this command
                return true;
            }

            WarmupInfo warmupInfo = state.warmupCommands.get(firstWord(command));
            if (warmupInfo == null) { // There isn't an in-progress warmup for this command
                warmupInfo = new WarmupInfo(command);
                state.warmupCommands.put(command, warmupInfo);
                sender.sendMessage(ChatColor.YELLOW + "Warmup started for command '" 
                        + firstWord(command) + "', time remaining: " + 
                        formatTime(requiredWarmupTime) + " seconds");
                return false;
            }
            
            if (!command.equals(warmupInfo.fullCommand)) {
                sender.sendMessage(ChatColor.RED + "You are trying to use the command '" 
                        + command + "', which already has a warmup in progress. Type /warmup -c " 
                        + firstWord(command) + " to cancel the existing warmup" );
            }
            return false;
        }
    }
    
    public CooldownState getState(CommandSource sender) {
        CooldownState state = states.get(sender.getName());
        if (state == null) {
            state = new CooldownState();
            states.put(sender.getName(), state);
        }
        state.sender = sender;
        return state;
    }
    
    private static class WarmupInfo {
        public final String fullCommand;
        public int remainingTime;
        
        public WarmupInfo(String fullCommand) {
            this.fullCommand = fullCommand;
        }
    } 
    
    private static class CooldownState {
        public CommandSource sender;
        public final Map<String, WarmupInfo> warmupCommands = new ConcurrentHashMap<String, WarmupInfo>();
        public final Map<String, Integer> cooldownCommands = new ConcurrentHashMap<String, Integer>();
    }
}
