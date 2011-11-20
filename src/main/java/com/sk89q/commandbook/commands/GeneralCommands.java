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
import org.bukkit.GameMode;
import org.bukkit.Location;
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
            flags = "do", min = 1, max = 3)
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

        if (item == null) {
            throw new CommandException("Something went wrong parsing the item info!");
        }
        giveItem(sender, item, amt, targets, plugin, args.hasFlag('d'), args.hasFlag('o'));
    }
    
    @Command(aliases = {"give"},
            usage = "[-d] <target> <item[:data]> [amount]", desc = "Give an item",
            flags = "do", min = 2, max = 3)
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

        if (item == null) {
            throw new CommandException("Something went wrong parsing the item info!");
        }
        giveItem(sender, item, amt, targets, plugin, args.hasFlag('d'), args.hasFlag('o'));
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
                
                out.append(plugin.useDisplayNames ? player.getDisplayName() : player.getName());
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
    
    @Command(aliases = {"playertime"},
            usage = "[filter] <time|\"current\">", desc = "Get/change a player's time",
            flags = "rsw", min = 0, max = 2)
    public static void playertime(CommandContext args, CommandBookPlugin plugin,
                                  CommandSender sender) throws CommandException {
        Iterable<Player> players;
        String timeStr = "current";
        boolean included = false;

        if (args.argsLength() < 2) {
            plugin.checkPermission(sender, "commandbook.time.player");
            if (args.argsLength() == 1) {
                timeStr = args.getString(0);
            }
            players = plugin.matchPlayers(plugin.checkPlayer(sender));
        } else {
            players = plugin.matchPlayers(sender, args.getString(0));
            timeStr = args.getString(1);
            plugin.checkPermission(sender, "commandbook.time.player.other");
        }

        if (args.hasFlag('r')) {
            for (Player player : players) {
                player.resetPlayerTime();
                if (!args.hasFlag('s')) {
                    player.sendMessage(ChatColor.YELLOW + "Your time was reset to world time");
                }
                if (sender instanceof Player && ((Player) sender).equals(player)) {
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
            plugin.checkPermission(sender, "commandbook.time.player.check");
            sender.sendMessage(ChatColor.YELLOW
                    + "Player Time: " + CommandBookUtil.getTimeString(plugin.matchSinglePlayer(sender,
                    args.getString(0, plugin.checkPlayer(sender).getName())).getPlayerTime()));
            return;
        }

        for (Player player : players) {
            if (!player.equals(sender)) {
                plugin.checkPermission(sender, "commandbook.time.player.other");
                player.sendMessage(ChatColor.YELLOW + "Your time set to " + CommandBookUtil.getTimeString(player.getPlayerTime()));
            } else {
                plugin.checkPermission(sender, "commandbook.time.player");
                player.sendMessage(ChatColor.YELLOW + "Your time set to " + CommandBookUtil.getTimeString(player.getPlayerTime()));
                included = true;
            }
            player.setPlayerTime(args.hasFlag('w') ? Integer.parseInt(timeStr) : plugin.matchTime(timeStr), args.hasFlag('w'));
        }
        if (!included) {
            sender.sendMessage(ChatColor.YELLOW + "Player times set to " + CommandBookUtil.getTimeString(plugin.matchTime(timeStr)));
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
    
    @Command(aliases = {"more"},
            usage = "[player]", desc = "Gets more of an item",
            flags = "aio", min = 0, max = 1)
    @CommandPermissions({"commandbook.more"})
    public static void more(CommandContext args, CommandBookPlugin plugin,
            CommandSender sender) throws CommandException {

        Iterable<Player> targets = null;
        boolean moreAll = args.hasFlag('a');
        boolean infinite = args.hasFlag('i');
        boolean overrideStackSize = args.hasFlag('o');
        if (infinite) {
            plugin.checkPermission(sender, "commandbook.more.infinite");
        } else if (overrideStackSize) {
            plugin.checkPermission(sender, "commandbook.override.maxstacksize");
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
                    expandStack(inventory.getItem(i), infinite, overrideStackSize);
                }
            } else {
                expandStack(player.getItemInHand(), infinite, overrideStackSize);
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

        if (plugin.getSession(player).getIdleStatus() == null) {
            String status = "";
            if (args.argsLength() > 0) {
                status = args.getJoinedStrings(0);
                plugin.getSession(player).setIdleStatus(status);
            }

            player.sendMessage(ChatColor.YELLOW
                    + (status.isEmpty() ? "Set as away" : "Set away status to \"" + status + "\"")
                    + ". To return, type /afk again.");
        } else {
            player.sendMessage(ChatColor.YELLOW + "You are no longer away.");
            plugin.getSession(player).setIdleStatus(null);
        }
    }

    @Command(aliases = {"gamemode"},
            usage = "[player] [gamemode]", desc = "Change a player's gamemode",
            flags = "c", min = 0, max = 2)
    @CommandPermissions({"commandbook.gamemode"})
    public static void gamemode(CommandContext args, CommandBookPlugin plugin,
            CommandSender sender) throws CommandException {

        Player player = null;
        GameMode mode = null;
        boolean change = false;

        if (args.argsLength() == 0) { // check self
            // check current player
            plugin.checkPermission(sender, "commandbook.gamemode.check");
            player = plugin.checkPlayer(sender);
            mode = player.getGameMode();
        } else {
            if (args.hasFlag('c')) { //check other player
                plugin.checkPermission(sender, "commandbook.gamemode.check.other");
                player = plugin.matchSinglePlayer(sender, args.getString(0));
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
                    player = plugin.checkPlayer(sender);
                } else { // 2 - first is player, second mode
                    plugin.checkPermission(sender, "commandbook.gamemode.change.other");
                    player = plugin.matchSinglePlayer(sender, args.getString(0));
                    modeString = args.getString(1);
                }

                if (modeString.equals("0") || modeString.equalsIgnoreCase("survival")) {
                    mode = GameMode.SURVIVAL;
                } else if (modeString.equals("1") || modeString.equalsIgnoreCase("creative")) {
                    mode = GameMode.CREATIVE;
                } else {
                    throw new CommandException("Unrecognized gamemode " + modeString
                            + ". Please use 'creative' (1) or 'surival' (0).");
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
