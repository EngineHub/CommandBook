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

package com.sk89q.commandbook.locations;

import com.sk89q.commandbook.CommandBook;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

public class NamedLocation {

    private String name;
    private UUID ownerID;
    private String ownerName;
    private String worldName;
    private Location loc;

    /**
     * This constructor will be removed and this class will be made immutable in a future release.
     *
     * @param name
     * @param loc
     */
    @Deprecated
    public NamedLocation(String name, Location loc) {
        this(name, null, null, loc);
    }

    /**
     * Constructs a new NamedLocation.
     *
     * @param name the ID of the NamedLocation
     * @param player the player who's ID & name will be used as the owner, this cannot be null
     * @param loc the location this NamedLocation represents, this cannot be null
     */
    public NamedLocation(String name, OfflinePlayer player, Location loc) {
        this(name, player.getUniqueId(), player.getName(), loc);
    }

    /**
     * Constructs a new NamedLocation.
     *
     * @param name the ID of the NamedLocation
     * @param ownerID the UUID of the creating player
     * @param ownerName the name of the creating player
     * @param loc the location this NamedLocation represents, this cannot be null
     */
    public NamedLocation(String name, UUID ownerID, String ownerName, Location loc) {
        this.name = name;
        this.ownerID = ownerID;
        this.ownerName = ownerName;
        this.worldName = loc.getWorld().getName();
        this.loc = loc;
    }

    /**
     * Returns the name of the NamedLocation. For the name of the player who
     * owns this NamedLocation please see {@link #getOwnerName}.
     *
     * @return the NamedLocation's name
     */
    public String getName() {
        return name;
    }

    @Deprecated
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the stored world name
     *
     * @return the name of the world stored
     */
    public String getWorldName() {
        return worldName;
    }

    @Deprecated
    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }

    /**
     * Returns the UUID of the player who owns this NamedLocation
     *
     * @return the owners UUID
     */
    public UUID getOwnerID() {
        return ownerID;
    }

    @Deprecated
    public UUID getCreatorID() {
        return getOwnerID();
    }

    @Deprecated
    public void setCreatorID(UUID creatorID) {
        this.ownerID = creatorID;
    }

    /**
     * Obtains the name of the owner of this NamedLocation. If no owner name is available,
     * this method will fetch the name from the stored UUID.
     *
     * @return the creator of the NamedLocation
     */
    public String getOwnerName() {
        if (ownerName == null || ownerName.isEmpty()) {
            return CommandBook.server().getOfflinePlayer(ownerID).getName();
        }
        return ownerName;
    }

    @Deprecated
    public String getCreatorName() {
        return getOwnerName();
    }

    @Deprecated
    public void setCreatorName(String creatorName) {
        this.ownerName = creatorName;
    }

    /**
     * Obtains the location held by this class. Please note in a future release of CommandBook
     * this will return a copy rather than the current mutable location.
     *
     * @return the held mutable location
     */
    public Location getLocation() {
        return loc;
    }

    @Deprecated
    public void setLocation(Location loc) {
        this.loc = loc;
    }

    @Deprecated
    public void teleport(Player player) {
        loc.getChunk().load(true);
        player.teleport(loc);
    }
}
