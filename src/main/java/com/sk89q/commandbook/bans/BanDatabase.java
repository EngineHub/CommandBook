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

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.net.InetAddress;

/**
 * Interface for a ban database.
 *
 * @author sk89q
 */
public interface BanDatabase extends Iterable<Ban> {
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
     * @param name The name to check
     * @return Whether name is banned
     */
    public boolean isBannedName(String name);

    /**
     * Checks if a player's ddress is banned.
     *
     * @param address The address to check
     * @return Whether the given address is banned
     */
    public boolean isBannedAddress(InetAddress address);

    /**
     * Gets the ban message for a banned name.
     * WARNING: This method's is spelled incorrectly and will be removed soon
     * @param name The name to check
     * @return The banned message for the given name
     * @see #getBannedNameMessage(String)
     */
    @Deprecated
    public String getBannedNameMesage(String name);

    /**
     * Gets the ban message for a banned name.
     *
     * @param name The name to check
     * @return The banned message for the given name
     */
    public String getBannedNameMessage(String name);

    /**
     * Gets the ban message for a banned address.
     *
     * @param address The address to check
     * @return The banned message for the given address
     */
    public String getBannedAddressMessage(String address);

    /**
     * Bans a name.
     *
     * @param name
     * @param source
     * @param reason
     */
    @Deprecated
    public void banName(String name, CommandSender source, String reason);

    /**
     * Bans an address.
     *
     * @param address
     * @param source
     * @param reason
     */
    @Deprecated
    public void banAddress(String address, CommandSender source, String reason);

    /**
     * Bans a player by name and address
     *
     * @param player
     * @param source
     * @param reason
     * @param end
     */
    public void ban(Player player, CommandSender source, String reason, long end);

    /**
     * Bans a name and or address.
     * @param name
     * @param address
     * @param source
     * @param reason
     * @param end
     */
    public void ban(String name, String address, CommandSender source, String reason, long end);

    /**
     * Unbans a name.
     *
     * @param name
     * @param source
     * @param reason
     * @return whether the name was found
     */
    @Deprecated
    public boolean unbanName(String name, CommandSender source, String reason);

    /**
     * Unbans an address.
     *
     * @param address
     * @param source
     * @param reason
     * @return whether the address was found
     */
    @Deprecated
    public boolean unbanAddress(String address, CommandSender source, String reason);

    /**
     * Unban a name and/or address. First looks up by name, then if not found looks up by address.
     *
     * @param name
     * @param address
     * @param source
     * @param reason
     * @return whether the name or address was found
     */
    public boolean unban(String name, String address, CommandSender source, String reason);

    /**
     * Unbans a name.
     *
     * @param player
     * @param source
     * @param reason
     */
    public void logKick(Player player, CommandSender source, String reason);

    /**
     * Imports the bans from another ban database.
     *
     * @param bans
     */
    public void importFrom(BanDatabase bans);

    /**
     * Returns a Ban with the given name
     * @param name The name given to the ban.
     * @return The applicable ban
     */
    public Ban getBannedName(String name);

    /**
     * Returns a Ban with the given address
     * @param address The address given to the ban.
     * @return The applicable ban
     */
    public Ban getBannedAddress(String address);
}
