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


package com.sk89q.commandbook.jinglenote;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.enginehub.jinglenote.JingleNotePlayer;
import org.enginehub.jinglenote.bukkit.BukkitJingleNotePlayer;
import org.enginehub.jinglenote.sequencer.JingleSequencer;

/**
 * A manager of play instances.
 *
 * @author sk89q
 */
public class JingleNoteManager {
    /**
     * List of instances.
     */
    protected final Map<UUID, JingleNotePlayer> instances = new HashMap<>();

    public void play(Player player, JingleSequencer sequencer) {
        UUID playerID = player.getUniqueId();

        // Existing player found!
        if (instances.containsKey(playerID)) {
            JingleNotePlayer existing = instances.get(playerID);
            existing.stop();
            instances.remove(playerID);
        }

        JingleNotePlayer notePlayer = new BukkitJingleNotePlayer(player, sequencer);
        Thread thread = new Thread(notePlayer::play);
        thread.setDaemon(true);
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.setName("JingleNotePlayer for " + playerID);
        thread.start();

        instances.put(playerID, notePlayer);
    }

    public boolean stop(UUID playerID) {
        // Existing player found!
        if (instances.containsKey(playerID)) {
            JingleNotePlayer existing = instances.get(playerID);
            existing.stop();
            instances.remove(playerID);
            return true;
        }

        return false;
    }

    public boolean stop(Player player) {
        return stop(player.getUniqueId());
    }

    public void stopAll() {
        for (JingleNotePlayer notePlayer : instances.values()) {
            notePlayer.stop();
        }

        instances.clear();
    }
}