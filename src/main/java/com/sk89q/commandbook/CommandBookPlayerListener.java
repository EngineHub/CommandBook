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

package com.sk89q.commandbook;

import static com.sk89q.commandbook.CommandBookUtil.replaceColorMacros;
import static com.sk89q.commandbook.CommandBookUtil.sendMessage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import org.bukkit.event.player.PlayerQuitEvent;
import com.sk89q.commandbook.events.MOTDSendEvent;
import com.sk89q.commandbook.events.OnlineListSendEvent;
import com.sk89q.jinglenote.MidiJingleSequencer;

/**
 * Handler for player events.
 * 
 * @author sk89q
 */
public class CommandBookPlayerListener extends PlayerListener {
    protected static final Logger logger = Logger.getLogger("Minecraft.CommandBook");
    protected final static Pattern namePattern = Pattern.compile(
            "^[abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_]{1,16}$");
    
    protected CommandBookPlugin plugin;
    
    /**
     * Construct the listener.
     * 
     * @param plugin
     */
    public CommandBookPlayerListener(CommandBookPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Called on player login.
     */
    @Override
    public void onPlayerLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        
        if (plugin.verifyNameFormat && !namePattern.matcher(player.getName()).matches()) {
            logger.info("Name verification: " + player.getName() + " was kicked " +
            		"for having an invalid name (to disable, turn off verify-name-format in CommandBook)");
            event.disallow(Result.KICK_OTHER, "Invalid player name detected!");
            return;
        }
        
        try {
            if (plugin.getBanDatabase().isBannedName(event.getPlayer().getName())
                    || plugin.getBanDatabase().isBannedAddress(
                            event.getPlayer().getAddress().getAddress())) {
                event.disallow(Result.KICK_BANNED, plugin.getBanMessage());
                return;
            }
        } catch (NullPointerException e) {
            // Bug in CraftBukkit
        }
    }

    /**
     * Called on player chat.
     */
    @Override
    public void onPlayerChat(PlayerChatEvent event) {
        if (plugin.getAdminSession(event.getPlayer()).isMute()) {
            event.getPlayer().sendMessage(ChatColor.RED + "You are muted.");
            event.setCancelled(true);
            return;
        }
    }
    
    /**
     * Called on player join.
     */
    @Override
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Trigger the session
        plugin.getSession(player).handleReconnect();
        
        // Show the MOTD.
        String motd = plugin.getMessage("motd");
        
        if (motd != null) {
            plugin.getServer().getPluginManager().callEvent(
                    new MOTDSendEvent(player));
            
            sendMessage(player,
                    replaceColorMacros(
                    plugin.replaceMacros(
                    player, motd)));
        }
        
        // Show the online list
        if (plugin.getConfiguration().getBoolean("online-on-join", true)) {
            plugin.getServer().getPluginManager().callEvent(
                    new OnlineListSendEvent(player));
            
            CommandBookUtil.sendOnlineList(
                    plugin.getServer().getOnlinePlayers(), player, plugin);
        }

        try {
            MidiJingleSequencer sequencer = new MidiJingleSequencer(
                    new File(plugin.getDataFolder(), "intro.mid"));
            plugin.getJingleNoteManager().play(player, sequencer, 2000);
        } catch (MidiUnavailableException e) {
            logger.log(Level.WARNING, "CommandBook: Failed to access MIDI: "
                    + e.getMessage());
        } catch (InvalidMidiDataException e) {
            logger.log(Level.WARNING, "CommandBook: Failed to read intro MIDI file: "
                    + e.getMessage());
        } catch (FileNotFoundException e) {
            logger.info("CommandBook: No intro.mid; not playing intro song.");
        } catch (IOException e) {
            logger.log(Level.WARNING, "CommandBook: Failed to read intro MIDI file: "
                    + e.getMessage());
        }
    }
    
    /**
     * Called on player respawn.
     */
    @Override
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        
        if (plugin.exactSpawn) {
            event.setRespawnLocation(player.getWorld().getSpawnLocation());
        }
    }

    /**
     * Called when a player interacts.
     */
    @Override
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        
        if (plugin.getSession(player).hasThor()) {
            Material held = player.getItemInHand().getType();
            
            if (held != Material.DIAMOND_PICKAXE
                    && held != Material.IRON_PICKAXE
                    && held != Material.GOLD_PICKAXE
                    && held != Material.STONE_PICKAXE
                    && held != Material.WOOD_PICKAXE) {
                return;
            }
            
            if (event.getAction() == Action.LEFT_CLICK_AIR) {
                Block block = player.getTargetBlock(null, 300);
                if (block != null) {
                    player.getWorld().strikeLightning(block.getLocation());
                }
            } else if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                Block block = event.getClickedBlock();
                player.getWorld().strikeLightning(block.getLocation());
            }
        }
    }

    /**
     * Called on player disconnect.
     */
    @Override
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getSession(event.getPlayer()).handleDisconnect();
        plugin.getAdminSession(event.getPlayer()).handleDisconnect();
    }
    
}
