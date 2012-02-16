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

import org.spout.api.ChatColor;
import org.spout.api.command.CommandSource;
import org.spout.api.entity.Position;
import org.spout.api.geo.discrete.atomic.Transform;
import org.spout.api.player.Player;

public class TeleportPlayerIterator extends PlayerIteratorAction {
    
    protected final Position loc;
    protected Position oldLoc;
    protected final boolean silent;
    
    public TeleportPlayerIterator(CommandSource sender, Position loc) {
        this(sender, loc, false);
    }

    public TeleportPlayerIterator(CommandSource sender, Position loc, boolean silent) {
        super(sender);
        this.loc = loc;
        this.silent = silent;
    }
    
    @Override
    public void perform(Player player) {
        oldLoc = player.getEntity().getPosition();
        player.getEntity().setPosition(loc);
    }
    
    @Override
    public void onCaller(Player player) {
        player.sendMessage(ChatColor.YELLOW + "Teleported.");
    }
    
    @Override
    public void onVictim(CommandSource sender, Player player) {
        if (silent)
            return;

        if (oldLoc.getPosition().getWorld().equals(loc.getPosition().getWorld())) {
            player.sendMessage(ChatColor.YELLOW + "You've been teleported by "
                    + PlayerUtil.toName(sender) + ".");
        } else {
            player.sendMessage(ChatColor.YELLOW + "You've been teleported by "
                    + PlayerUtil.toName(sender) + " to world '"
                    + loc.getPosition().getWorld().getName() + "'.");
        }
    }
    
    @Override
    public void onInformMany(CommandSource sender, int affected) {
        sender.sendMessage(ChatColor.YELLOW.toString()
                + affected + " teleported.");
    }
}
