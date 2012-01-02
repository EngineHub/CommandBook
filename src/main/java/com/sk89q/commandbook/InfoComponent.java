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

import com.sk89q.commandbook.components.AbstractComponent;
import com.sk89q.commandbook.components.ComponentInformation;
import com.sk89q.commandbook.util.PlayerUtil;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static com.sk89q.commandbook.CommandBookUtil.getCardinalDirection;

@ComponentInformation(friendlyName = "Info", desc = "Info contains commands that allow users to gather " +
        "information about the world, without being able to make changes.")
public class InfoComponent extends AbstractComponent {

    @Override
    public void initialize() {
        registerCommands(Commands.class);
    }
    
    public class Commands {
        @Command(aliases = {"whereami", "getpos", "pos", "where"},
                usage = "[player]", desc = "Show your current location",
                flags = "", min = 0, max = 1)
        @CommandPermissions({"commandbook.whereami"})
        public void whereAmI(CommandContext args, CommandSender sender) throws CommandException {

            Player player;

            if (args.argsLength() == 0) {
                player = PlayerUtil.checkPlayer(sender);
            } else {
                CommandBook.inst().checkPermission(sender, "commandbook.whereami.other");

                player = PlayerUtil.matchSinglePlayer(sender, args.getString(0));
            }

            Location pos = player.getLocation();

            sender.sendMessage(ChatColor.YELLOW + "Player: " + player.getName() + (player == sender ? "(That's you!)" : ""));
            sender.sendMessage(ChatColor.YELLOW +
                    "World: " + player.getWorld().getName());
            sender.sendMessage(ChatColor.YELLOW +
                    String.format("Location: (%.4f, %.4f, %.4f)",
                            pos.getX(), pos.getY(), pos.getZ()));
            sender.sendMessage(ChatColor.YELLOW +
                    "Depth: " + (int) Math.floor(pos.getY()));

            if (CommandBook.inst().hasPermission(sender, "commandbook.whereami.compass")) {
                sender.sendMessage(ChatColor.YELLOW +
                        String.format("Direction: %s",
                                getCardinalDirection(player)));
            }
        }

        @Command(aliases = {"whois"},
                usage = "[player]", desc = "Tell information about a player",
                flags = "", min = 0, max = 1)
        @CommandPermissions({"commandbook.whois"})
        public void whois(CommandContext args, CommandSender sender) throws CommandException {

            Player player;

            if (args.argsLength() == 0) {
                player = PlayerUtil.checkPlayer(sender);
            } else {
                CommandBook.inst().checkPermission(sender, "commandbook.whois.other");

                player = PlayerUtil.matchSinglePlayer(sender, args.getString(0));
            }

            sender.sendMessage(ChatColor.YELLOW
                    + "Name: " + player.getName());
            sender.sendMessage(ChatColor.YELLOW
                    + "Display name: " + player.getDisplayName());
            sender.sendMessage(ChatColor.YELLOW
                    + "Entity ID #: " + player.getEntityId());
            sender.sendMessage(ChatColor.YELLOW
                    + "Current vehicle: " + player.getVehicle());

            if (CommandBook.inst().hasPermission(sender, "commandbook.ip-address")) {
                sender.sendMessage(ChatColor.YELLOW
                        + "Address: " + player.getAddress().toString());
            }
        }

        @Command(aliases = {"compass"},
                usage = "[player]", desc = "Show your current compass direction",
                flags = "", min = 0, max = 1)
        @CommandPermissions({"commandbook.whereami.compass"})
        public void compass(CommandContext args, CommandSender sender) throws CommandException {

            Player player;

            if (args.argsLength() == 0) {
                player = PlayerUtil.checkPlayer(sender);
            } else {
                CommandBook.inst().checkPermission(sender, "commandbook.whereami.compass.other");

                player = PlayerUtil.matchSinglePlayer(sender, args.getString(0));
            }

            sender.sendMessage(ChatColor.YELLOW +
                    String.format("Your direction: %s",
                            getCardinalDirection(player)));
        }

        @Command(aliases = {"biome"},
                usage = "[player]", desc = "Get your current biome",
                flags = "", min = 0, max = 1)
        @CommandPermissions({"commandbook.biome"})
        public void biome(CommandContext args, CommandSender sender) throws CommandException {

            Player player;

            if (args.argsLength() == 0) {
                player = PlayerUtil.checkPlayer(sender);
            } else {
                CommandBook.inst().checkPermission(sender, "commandbook.biome.other");

                player = PlayerUtil.matchSinglePlayer(sender, args.getString(0));
            }

            sender.sendMessage(ChatColor.YELLOW + player.getLocation().getBlock().getBiome().name().toLowerCase().replace("_"," ")+" biome.");

        }
    }
}
