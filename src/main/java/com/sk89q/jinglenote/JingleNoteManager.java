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


package com.sk89q.jinglenote;

import java.util.HashMap;
import java.util.Map;

import com.sk89q.jinglenote.bukkit.BukkitJingleNotePlayer;

/**
 * A manager of play instances.
 *
 * @author sk89q
 */
public class JingleNoteManager {

    /**
     * List of instances.
     */
    protected final Map<String, JingleNotePlayer> instances = new HashMap<String, JingleNotePlayer>();

    public void play(String player, JingleSequencer sequencer) {

        // Existing player found!
        if (instances.containsKey(player)) {
            JingleNotePlayer existing = instances.get(player);
            existing.stop();
            instances.remove(player);
        }

        JingleNotePlayer notePlayer = new BukkitJingleNotePlayer(player, sequencer);
        Thread thread = new Thread(notePlayer);
        thread.setDaemon(true);
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.setName("JingleNotePlayer for " + player);
        thread.start();

        instances.put(player, notePlayer);
    }

    public boolean stop(String player) {

        // Existing player found!
        if (instances.containsKey(player)) {
            JingleNotePlayer existing = instances.get(player);
            existing.stop();
            instances.remove(player);
            return true;
        }
        return false;
    }

    public void stopAll() {

        for (JingleNotePlayer notePlayer : instances.values()) {
            notePlayer.stop();
        }

        instances.clear();
    }
}