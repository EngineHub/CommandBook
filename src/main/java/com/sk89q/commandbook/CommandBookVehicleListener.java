/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sk89q.commandbook;

import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import com.sk89q.worldedit.Vector;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.vehicle.VehicleListener;
import org.bukkit.event.vehicle.VehicleMoveEvent;
/**
 * Handler for vehicle events.
 * 
 * @author Turtle9598
 */

public class CommandBookVehicleListener extends VehicleListener{
    
    protected static final Logger logger = Logger.getLogger("Minecraft.CommandBook");
    
    protected CommandBookPlugin plugin;
    /**
     * Construct the listener.
     * 
     * @param plugin
     */
    public CommandBookVehicleListener(CommandBookPlugin plugin) {
        this.plugin = plugin;
    }
  
    /**
    * Called when a vehicle moves.
    */
    
    @Override
    public void onVehicleMove(VehicleMoveEvent event) {
        Vehicle vehicle = event.getVehicle();
        if (vehicle.getPassenger() == null
                || !(vehicle.getPassenger() instanceof Player)) return;
        Player player = (Player) vehicle.getPassenger();

        if (event.getFrom().getBlockX() != event.getTo().getBlockX()
                || event.getFrom().getBlockY() != event.getTo().getBlockY()
                || event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {

            Vector pt = new Vector(event.getTo().getBlockX(), event.getTo().getBlockY(), event.getTo().getBlockZ());

            if (plugin.getAdminSession(player).isFrozen()) {
                
                player.sendMessage(ChatColor.RED + "You are frozen.");

                vehicle.setVelocity(new org.bukkit.util.Vector(0,0,0));
                vehicle.teleport(event.getFrom());
                return;
            }
        }
    }
}
