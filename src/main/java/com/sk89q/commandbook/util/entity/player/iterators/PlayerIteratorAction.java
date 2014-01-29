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

package com.sk89q.commandbook.util.entity.player.iterators;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Performs an action over a list of players.
 * 
 * @author sk89q
 */
public abstract class PlayerIteratorAction {

    protected final CommandSender sender;
    
    private boolean included;
    private int affected = 0;
    
    /**
     * Construct the object.
     *
     * @param sender
     */
    public PlayerIteratorAction(CommandSender sender) {
        this.sender = sender;
    }
    
    /**
     * Iterate over a list of players and perform the action on the player.
     * 
     * @param targets
     */
    public void iterate(Iterable<Player> targets) {
        for (Player player : targets) {
            perform(player);
            
            // Tell the user
            if (player.equals(sender)) {
                onCaller(player);
                
                // Keep track of this
                included = true;
            } else {
                onVictim(sender, player);
            }
            
            affected++;
        }
        
        if (!included) {
            onInform(sender, affected);
            onInformMany(sender, affected);
        } else if (affected > 1) {
            onInformMany(sender, affected);
        }
        
        onComplete(sender, affected);
    }
    
    /**
     * Get the sender.
     * 
     * @return
     */
    public CommandSender getSender() {
        return sender;
    }
    
    /**
     * Perform the action.
     * 
     * @param player
     */
    public abstract void perform(Player player);
    
    /**
     * Called when the caller is affected by the action.
     * 
     * @param player
     */
    public abstract void onCaller(Player player);
    
    /**
     * Called when a player is a victim and is not the caller.
     * 
     * @param sender 
     * @param player
     */
    public abstract void onVictim(CommandSender sender, Player player);
    
    /**
     * Called on informing of the sender of the action. This is only called
     * if the sender was also not a victim.
     * 
     * @param sender
     * @param affected 
     */
    public void onInform(CommandSender sender, int affected) {
    }
    
    /**
     * Called on operation complete if more than one player was affected and the
     * caller was also not affected or the caller was console.
     * 
     * @param sender
     * @param affected 
     */
    public void onInformMany(CommandSender sender, int affected) {
    }
    
    /**
     * Called on operation complete.
     * 
     * @param sender
     * @param affected 
     */
    public void onComplete(CommandSender sender, int affected) {
    }
}
