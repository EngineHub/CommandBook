// $Id$
/*
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

package com.sk89q.commandbook.bans;

import java.net.InetAddress;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Interface for a ban database.
 * 
 * @author sk89q
 */
public interface BanDatabase {
    /**
     * Load the ban database.
     * 
     * @return whether the operation was fully successful
     */
    public boolean load();
    
    /**
     * Save the database.
     * 
     * @return whether the operation was fully successful
     */
    public boolean save();

    /**
     * Unloads the database
     *
     * @return whether the operation was fully successful
     */
    public boolean unload();
    
    /**
     * Checks if a player's name is banned.
     * 
     * @param name
     * @return
     */
    public boolean isBannedName(String name);
    
    /**
     * Checks if a player's name is banned.
     * 
     * @param address
     * @return
     */
    public boolean isBannedAddress(InetAddress address);
    
    /**
     * Bans a name.
     * 
     * @param name
     * @param source 
     * @param reason 
     */
    public void banName(String name, CommandSender source, String reason);
    
    /**
     * Bans an address.
     * 
     * @param address
     * @param source 
     * @param reason 
     */
    public void banAddress(String address, CommandSender source, String reason);
    
    /**
     * Unbans a name.
     * 
     * @param name
     * @param source 
     * @param reason 
     * @return whether the name was found
     */
    public boolean unbanName(String name, CommandSender source, String reason);
    
    /**
     * Unbans an address.
     * 
     * @param address
     * @param source 
     * @param reason 
     * @return whether the address was found
     */
    public boolean unbanAddress(String address, CommandSender source, String reason);
    
    /**
     * Unbans a name.
     * 
     * @param player
     * @param source 
     * @param reason 
     */
    public void logKick(Player player, CommandSender source, String reason);
}
