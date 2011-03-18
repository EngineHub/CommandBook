// $Id$
/*
 * Copyright (C) 2010, 2011 sk89q <http://www.sk89q.com>
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

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;

public class DebuggingCommands {

    @Command(aliases = {"testclock"},
            usage = "", desc = "Tests the clock rate of your server",
            flags = "", min = 0, max = 1)
    @CommandPermissions({"commandbook.debug.testclock"})
    public static void testClock(CommandContext args, CommandBookPlugin plugin,
            final CommandSender sender) throws CommandException {
        
        int expected = 5;
        
        if (args.argsLength() == 1) {
            expected = Math.min(30, Math.max(1, args.getInteger(0)));
        }

        sender.sendMessage(ChatColor.DARK_RED
                + "Timing clock test for " + expected + " IN-GAME seconds...");
        sender.sendMessage(ChatColor.DARK_RED
                + "DO NOT CHANGE A WORLD'S TIME OR PERFORM A HEAVY OPERATION.");
        
        final World world = plugin.getServer().getWorlds().get(0);
        final double expectedTime = expected * 1000;
        final double expectedSecs = expected;
        final int expectedTicks = 20 * (int)expectedSecs;
        final long start = System.currentTimeMillis();
        final long startTicks = world.getFullTime();
        
        Runnable task = new Runnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                long nowTicks = world.getFullTime();
                
                long elapsedTime = now - start;
                double elapsedSecs = elapsedTime / 1000.0;
                int elapsedTicks = (int) (nowTicks - startTicks);
                
                double error = (expectedTime - elapsedTime) / elapsedTime * 100;
                double clockRate = elapsedTicks / elapsedSecs;
                
                if (expectedTicks != elapsedTicks) {
                    sender.sendMessage(ChatColor.DARK_RED
                            + "Warning: Bukkit scheduler inaccurate; expected "
                            + expectedTicks + ", got " + elapsedTicks);
                }
                
                if (Math.round(clockRate) == 20) {
                    sender.sendMessage(ChatColor.YELLOW + "Clock test result: "
                            + ChatColor.GREEN + "EXCELLENT");
                } else {
                    if (elapsedSecs > expectedSecs) {
                        if (clockRate < 19) {
                            sender.sendMessage(ChatColor.YELLOW + "Clock test result: "
                                    + ChatColor.DARK_RED + "CLOCK BEHIND");
                            sender.sendMessage(ChatColor.DARK_RED
                                    + "WARNING: You have potential block respawn issues.");
                        } else {
                            sender.sendMessage(ChatColor.YELLOW + "Clock test result: "
                                    + ChatColor.DARK_RED + "CLOCK BEHIND");
                        }
                    } else {
                        sender.sendMessage(ChatColor.YELLOW + "Clock test result: "
                                + ChatColor.DARK_RED + "CLOCK AHEAD");
                    }
                }

                sender.sendMessage(ChatColor.GRAY + "Expected time elapsed: " + expectedTime + "ms");
                sender.sendMessage(ChatColor.GRAY + "Time elapsed: " + elapsedTime + "ms");
                sender.sendMessage(ChatColor.GRAY + "Error: " + error + "%");
                sender.sendMessage(ChatColor.GRAY + "Actual clock rate: " + clockRate + " ticks/sec");
                sender.sendMessage(ChatColor.GRAY + "Expected clock rate: 20 ticks/sec");
            }
        };
        
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, task, expectedTicks);
    }
    
    @Command(aliases = {"serverinfo"},
            usage = "", desc = "Get server information",
            flags = "", min = 0, max = 0)
    @CommandPermissions({"commandbook.debug.serverinfo"})
    public static void serverInfo(CommandContext args, CommandBookPlugin plugin,
            CommandSender sender) throws CommandException {
        
        Runtime rt = Runtime.getRuntime();

        sender.sendMessage(ChatColor.YELLOW
                + String.format("System: %s %s (%s)",
                        System.getProperty("os.name"),
                        System.getProperty("os.version"),
                        System.getProperty("os.arch")));
        sender.sendMessage(ChatColor.YELLOW
                + String.format("Java: %s %s (%s)",
                        System.getProperty("java.vendor"),
                        System.getProperty("java.version"),
                        System.getProperty("java.vendor.url")));
        sender.sendMessage(ChatColor.YELLOW
                + String.format("JVM: %s %s %s",
                        System.getProperty("java.vm.vendor"),
                        System.getProperty("java.vm.name"),
                        System.getProperty("java.vm.version")));
        
        sender.sendMessage(ChatColor.YELLOW + "Available processors: "
                + rt.availableProcessors());
        
        sender.sendMessage(ChatColor.YELLOW + "Available total memory: "
                + Math.floor(rt.maxMemory() / 1024.0 / 1024.0) + " MB");
        
        sender.sendMessage(ChatColor.YELLOW + "JVM allocated memory: "
                + Math.floor(rt.totalMemory() / 1024.0 / 1024.0) + " MB");
        
        sender.sendMessage(ChatColor.YELLOW + "Free allocated memory: "
                + Math.floor(rt.freeMemory() / 1024.0 / 1024.0) + " MB");
    }
}
