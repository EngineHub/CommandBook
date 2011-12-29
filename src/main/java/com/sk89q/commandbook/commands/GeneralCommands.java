// $Id$
/*
 * CommandBook
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

package com.sk89q.commandbook.commands;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.util.PlayerUtil;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import com.sk89q.commandbook.CommandBookUtil;
import com.sk89q.commandbook.events.MOTDSendEvent;
import com.sk89q.commandbook.events.OnlineListSendEvent;
import com.sk89q.jinglenote.MidiJingleSequencer;
import com.sk89q.minecraft.util.commands.*;
import static com.sk89q.commandbook.CommandBookUtil.*;

public class GeneralCommands {
    
    private CommandBook plugin;
    
    public GeneralCommands(CommandBook plugin) {
        this.plugin = plugin;
    }

    @Command(aliases = {"cmdbook"}, desc = "CommandBook commands",
            flags = "d", min = 1, max = 3)
    @NestedCommand({CommandBookCommands.class})
    public static void cmdBook() {
    }
    
    @Command(aliases = {"who"},
            usage = "[filter]", desc = "Get the list of online users",
            min = 0, max = 1)
    @CommandPermissions({"commandbook.who"})
    public void who(CommandContext args, CommandSender sender) throws CommandException {
        Player[] online = plugin.getServer().getOnlinePlayers();
        
        // Some crappy wrappers uses this to detect if the server is still
        // running, even though this is a very unreliable way to do it
        if (!(sender instanceof Player) && plugin.crappyWrapperCompat) {
            StringBuilder out = new StringBuilder();
            
            out.append("Connected players: ");
            
            // To keep track of commas
            boolean first = true;
            
            // Now go through the list of players and find any matching players
            // (in case of a filter), and create the list of players.
            for (Player player : online) {
                if (!first) {
                    out.append(", ");
                }
                
                out.append(plugin.useDisplayNames ? player.getDisplayName() : player.getName());
                out.append(ChatColor.WHITE);

                first = false;
            }
            
            sender.sendMessage(out.toString());
            
            return;
        }

        plugin.getEventManager().callEvent(
                new OnlineListSendEvent(sender));
        
        // This applies mostly to the console, so there might be 0 players
        // online if that's the case!
        if (online.length == 0) {
            sender.sendMessage("0 players are online.");
            return;
        }
        
        // Get filter
        String filter = args.getString(0, "").toLowerCase();
        filter = filter.length() == 0 ? null : filter;

        // For filtered queries, we say something a bit different
        if (filter == null) {
            CommandBookUtil.sendOnlineList(
                    plugin.getServer().getOnlinePlayers(), sender, plugin);
            return;
            
        }
        
        StringBuilder out = new StringBuilder();
        
        out.append(ChatColor.GRAY + "Found players (out of ");
        out.append(ChatColor.GRAY + "" + online.length);
        out.append(ChatColor.GRAY + "): ");
        out.append(ChatColor.WHITE);
        
        // To keep track of commas
        boolean first = true;
        
        // Now go through the list of players and find any matching players
        // (in case of a filter), and create the list of players.
        for (Player player : online) {
            // Process the filter
            if (filter != null && !player.getName().toLowerCase().contains(filter)) {
                break;
            }
            
            if (!first) {
                out.append(", ");
            }
            
            out.append(player.getName());
            
            first = false;
        }
        
        // This means that no matches were found!
        if (first) {
            sender.sendMessage(ChatColor.RED + "No players (out of "
                    + online.length + ") matched '" + filter + "'.");
            return;
        }
        
        sender.sendMessage(out.toString());
    }
    
    @Command(aliases = {"motd"},
            usage = "", desc = "Show the message of the day",
            min = 0, max = 0)
    @CommandPermissions({"commandbook.motd"})
    public void motd(CommandContext args, CommandSender sender) throws CommandException {
        
        String motd = plugin.getMessage("motd");
        
        if (motd == null) {
            sender.sendMessage(ChatColor.RED + "MOTD not configured in CommandBook yet!");
        } else {
            plugin.getEventManager().callEvent(
                    new MOTDSendEvent(sender));
            
            sendMessage(sender,
                    replaceColorMacros(
                            plugin.replaceMacros(
                                    sender, motd)));
        }
    }
    
    @Command(aliases = {"rules"},
            usage = "", desc = "Show the rules",
            min = 0, max = 0)
    @CommandPermissions({"commandbook.rules"})
    public void rules(CommandContext args, CommandSender sender) throws CommandException {
        
        String motd = plugin.getMessage("rules");
        
        if (motd == null) {
            sender.sendMessage(ChatColor.RED + "Rules not configured in CommandBook yet!");
        } else {
            sendMessage(sender,
                    replaceColorMacros(
                            plugin.replaceMacros(
                                    sender, motd)));
        }
    }
    
    @Command(aliases = {"whereami"},
            usage = "[player]", desc = "Show your current location",
            flags = "", min = 0, max = 1)
    @CommandPermissions({"commandbook.whereami"})
    public void whereAmI(CommandContext args, CommandSender sender) throws CommandException {
        
        Player player;
        
        if (args.argsLength() == 0) {
            player = PlayerUtil.checkPlayer(sender);
        } else {
            plugin.checkPermission(sender, "commandbook.whereami.other");
            
            player = PlayerUtil.matchSinglePlayer(sender, args.getString(0));
        }

        Location pos = player.getLocation();
        
        sender.sendMessage(ChatColor.YELLOW +
                "You are in the world: " + PlayerUtil.checkPlayer(sender).getWorld().getName());
        sender.sendMessage(ChatColor.YELLOW +
                String.format("You're at: (%.4f, %.4f, %.4f)",
                        pos.getX(), pos.getY(), pos.getZ()));
        sender.sendMessage(ChatColor.YELLOW +
                "Your depth is: " + (int) Math.floor(pos.getY()));
        
        if (plugin.hasPermission(sender, "commandbook.whereami.compass")) {
            sender.sendMessage(ChatColor.YELLOW +
                    String.format("Your direction: %s",
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
            plugin.checkPermission(sender, "commandbook.whois.other");
            
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
        
        if (plugin.hasPermission(sender, "commandbook.ip-address")) {
            sender.sendMessage(ChatColor.YELLOW
                    + "Address: " + player.getAddress().toString());
        }
    }
    

    
    @Command(aliases = {"ping"},
            usage = "", desc = "A dummy command",
            flags = "", min = 0, max = 0)
    public void ping(CommandContext args, CommandSender sender) throws CommandException {
        sender.sendMessage(ChatColor.YELLOW + "Pong!");
    }
    
    @Command(aliases = {"pong"},
            usage = "", desc = "A dummy command",
            flags = "", min = 0, max = 0)
    public void pong(CommandContext args, CommandSender sender) throws CommandException {
        
        sender.sendMessage(ChatColor.YELLOW +
                "I hear " + PlayerUtil.toName(sender) + " likes cute Asian boys.");
    }

    @Command(aliases = {"gamemode"},
            usage = "[player] [gamemode]", desc = "Change a player's gamemode",
            flags = "c", min = 0, max = 2)
    @CommandPermissions({"commandbook.gamemode"})
    public void gamemode(CommandContext args, CommandSender sender) throws CommandException {

        Player player = null;
        GameMode mode = null;
        boolean change = false;

        if (args.argsLength() == 0) { // check self
            // check current player
            plugin.checkPermission(sender, "commandbook.gamemode.check");
            player = PlayerUtil.checkPlayer(sender);
            mode = player.getGameMode();
        } else {
            if (args.hasFlag('c')) { //check other player
                plugin.checkPermission(sender, "commandbook.gamemode.check.other");
                player = PlayerUtil.matchSinglePlayer(sender, args.getString(0));
                mode = player.getGameMode();
            } else {
                change = true;

                // we're going to assume that the first arg of one is mode, but the first of two is player
                // if they want to check another player, they should use -c instead, since we can't guess
                // reliably whether (with a single arg) they meant a player or a mode
                String modeString = null;
                if (args.argsLength() == 1) { // self mode
                    plugin.checkPermission(sender, "commandbook.gamemode.change");
                    modeString = args.getString(0);
                    player = PlayerUtil.checkPlayer(sender);
                } else { // 2 - first is player, second mode
                    plugin.checkPermission(sender, "commandbook.gamemode.change.other");
                    player = PlayerUtil.matchSinglePlayer(sender, args.getString(0));
                    modeString = args.getString(1);
                }

                try {
                    mode = GameMode.valueOf(modeString.toUpperCase());
                } catch (IllegalArgumentException e) {
                    try {
                        mode = GameMode.getByValue(Integer.parseInt(modeString));
                    } catch (NumberFormatException ex) {}
                }
                if (mode == null) {
                    throw new CommandException("Unrecognized gamemode: " + modeString + ".");
                }
            }
        }

        if (player == null || mode == null) {
            throw new CommandException("Something went wrong, please try again.");
        }

        String message = null;
        if (change) {
            if (player.getGameMode() == mode) {
                message = " already had gamemode " + mode.toString();
                change = false;
            } else {
                message = " changed to gamemode " + mode.toString();
            }
        } else {
            message = " currently has gamemode " + mode.toString();
        }
        if (change) {
            player.setGameMode(mode);
        }
        sender.sendMessage("Player " + (plugin.useDisplayNames ? player.getDisplayName() : player.getName())
                + message + ".");
        return;
    }
}
