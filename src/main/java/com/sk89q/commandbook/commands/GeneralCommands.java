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
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import com.sk89q.commandbook.CommandBookPlugin;
import com.sk89q.commandbook.CommandBookUtil;
import com.sk89q.commandbook.events.MOTDSendEvent;
import com.sk89q.commandbook.events.OnlineListSendEvent;
import com.sk89q.jinglenote.MidiJingleSequencer;
import com.sk89q.minecraft.util.commands.*;
import static com.sk89q.commandbook.CommandBookUtil.*;

public class GeneralCommands {

    @Command(aliases = {"cmdbook"}, desc = "CommandBook commands",
            flags = "d", min = 1, max = 3)
    @NestedCommand({CommandBookCommands.class})
    public static void cmdBook() {
    }
    
    @Command(aliases = {"item"},
            usage = "[target] <item[:data]> [amount]", desc = "Give an item",
            flags = "d", min = 1, max = 3)
    @CommandPermissions({"commandbook.give"})
    public static void item(CommandContext args, CommandBookPlugin plugin,
            CommandSender sender) throws CommandException {
        
        ItemStack item = null;
        int amt = plugin.defaultItemStackSize;
        Iterable<Player> targets = null;

        // How this command handles parameters depends on how many there
        // are, so the following code splits the incoming input
        // into three different possibilities
        
        // One argument: Just the item type and amount 1
        if (args.argsLength() == 1) {
            item = plugin.matchItem(sender, args.getString(0));
            targets = plugin.matchPlayers(plugin.checkPlayer(sender));
        // Two arguments: Item type and amount
        } else if (args.argsLength() == 2) {
            item = plugin.matchItem(sender, args.getString(0));
            amt = args.getInteger(1);
            targets = plugin.matchPlayers(plugin.checkPlayer(sender));
        // Three arguments: Player, item type, and item amount
        } else if (args.argsLength() == 3) {
            item = plugin.matchItem(sender, args.getString(1));
            amt = args.getInteger(2);
            targets = plugin.matchPlayers(sender, args.getString(0));
            
            // Make sure that this player has permission to give items to other
            /// players!
            plugin.checkPermission(sender, "commandbook.give.other");
        }
        
        giveItem(sender, item, amt, targets, plugin, args.hasFlag('d'));
    }
    
    @Command(aliases = {"give"},
            usage = "[-d] <target> <item[:data]> [amount]", desc = "Give an item",
            flags = "d", min = 2, max = 3)
    @CommandPermissions({"commandbook.give.other"})
    public static void give(CommandContext args, CommandBookPlugin plugin,
            CommandSender sender) throws CommandException {
        
        ItemStack item = null;
        int amt = plugin.defaultItemStackSize;
        Iterable<Player> targets = null;

        // How this command handles parameters depends on how many there
        // are, so the following code splits the incoming input
        // into three different possibilities

        // Two arguments: Player, item type
        if (args.argsLength() == 2) {
            targets = plugin.matchPlayers(sender, args.getString(0));
            item = plugin.matchItem(sender, args.getString(1));
        // Three arguments: Player, item type, and item amount
        } else if (args.argsLength() == 3) {
            targets = plugin.matchPlayers(sender, args.getString(0));
            item = plugin.matchItem(sender, args.getString(1));
            amt = args.getInteger(2);
        }
        
        giveItem(sender, item, amt, targets, plugin, args.hasFlag('d'));
    }
    
    @Command(aliases = {"who"},
            usage = "[filter]", desc = "Get the list of online users",
            min = 0, max = 1)
    @CommandPermissions({"commandbook.who"})
    public static void who(CommandContext args, CommandBookPlugin plugin,
            CommandSender sender) throws CommandException {
        
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
                
                out.append(player.getName());
                out.append(ChatColor.WHITE);

                first = false;
            }
            
            sender.sendMessage(out.toString());
            
