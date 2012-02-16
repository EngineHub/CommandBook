// $Id$
/*
 * Tetsuuuu plugin for SK's Minecraft Server
 * Copyright (C) 2010 sk89q <http://www.sk89q.com>
 * All rights reserved.
*/

package com.sk89q.jinglenote;

import java.util.HashMap;
import java.util.Map;

import com.sk89q.worldedit.blocks.BlockType;
import org.spout.api.geo.World;
import org.spout.api.geo.discrete.Point;
import org.spout.api.geo.discrete.Pointm;
import org.spout.api.math.MathHelper;
import org.spout.api.player.Player;

/**
 * A manager of play instances.
 * 
 * @author sk89q
 */
public class JingleNoteManager {
    /**
     * List of instances.
     */
    protected final Map<String, JingleNotePlayer> instances
            = new HashMap<String, JingleNotePlayer>();
    
    public void play(Player player, JingleSequencer sequencer, int delay) {
        String name = player.getName();
        Point loc = findLocation(player);
        
        // Existing player found!
        if (instances.containsKey(name)) {
            JingleNotePlayer existing = instances.get(name);
            Point existingLoc = existing.getLocation();
            
            existing.stop(existingLoc.equals(loc));
            
            instances.remove(name);
        }
        
        JingleNotePlayer notePlayer = new JingleNotePlayer(player, loc, sequencer, delay);
        Thread thread = new Thread(notePlayer);
        thread.setName("JingleNotePlayer for " + player.getName());
        thread.start();
        
        instances.put(name, notePlayer);
    }
    
    public boolean stop(Player player) {
        String name = player.getName();
        
        // Existing player found!
        if (instances.containsKey(name)) {
            JingleNotePlayer existing = instances.get(name);
            existing.stop(false);
            instances.remove(name);
            return true;
        }
        return false;
    }
    
    public void stopAll() {
        for (JingleNotePlayer notePlayer : instances.values()) {
            notePlayer.stop(false);
        }
        
        instances.clear();
    }
    
    private Point findLocation(Player player) {
        World world = player.getEntity().getWorld();
        
        Pointm loc = player.getEntity().getPoint();
        loc.setY(loc.getY() - 2);
        
        if (!BlockType.canPassThrough(world.getBlockId(MathHelper.floor(loc.getX()), 
                MathHelper.floor(loc.getY()), 
                MathHelper.floor(loc.getZ())))) {
            return loc;
        }
        
        loc.setY(loc.getY() + 4);
        
        return loc;
    }
}
