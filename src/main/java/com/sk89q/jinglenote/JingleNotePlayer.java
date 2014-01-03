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

import com.sk89q.jinglenote.JingleSequencer.Note;

public abstract class JingleNotePlayer implements Runnable {

    protected final String player;
    protected JingleSequencer sequencer;

    /**
     * Constructs a new JingleNotePlayer
     * 
     * @param player The player who is hearing this's name.
     * @param seq The JingleSequencer to play.
     * @param area The SearchArea for this player. (optional)
     */
    public JingleNotePlayer(String player, JingleSequencer seq) {
        this.player = player;
        sequencer = seq;
    }

    @Override
    public void run() {

        if(sequencer == null)
            return;
        try {
            try {
                sequencer.run(this);
            } catch (Throwable t) {
                t.printStackTrace();
            }

            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            sequencer.stop();
            sequencer = null;
        }
    }

    public String getPlayer() {
        return player;
    }

    public void stop() {
        if (sequencer != null) {
            sequencer.stop();
        }
    }

    public abstract void play(Note note);
}