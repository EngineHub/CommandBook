package com.sk89q.commandbook.command.argument;

import com.google.common.collect.ImmutableList;
import org.bukkit.entity.Player;

import java.util.Iterator;

public class SinglePlayerTarget implements PlayerTarget {
    private Player player;

    public SinglePlayerTarget(Player player) {
        this.player = player;
    }

    public Player get() {
        return player;
    }

    @Override
    public Iterator<Player> iterator() {
        return ImmutableList.of(player).iterator();
    }
}
