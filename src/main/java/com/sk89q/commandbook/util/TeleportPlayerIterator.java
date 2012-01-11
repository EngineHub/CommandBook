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

package com.sk89q.commandbook.util;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.sk89q.commandbook.CommandBook;

public class TeleportPlayerIterator extends PlayerIteratorAction {
    
    protected Location loc;
    protected Location oldLoc;
    protected boolean silent;
    
    public TeleportPlayerIterator(CommandSender sender, Location loc) {
        this(sender, loc, false);
    }

    public TeleportPlayerIterator(CommandSender sender, Location loc, boolean silent) {
        super(sender);
        this.loc = loc;
        this.silent = silent;
    }
    
    @Override
    public void perform(Player player) {
        oldLoc = player.getLocation();
        player.teleport(loc);
    }
    
    @Override
    public void onCaller(Player player) {
        player.sendMessage(ChatColor.YELLOW + "Teleported.");
    }
    
    @Override
    public void onVictim(CommandSender sender, Player player) {
        if (silent)
            return;

        if (oldLoc.getWorld().equals(loc.getWorld())) {
            player.sendMessage(ChatColor.YELLOW + "You've been teleported by "
                    + PlayerUtil.toName(sender) + ".");
        } else {
            player.sendMessage(ChatColor.YELLOW + "You've been teleported by "
                    + PlayerUtil.toName(sender) + " to world '"
                    + loc.getWorld().getName() + "'.");
        }
    }
    
    @Override
    public void onInformMany(CommandSender sender, int affected) {
        sender.sendMessage(ChatColor.YELLOW.toString()
                + affected + " teleported.");
    }
}
