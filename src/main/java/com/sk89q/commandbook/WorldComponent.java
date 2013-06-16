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

import com.sk89q.commandbook.util.LocationUtil;
import com.sk89q.commandbook.util.PlayerUtil;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

@ComponentInformation(friendlyName = "World Tools", desc = "Various world-related commands.")
public class WorldComponent extends BukkitComponent {
    @Override
    public void enable() {
        registerCommands(Commands.class);
    }

    public class Commands {
        @Command(aliases = {"weather"},
                usage = "<'stormy'|'sunny'> [duration] [world]", desc = "Change the world weather",
                min = 1, max = 3)
        @CommandPermissions({"commandbook.weather"})
        public void weather(CommandContext args, CommandSender sender) throws CommandException {

            World world;
            String weatherStr = args.getString(0);
            int duration = -1;

            if (args.argsLength() == 1) {
                world = PlayerUtil.checkPlayer(sender).getWorld();
            } else if (args.argsLength() == 2) {
                world = PlayerUtil.checkPlayer(sender).getWorld();
                duration = args.getInteger(1);
            } else { // A world was specified!
                world = LocationUtil.matchWorld(sender, args.getString(2));
                duration = args.getInteger(1);
            }

            if (weatherStr.equalsIgnoreCase("stormy")
                    || weatherStr.equalsIgnoreCase("rainy")
                    || weatherStr.equalsIgnoreCase("snowy")
                    || weatherStr.equalsIgnoreCase("rain")
                    || weatherStr.equalsIgnoreCase("snow")
                    || weatherStr.equalsIgnoreCase("storm")
                    || weatherStr.equalsIgnoreCase("on")) {

                world.setStorm(true);

                if (duration > 0) {
                    world.setWeatherDuration(duration * 20);
                }

                if (CommandBook.inst().broadcastChanges) {
                    CommandBook.server().broadcastMessage(ChatColor.YELLOW
                            + PlayerUtil.toColoredName(sender, ChatColor.YELLOW) + " has started on a storm on '"
                            + world.getName() + "'.");
                }

                // Tell console, since console won't get the broadcast message.
                if (!CommandBook.inst().broadcastChanges) {
                    sender.sendMessage(ChatColor.YELLOW + "Stormy weather enabled.");
                }

            } else if (weatherStr.equalsIgnoreCase("clear")
                    || weatherStr.equalsIgnoreCase("sunny")
                    || weatherStr.equalsIgnoreCase("sun")
                    || weatherStr.equalsIgnoreCase("snowy")
                    || weatherStr.equalsIgnoreCase("rain")
                    || weatherStr.equalsIgnoreCase("snow")
                    || weatherStr.equalsIgnoreCase("off")) {

                world.setStorm(false);

                if (duration > 0) {
                    world.setWeatherDuration(duration * 20);
                }

                if (CommandBook.inst().broadcastChanges) {
                    CommandBook.server().broadcastMessage(ChatColor.YELLOW
                            + PlayerUtil.toColoredName(sender, ChatColor.YELLOW) + " has stopped a storm on '"
                            + world.getName() + "'.");
                }

                // Tell console, since console won't get the broadcast message.
                if (!CommandBook.inst().broadcastChanges) {
                    sender.sendMessage(ChatColor.YELLOW + "Stormy weather disabled.");
                }

            } else {
                throw new CommandException("Unknown weather state! Acceptable states: sunny or stormy");
            }
        }

        @Command(aliases = {"thunder"},
                usage = "<'on'|'off'> [duration] [world]", desc = "Change the thunder state",
                min = 1, max = 3)
        @CommandPermissions({"commandbook.weather.thunder"})
        public void thunder(CommandContext args, CommandSender sender) throws CommandException {

            World world;
            String weatherStr = args.getString(0);
            int duration = -1;

            if (args.argsLength() == 1) {
                world = PlayerUtil.checkPlayer(sender).getWorld();
            } else if (args.argsLength() == 2) {
                world = PlayerUtil.checkPlayer(sender).getWorld();
                duration = args.getInteger(1);
            } else { // A world was specified!
                world = LocationUtil.matchWorld(sender, args.getString(2));
                duration = args.getInteger(1);
            }

            if (weatherStr.equalsIgnoreCase("on")) {
                world.setThundering(true);

                if (duration > 0) {
                    world.setThunderDuration(duration * 20);
                }

                sender.sendMessage(ChatColor.YELLOW + "Thunder enabled.");
            } else if (weatherStr.equalsIgnoreCase("off")) {
                world.setThundering(false);

                if (duration > 0) {
                    world.setThunderDuration(duration * 20);
                }

                sender.sendMessage(ChatColor.YELLOW + "Thunder disabled.");
            } else {
                throw new CommandException("Unknown thunder state! Acceptable states: on or off");
            }
        }
    }
}
