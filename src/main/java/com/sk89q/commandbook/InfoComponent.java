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

import com.sk89q.commandbook.bans.BansComponent;
import com.sk89q.commandbook.commands.PaginatedResult;
import com.sk89q.commandbook.components.AbstractComponent;
import com.sk89q.commandbook.components.ComponentInformation;
import com.sk89q.commandbook.components.InjectComponent;
import com.sk89q.commandbook.util.LocationUtil;
import com.sk89q.commandbook.util.PlayerUtil;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import static com.sk89q.commandbook.CommandBookUtil.getCardinalDirection;

@ComponentInformation(friendlyName = "Info", desc = "Info contains commands that allow users to gather " +
        "information about the world, without being able to make changes.")
public class InfoComponent extends AbstractComponent {
    
    @InjectComponent BansComponent bans;
    
    @InjectComponent GodComponent god;

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
                player = PlayerUtil.matchSinglePlayer(sender, args.getString(0));
                if (player != sender) {
                    CommandBook.inst().checkPermission(sender, "commandbook.whereami.other");
                }
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

        private final SimpleDateFormat dateFormat =
                new SimpleDateFormat("dd/MM/yyyy HH:mm");
        
        @Command(aliases = {"whois"},
                usage = "[-p page] [player]", desc = "Tell information about a player",
                flags = "op:", min = 0, max = 1)
        @CommandPermissions({"commandbook.whois"})
        public void whois(CommandContext args, CommandSender sender) throws CommandException {

            OfflinePlayer offline;

            if (args.argsLength() == 0) {
                offline = PlayerUtil.checkPlayer(sender);
            } else {
                try {
                    offline = PlayerUtil.matchSinglePlayer(sender, args.getString(0));
                } catch (CommandException e) {
                    if (args.hasFlag('o')) {
                        offline = CommandBook.server().getOfflinePlayer(args.getString(0));
                    } else {
                        throw e;
                    }
                }
                
                if (offline != sender) {
                    CommandBook.inst().checkPermission(sender, "commandbook.whois.other");
                }
            }
            
            List<String> results = new ArrayList<String>();

            if (offline instanceof Player) {
                Player player = (Player) offline;
                results.add("Display name: " + player.getDisplayName());
                results.add("Entity ID #: " + player.getEntityId());
                results.add("Current vehicle: " + player.getVehicle());
                if (god != null && CommandBook.inst().hasPermission(sender, "commandbook.god.check")) {
                    results.add("Player " + (god.hasGodMode(player) ? "has" : "does not have") + " god mode");
                }

                if (CommandBook.inst().hasPermission(sender, "commandbook.ip-address")) {
                    results.add("Address: " + player.getAddress().toString());
                }
            }
            
            Location bedSpawn = offline.getBedSpawnLocation();
            if (bedSpawn != null) {
                results.add("Bed spawn location:" + 
                        LocationUtil.toFriendlyString(bedSpawn));
            } else {
                results.add("No bed spawn location");
            }
            
            if (offline.hasPlayedBefore()) {
                results.add("First joined: " + dateFormat.format(offline.getFirstPlayed()) 
                    + "; Last joined: " + dateFormat.format(offline.getLastPlayed()));
            }
            
            if (bans != null && CommandBook.inst().hasPermission(sender, "commandbook.bans.isbanned")) {
                results.add("Player " + 
                        (bans.getBanDatabase().isBannedName(offline.getName()) ? "is" 
                        : "is not") + " banned.");
            }
            
            new PaginatedResult<String>("Name: " + offline.getName()) {

                @Override
                public String format(String entry) {
                    return entry;
                }
            }.display(sender, results, args.getFlagInteger('p', 1));
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
                player = PlayerUtil.matchSinglePlayer(sender, args.getString(0));
                if (player != sender) {
                    CommandBook.inst().checkPermission(sender, "commandbook.whereami.compass.other");
                }
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
                player = PlayerUtil.matchSinglePlayer(sender, args.getString(0));
                if (player != sender) {
                    CommandBook.inst().checkPermission(sender, "commandbook.biome.other");
                }
            }

            sender.sendMessage(ChatColor.YELLOW + player.getLocation().getBlock().getBiome().name().toLowerCase().replace("_"," ")+" biome.");

        }
    }
}