            return;
        }

        plugin.getServer().getPluginManager().callEvent(
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
    
    @Command(aliases = {"time"},
            usage = "[world] <time|\"current\">", desc = "Get/change the world time",
            flags = "l", min = 0, max = 2)
    public static void time(CommandContext args, CommandBookPlugin plugin,
            CommandSender sender) throws CommandException {
        
        World world;
        String timeStr;
        boolean onlyLock = false;

        // Easy way to get the time
        if (args.argsLength() == 0) {
            world = plugin.checkPlayer(sender).getWorld();
            timeStr = "current";
        // If no world was specified, get the world from the sender, but
        // fail if the sender isn't player
        } else if (args.argsLength() == 1) {
            world = plugin.checkPlayer(sender).getWorld();
            timeStr = args.getString(0);
        } else { // A world was specified!
            world = plugin.matchWorld(sender, args.getString(0));
            timeStr = args.getString(1);
        }
        
        // Let the player get the time
        if (timeStr.equalsIgnoreCase("current")
                || timeStr.equalsIgnoreCase("cur")
                || timeStr.equalsIgnoreCase("now")) {
            
            // We want to lock to the current time
            if (!args.hasFlag('l')) {
                plugin.checkPermission(sender, "commandbook.time.check");
                sender.sendMessage(ChatColor.YELLOW
                        + "Time: " + CommandBookUtil.getTimeString(world.getTime()));
                return;
            }
            
            onlyLock = true;
        }
        
        plugin.checkPermission(sender, "commandbook.time");

        if (!onlyLock) {
            plugin.getTimeLockManager().unlock(world);
            world.setTime(plugin.matchTime(timeStr));
        }
        
        String verb = "set";
        
        // Locking
        if (args.hasFlag('l')) {
            plugin.checkPermission(sender, "commandbook.time.lock");
            plugin.getTimeLockManager().lock(world);
            verb = "locked";
        }
        
        if (plugin.broadcastChanges) { 
            plugin.getServer().broadcastMessage(ChatColor.YELLOW
                    + plugin.toName(sender) + " " + verb + " the time of '"
                    + world.getName() + "' to "
                    + CommandBookUtil.getTimeString(world.getTime()) + ".");
        }
        
        // Tell console, since console won't get the broadcast message.
        if (!(sender instanceof Player) || !plugin.broadcastChanges) {
            sender.sendMessage(ChatColor.YELLOW + "Time " + verb + " to "
                    + CommandBookUtil.getTimeString(world.getTime()) + ".");
        }
    }
    
    @Command(aliases = {"motd"},
            usage = "", desc = "Show the message of the day",
            min = 0, max = 0)
    @CommandPermissions({"commandbook.motd"})
    public static void motd(CommandContext args, CommandBookPlugin plugin,
            CommandSender sender) throws CommandException {
        
        String motd = plugin.getMessage("motd");
        
        if (motd == null) {
            sender.sendMessage(ChatColor.RED + "MOTD not configured in CommandBook yet!");
        } else {
            plugin.getServer().getPluginManager().callEvent(
                    new MOTDSendEvent(sender));
            
            sendMessage(sender,
                    replaceColorMacros(
                    plugin.replaceMacros(
                    sender, motd)));
        }
    }
    
    @Command(aliases = {"intro"},
            usage = "", desc = "Play the introduction song",
            min = 0, max = 0)
    @CommandPermissions({"commandbook.intro"})
    public static void intro(CommandContext args, CommandBookPlugin plugin,
            CommandSender sender) throws CommandException {

        Player player = plugin.checkPlayer(sender);
        
        if (plugin.disableMidi) {
            throw new CommandException("MIDI support is disabled.");
        }

        try {
            MidiJingleSequencer sequencer = new MidiJingleSequencer(
                    new File(plugin.getDataFolder(), "intro.mid"));
            plugin.getJingleNoteManager().play(player, sequencer, 0);
            sender.sendMessage(ChatColor.YELLOW + "Playing intro.midi...");
        } catch (MidiUnavailableException e) {
            throw new CommandException("Failed to access MIDI: "
                    + e.getMessage());
        } catch (InvalidMidiDataException e) {
            throw new CommandException("Failed to read intro MIDI file: "
                    + e.getMessage());
        } catch (FileNotFoundException e) {
            throw new CommandException("No intro.mid is available.");
        } catch (IOException e) {
            throw new CommandException("Failed to read intro MIDI file: "
                    + e.getMessage());
        }
    }
    
    @Command(aliases = {"midi"},
            usage = "[midi]", desc = "Play a MIDI file",
            min = 0, max = 1)
    public static void midi(CommandContext args, CommandBookPlugin plugin,
            CommandSender sender) throws CommandException {

        Player player = plugin.checkPlayer(sender);
        
        if (args.argsLength() == 0) {
            plugin.getJingleNoteManager().stop(player);
            sender.sendMessage(ChatColor.YELLOW + "All music stopped.");
            return;
        }
        
        plugin.checkPermission(sender, "commandbook.midi");
        
        String filename = args.getString(0);
        
        if (!filename.matches("^[A-Za-z0-9 \\-_\\.~\\[\\]\\(\\$),]+$")) {
            throw new CommandException("Invalid filename specified!");
        }
        
        File[] trialPaths = {
                new File(plugin.getDataFolder(), "midi/" + filename),
                new File(plugin.getDataFolder(), "midi/" + filename + ".mid"),
                new File(plugin.getDataFolder(), "midi/" + filename + ".midi"),
                new File("midi", filename),
                new File("midi", filename + ".mid"),
                new File("midi", filename + ".midi"),
                };
        
        File file = null;
        
        for (File f : trialPaths) {
            if (f.exists()) {
                file = f;
                break;
            }
        }
        
        if (file == null) {
            throw new CommandException("The specified MIDI file was not found.");
        }
        
        try {
            MidiJingleSequencer sequencer = new MidiJingleSequencer(file);
            plugin.getJingleNoteManager().play(player, sequencer, 0);
            sender.sendMessage(ChatColor.YELLOW + "Playing " + file.getName()
                    + "... Use '/midi' to stop.");
        } catch (MidiUnavailableException e) {
            throw new CommandException("Failed to access MIDI: "
                    + e.getMessage());
        } catch (InvalidMidiDataException e) {
            throw new CommandException("Failed to read intro MIDI file: "
                    + e.getMessage());
        } catch (FileNotFoundException e) {
            throw new CommandException("The specified MIDI file was not found.");
        } catch (IOException e) {
            throw new CommandException("Failed to read intro MIDI file: "
                    + e.getMessage());
        }
    }
    
    @Command(aliases = {"rules"},
            usage = "", desc = "Show the rules",
            min = 0, max = 0)
    @CommandPermissions({"commandbook.rules"})
    public static void rules(CommandContext args, CommandBookPlugin plugin,
            CommandSender sender) throws CommandException {
        
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
    public static void whereAmI(CommandContext args, CommandBookPlugin plugin,
            CommandSender sender) throws CommandException {
        
        Player player;
        
        if (args.argsLength() == 0) {
            player = plugin.checkPlayer(sender);
        } else {
            plugin.checkPermission(sender, "commandbook.whereami.other");
            
            player = plugin.matchSinglePlayer(sender, args.getString(0));
        }

        Location pos = player.getLocation();
        
        sender.sendMessage(ChatColor.YELLOW +
                "You are in the world: " + plugin.checkPlayer(sender).getWorld().getName());
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
    
    @Command(aliases = {"compass"},
            usage = "[player]", desc = "Show your current compass direction",
            flags = "", min = 0, max = 1)
    @CommandPermissions({"commandbook.whereami.compass.other"})
    public static void compass(CommandContext args, CommandBookPlugin plugin,
            CommandSender sender) throws CommandException {

        Player player;
        
        if (args.argsLength() == 0) {
            player = plugin.checkPlayer(sender);
        } else {
            plugin.checkPermission(sender, "commandbook.whereami.other");
            
            player = plugin.matchSinglePlayer(sender, args.getString(0));
        }

        sender.sendMessage(ChatColor.YELLOW +
                String.format("Your direction: %s",
                        getCardinalDirection(player)));
    }

    @Command(aliases = {"biome"},
            usage = "[player]", desc = "Get your current biome",
            flags = "", min = 0, max = 1)
    @CommandPermissions({"commandbook.biome"})
    public static void biome(CommandContext args, CommandBookPlugin plugin,
            CommandSender sender) throws CommandException {
        
        Player player;

        if (args.argsLength() == 0) {
            player = plugin.checkPlayer(sender);
        } else {
            plugin.checkPermission(sender, "commandbook.biome.other");
            
            player = plugin.matchSinglePlayer(sender, args.getString(0));
        }

        sender.sendMessage(ChatColor.YELLOW + player.getLocation().getBlock().getBiome().name().toLowerCase().replace("_"," ")+" biome.");

    }

    @Command(aliases = {"whois"},
            usage = "[player]", desc = "Tell information about a player",
            flags = "", min = 0, max = 1)
    @CommandPermissions({"commandbook.whois"})
    public static void whois(CommandContext args, CommandBookPlugin plugin,
            CommandSender sender) throws CommandException {
        
        Player player;
        
        if (args.argsLength() == 0) {
            player = plugin.checkPlayer(sender);
        } else {
            plugin.checkPermission(sender, "commandbook.whois.other");
            
            player = plugin.matchSinglePlayer(sender, args.getString(0));
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
    
    @Command(aliases = {"setspawn"},
            usage = "[location]", desc = "Change spawn location",
            flags = "", min = 0, max = 1)
    @CommandPermissions({"commandbook.setspawn"})
    public static void setspawn(CommandContext args, CommandBookPlugin plugin,
            CommandSender sender) throws CommandException {
        
        World world;
        Location loc;
        
        if (args.argsLength() == 0) {
            Player player = plugin.checkPlayer(sender);
            world = player.getWorld();
            loc = player.getLocation();
        } else {
            loc = plugin.matchLocation(sender, args.getString(0));
            world = loc.getWorld();
        }

        world.setSpawnLocation(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());

        sender.sendMessage(ChatColor.YELLOW +
                "Spawn location of '" + world.getName() + "' set!");
    }
    
    @Command(aliases = {"clear"},
            usage = "[target]", desc = "Clear your inventory",
            flags = "as", min = 0, max = 1)
    @CommandPermissions({"commandbook.clear"})
    public static void clear(CommandContext args, CommandBookPlugin plugin,
            CommandSender sender) throws CommandException {
        
        Iterable<Player> targets = null;
        boolean clearAll = args.hasFlag('a');
        boolean clearSingle = args.hasFlag('s');
        boolean included = false;
        
        if (args.argsLength() == 0) {
            targets = plugin.matchPlayers(plugin.checkPlayer(sender));
        // A different player
        } else {
            targets = plugin.matchPlayers(sender, args.getString(0));
            
            // Make sure that this player can clear other players!
            plugin.checkPermission(sender, "commandbook.clear.other");
        }
        
        for (Player player : targets) {
            Inventory inventory = player.getInventory();
            
            if (clearSingle) {
                player.setItemInHand(null);
            } else {
                for (int i = (clearAll ? 0 : 9); i < 36; i++) {
                    inventory.setItem(i, null);
                }
                
                if (clearAll) {
                    // Armor slots
                    for (int i = 36; i <= 39; i++) {
                        inventory.setItem(i, null);
                    }
                }
            }
        
            // Tell the user about the given item
            if (player.equals(sender)) {
                if (clearAll) {
                    player.sendMessage(ChatColor.YELLOW
                            + "Your inventory has been cleared.");
                } else {
                    player.sendMessage(ChatColor.YELLOW
                            + "Your inventory has been cleared. Use -a to clear ALL.");
                }
                
                // Keep track of this
                included = true;
            } else {
                player.sendMessage(ChatColor.YELLOW
                        + "Your inventory has been cleared by "
                        + plugin.toName(sender));
                
            }
        }
        
        // The player didn't receive any items, then we need to send the
        // user a message so s/he know that something is indeed working
        if (!included) {
            sender.sendMessage(ChatColor.YELLOW
                    + "Inventories cleared.");
        }
    }
    
    @Command(aliases = {"ping"},
            usage = "", desc = "A dummy command",
            flags = "", min = 0, max = 0)
    public static void ping(CommandContext args, CommandBookPlugin plugin,
            CommandSender sender) throws CommandException {
        
        sender.sendMessage(ChatColor.YELLOW +
                "Pong!");
    }
    
    @Command(aliases = {"pong"},
            usage = "", desc = "A dummy command",
            flags = "", min = 0, max = 0)
    public static void pong(CommandContext args, CommandBookPlugin plugin,
            CommandSender sender) throws CommandException {
        
        sender.sendMessage(ChatColor.YELLOW +
                "I hear " + plugin.toName(sender) + " likes cute Asian boys.");
    }
    
    @Command(aliases = {"debug"}, desc = "Debugging commands")
    @NestedCommand({DebuggingCommands.class})
    public static void debug(CommandContext args, CommandBookPlugin plugin,
            CommandSender sender) throws CommandException {
    }
    
    @Command(aliases = {"weather"},
            usage = "<'stormy'|'sunny'> [duration] [world]", desc = "Change the world weather",
            min = 1, max = 3)
    @CommandPermissions({"commandbook.weather"})
    public static void weather(CommandContext args, CommandBookPlugin plugin,
            CommandSender sender) throws CommandException {
        
        World world;
        String weatherStr = args.getString(0);
        int duration = -1;

        if (args.argsLength() == 1) {
            world = plugin.checkPlayer(sender).getWorld();
        } else if (args.argsLength() == 2) {
            world = plugin.checkPlayer(sender).getWorld();
            duration = args.getInteger(1);
        } else { // A world was specified!
            world = plugin.matchWorld(sender, args.getString(2));
            duration = args.getInteger(1);
        }
        
        if (weatherStr.equalsIgnoreCase("stormy")
                || weatherStr.equalsIgnoreCase("rainy")
                || weatherStr.equalsIgnoreCase("snowy")
                || weatherStr.equalsIgnoreCase("rain")
                || weatherStr.equalsIgnoreCase("snow")
                || weatherStr.equalsIgnoreCase("on")) {
            
            world.setStorm(true);
            
            if (duration > 0) {
                world.setWeatherDuration(duration * 20);
            }

            if (plugin.broadcastChanges) { 
                plugin.getServer().broadcastMessage(ChatColor.YELLOW
                        + plugin.toName(sender) + " has started on a storm on '"
                        + world.getName() + "'.");
            }
            
            // Tell console, since console won't get the broadcast message.
            if (!(sender instanceof Player) || !plugin.broadcastChanges) {
                sender.sendMessage(ChatColor.YELLOW + "Stormy weather enabled.");
            }
            
        } else if (weatherStr.equalsIgnoreCase("clear")
                || weatherStr.equalsIgnoreCase("sunny")
                || weatherStr.equalsIgnoreCase("snowy")
                || weatherStr.equalsIgnoreCase("rain")
                || weatherStr.equalsIgnoreCase("snow")
                || weatherStr.equalsIgnoreCase("off")) {
            
            world.setStorm(false);
            
            if (duration > 0) {
                world.setWeatherDuration(duration * 20);
            }

            if (plugin.broadcastChanges) { 
                plugin.getServer().broadcastMessage(ChatColor.YELLOW
                        + plugin.toName(sender) + " has stopped a storm on '"
                        + world.getName() + "'.");
            }
            
            // Tell console, since console won't get the broadcast message.
            if (!(sender instanceof Player) || !plugin.broadcastChanges) {
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
    public static void thunder(CommandContext args, CommandBookPlugin plugin,
            CommandSender sender) throws CommandException {
        
        World world;
        String weatherStr = args.getString(0);
        int duration = -1;

        if (args.argsLength() == 1) {
            world = plugin.checkPlayer(sender).getWorld();
        } else if (args.argsLength() == 2) {
            world = plugin.checkPlayer(sender).getWorld();
            duration = args.getInteger(1);
        } else { // A world was specified!
            world = plugin.matchWorld(sender, args.getString(2));
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
    
    @Command(aliases = {"more"},
            usage = "[player]", desc = "Gets more of an item",
            flags = "ai", min = 0, max = 1)
    @CommandPermissions({"commandbook.more"})
    public static void more(CommandContext args, CommandBookPlugin plugin,
            CommandSender sender) throws CommandException {

        Iterable<Player> targets = null;
        boolean moreAll = args.hasFlag('a');
        boolean infinite = args.hasFlag('i');
        if (infinite) {
            plugin.checkPermission(sender, "commandbook.more.infinite");
        }
        boolean included = false;
        
        if (args.argsLength() == 0) {
            targets = plugin.matchPlayers(plugin.checkPlayer(sender));
        // A different player
        } else {
            targets = plugin.matchPlayers(sender, args.getString(0));
            
            // Make sure that this player can 'more' other players!
            plugin.checkPermission(sender, "commandbook.more.other");
        }
        
        for (Player player : targets) {
            Inventory inventory = player.getInventory();
            
            if (moreAll) {
                for (int i = 0; i < 39; i++) {
                    expandStack(inventory.getItem(i), infinite);
                }
            } else {
                expandStack(player.getItemInHand(), infinite);
            }
        
            // Tell the user about the given item
            if (player.equals(sender)) {
                player.sendMessage(ChatColor.YELLOW
                        + "Your item(s) has been expanded in stack size.");
                
                // Keep track of this
                included = true;
            } else {
                player.sendMessage(ChatColor.YELLOW
                        + "Your item(s) has been expanded in stack size by "
                        + plugin.toName(sender));
                
            }
        }
        
        // The player didn't receive any items, then we need to send the
        // user a message so s/he know that something is indeed working
        if (!included) {
            sender.sendMessage(ChatColor.YELLOW
                    + "Stack sizes increased.");
        }
    }

    @Command(aliases = {"afk"},
            usage = "", desc = "Set yourself as away",
            flags = "", min = 0, max = -1)
    @CommandPermissions({"commandbook.away"})
    public static void afk(CommandContext args, CommandBookPlugin plugin,
            CommandSender sender) throws CommandException {

        Player player = plugin.checkPlayer(sender);

        if (plugin.getAdminSession(player).getIdleStatus() == null) {
            String status = "";
            if (args.argsLength() > 0) {
                status = args.getJoinedStrings(0);
                plugin.getAdminSession(player).setIdleStatus(status);
            }

            player.sendMessage(ChatColor.YELLOW
                    + (status.isEmpty() ? "Set as away" : "Set away status to \"" + status + "\"")
                    + ". To return, type /afk again.");
        } else {
            player.sendMessage(ChatColor.YELLOW + "You are no longer away.");
            plugin.getAdminSession(player).setIdleStatus(null);
        }
    }

}
