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

import com.sk89q.commandbook.commands.PaginatedResult;
import com.sk89q.commandbook.util.ChatUtil;
import com.sk89q.commandbook.util.InputUtil;
import com.sk89q.commandbook.util.entity.player.PlayerUtil;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.text.SimpleDateFormat;
import java.util.*;

import static com.sk89q.commandbook.util.entity.EntityUtil.getCardinalDirection;

@ComponentInformation(friendlyName = "Info", desc = "Info contains commands that allow users to gather " +
        "information about the world, without being able to make changes.")
public class InfoComponent extends BukkitComponent {

    @Override
    public void enable() {
        registerCommands(Commands.class);
    }

    public static class PlayerWhoisEvent extends Event {
        private final OfflinePlayer player;
        private final CommandSender source;
        private final Map<String, String> taggedWhoisInformation = new LinkedHashMap<String, String>();
        private final List<String> taglessWhoisInformation = new ArrayList<String>();

        public PlayerWhoisEvent(OfflinePlayer player, CommandSender source) {
            this.player = player;
            this.source = source;
        }

        public OfflinePlayer getPlayer() {
            return player;
        }

        public CommandSender getSource() {
            return source;
        }

        public void addWhoisInformation(String key, Object value) {
            if (value == null) {
                addWhoisInformation(key, null);
            } else {
                addWhoisInformation(key, String.valueOf(value));
            }
        }

        public void addWhoisInformation(String key, String value) {
            if (key == null) {
                taglessWhoisInformation.add(value);
            } else {
                if (value == null) {
                    taggedWhoisInformation.remove(key);
                } else {
                    taggedWhoisInformation.put(key, value);
                }
            }
        }

        public Map<String, String> getTaggedWhoisInformation() {
            return Collections.unmodifiableMap(taggedWhoisInformation);
        }

        public List<String> getTaglessWhoisInformation() {
            return Collections.unmodifiableList(taglessWhoisInformation);
        }

        private static final HandlerList handlers = new HandlerList();

        public HandlerList getHandlers() {
            return handlers;
        }

        public static HandlerList getHandlerList() {
            return handlers;
        }
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
                player = InputUtil.PlayerParser.matchSinglePlayer(sender, args.getString(0));
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
                    offline = InputUtil.PlayerParser.matchSinglePlayer(sender, args.getString(0));
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

            PlayerWhoisEvent event = new PlayerWhoisEvent(offline, sender);

            if (offline instanceof Player) {
                Player player = (Player) offline;
                event.addWhoisInformation("Display name", player.getDisplayName());
                event.addWhoisInformation("Entity ID #", player.getEntityId());
                event.addWhoisInformation("Current vehicle", player.getVehicle());


                if (CommandBook.inst().hasPermission(sender, "commandbook.ip-address")) {
                    event.addWhoisInformation("Address", player.getAddress().toString());
                }
                event.addWhoisInformation("Game mode", player.getGameMode());
            }

            Location bedSpawn = offline.getBedSpawnLocation();
            if (bedSpawn != null) {
                event.addWhoisInformation("Bed spawn location",
                        ChatUtil.toFriendlyString(bedSpawn));
            } else {
                event.addWhoisInformation(null, "No bed spawn location");
            }

            if (offline.hasPlayedBefore()) {
                event.addWhoisInformation(null, "First joined: " + dateFormat.format(offline.getFirstPlayed())
                        + "; Last joined: " + dateFormat.format(offline.getLastPlayed()));
            }


            CommandBook.callEvent(event);

            List<String> results = new ArrayList<String>(event.getTaglessWhoisInformation());
            for (Map.Entry<String, String> entry : event.getTaggedWhoisInformation().entrySet()) {
                results.add(entry.getKey() + ": " + entry.getValue());
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
                player = InputUtil.PlayerParser.matchSinglePlayer(sender, args.getString(0));
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
                player = InputUtil.PlayerParser.matchSinglePlayer(sender, args.getString(0));
                if (player != sender) {
                    CommandBook.inst().checkPermission(sender, "commandbook.biome.other");
                }
            }

            Location loc = player.getLocation();
            String biomeName = loc.getWorld().getBiome(loc.getBlockX(), loc.getBlockZ()).name().toLowerCase().replace("_"," ");
            if (player.equals(sender)) {
                sender.sendMessage(ChatColor.YELLOW + "You are in the " + biomeName + " biome.");
            } else {
                sender.sendMessage(ChatColor.YELLOW + ChatUtil.toColoredName(player, ChatColor.YELLOW) + " is in the " + biomeName + " biome.");
            }

        }
    }
}
