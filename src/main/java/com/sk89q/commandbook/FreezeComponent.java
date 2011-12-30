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

import org.bukkit.entity.Vehicle;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.Location;
import com.sk89q.commandbook.components.AbstractComponent;
import com.sk89q.commandbook.components.ComponentInformation;
import com.sk89q.commandbook.components.InjectComponent;
import com.sk89q.commandbook.events.core.BukkitEvent;
import com.sk89q.commandbook.session.SessionComponent;
import com.sk89q.commandbook.util.PlayerUtil;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;


/**
 * 
 * @author Turtle9598
 */

@ComponentInformation(desc = "Blocks a specified player's movement on command")
public class FreezeComponent extends AbstractComponent implements Listener {

    @InjectComponent private SessionComponent sessions;
       
    @Override
    public void initialize() {
        registerCommands(Commands.class);
        CommandBook.inst().getEventManager().registerEvents(this, this);
    }

    /**
     * Called on player movement.
     * 
     * @param event Relevant event details
     */
    
    @BukkitEvent(type = Event.Type.PLAYER_MOVE)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (player.getVehicle() != null) return; // handled in vehicle listener
        if (event.getFrom().getBlockX() != event.getTo().getBlockX()
                || event.getFrom().getBlockY() != event.getTo().getBlockY()
                || event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {

            if (sessions.getAdminSession(player).isFrozen()) {
                
                player.sendMessage(ChatColor.RED + "You are frozen.");
            
                Location newLoc = event.getFrom();
                newLoc.setX(newLoc.getBlockX() + 0.5);
                newLoc.setY(newLoc.getBlockY());
                newLoc.setZ(newLoc.getBlockZ() + 0.5);
                event.setTo(newLoc);
                return;
            }
        }
    }
    
    /**
     * Called on player teleport.
     * 
     * @param event Relevant event details
     */
    
    @BukkitEvent(type = Event.Type.PLAYER_TELEPORT)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.UNKNOWN) {
            return; // Must check to see if the event is UNKNOWN as the Vehicle Move & Player Move events both use unknown teleport causes.
        }
        if (sessions.getAdminSession(player).isFrozen()) {
            player.sendMessage(ChatColor.RED + "You are frozen.");
            event.setCancelled(true);
        }
    }
    
    /**
     * Called on vehicle movement.
     * 
     * @param event Relevant event details
     */
    
    @BukkitEvent(type = Event.Type.VEHICLE_MOVE)
    public void onVehicleMove(VehicleMoveEvent event) {
        Vehicle vehicle = event.getVehicle();
        if (vehicle.getPassenger() == null
                || !(vehicle.getPassenger() instanceof Player)) return;
        Player player = (Player) vehicle.getPassenger();

        if (event.getFrom().getBlockX() != event.getTo().getBlockX()
                || event.getFrom().getBlockY() != event.getTo().getBlockY()
                || event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {

            if (sessions.getAdminSession(player).isFrozen()) {
                
                player.sendMessage(ChatColor.RED + "You are frozen.");

                vehicle.setVelocity(new org.bukkit.util.Vector(0,0,0));
                vehicle.teleport(event.getFrom());
                return;
            }
        }
    }

    public class Commands {
        @Command(aliases = {"freeze"}, usage = "<target>", desc = "Freeze a player", min = 1, max = 1)
        @CommandPermissions({"commandbook.freeze"})
        public void freeze(CommandContext args, CommandSender sender) throws CommandException {

            Player player = PlayerUtil.matchSinglePlayer(sender, args.getString(0));

            sessions.getAdminSession(player).setFrozen(true);

            player.sendMessage(ChatColor.YELLOW + "You've been frozen by "
                    + PlayerUtil.toName(sender));
            sender.sendMessage(ChatColor.YELLOW + "You've frozen "
                    + PlayerUtil.toName(player));
        }

        @Command(aliases = {"unfreeze"}, usage = "<target>", desc = "Unmute a player", min = 1, max = 1)
        @CommandPermissions({"commandbook.freeze"})
        public void unfreeze(CommandContext args, CommandSender sender) throws CommandException {
            Player player = PlayerUtil.matchSinglePlayer(sender, args.getString(0));

            sessions.getAdminSession(player).setFrozen(false);

            player.sendMessage(ChatColor.YELLOW + "You've been unfrozen by "
                    + PlayerUtil.toName(sender));
            sender.sendMessage(ChatColor.YELLOW + "You've unfrozen "
                    + PlayerUtil.toName(player));
        }
    }
}
