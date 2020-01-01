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

package com.sk89q.commandbook.component.bans;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.net.InetAddress;
import java.util.UUID;

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
     * Checks if a player's UUID is banned.
     *
     * @param ID The UUID to check
     * @return Whether the UUID is banned
     */
    public boolean isBanned(UUID ID);

    /**
     * Checks if a player's address is banned.
     *
     * @param address The address to check
     * @return Whether the given address is banned
     */
    public boolean isBanned(InetAddress address);

    /**
     * Gets the ban message for a banned UUID.
     *
     * @param ID The ID to check
     * @return The banned message for the given ID
     */
    @Deprecated
    public String getBannedMessage(UUID ID);

    /**
     * Gets the ban message for a banned address.
     *
     * @param address The address to check
     * @return The banned message for the given address
     */
    @Deprecated
    public String getBannedMessage(String address);

    /**
     * Bans a player by UUID and address.
     *
     * @param player
     * @param source
     * @param reason
     * @param end
     */
    public void ban(Player player, CommandSender source, String reason, long end);

    /**
     * Bans a UUID and or address.
     *
     * @param ID
     * @param name
     * @param address
     * @param source
     * @param reason
     * @param end
     */
    public void ban(UUID ID, String name, String address, CommandSender source, String reason, long end);

    /**
     * Unbans a name.
     *
     * WARNING: This method only works for cases where the UUID for the record is null.
     *
     * @param name
     * @param source
     * @param reason
     * @return whether the name was found
     */
    @Deprecated
    public boolean unbanName(String name, CommandSender source, String reason);

    /**
     * Unban a player by UUID.
     *
     * WARNING: This method will not unban a player's address
     *
     * @param player
     * @param source
     * @param reason
     * @return
     */
    public boolean unban(Player player, CommandSender source, String reason);

    /**
     * Unban a player and/or address. First looks up by UUID, then if not found looks up by address.
     *
     * @param ID
     * @param address
     * @param source
     * @param reason
     * @return
     */
    public boolean unban(UUID ID, String address, CommandSender source, String reason);

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
     * Returns a Ban with the given UUID
     * @param ID The UUID of the banned player.
     * @return The applicable ban
     */
    public Ban getBanned(UUID ID);

    /**
     * Returns a Ban with the given address
     * @param address The address given to the ban.
     * @return The applicable ban
     */
    public Ban getBanned(String address);
}
