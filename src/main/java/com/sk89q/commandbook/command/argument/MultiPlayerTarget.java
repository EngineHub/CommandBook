package com.sk89q.commandbook.command.argument;

import com.google.common.collect.ImmutableList;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Iterator;

public class MultiPlayerTarget implements PlayerTarget {
    private Collection<Player> players;

    public MultiPlayerTarget(Player player) {
        this.players = ImmutableList.of(player);
    }

    public MultiPlayerTarget(Collection<Player> player) {
        this.players = player;
    }

    @Override
    public Iterator<Player> iterator() {
        return players.iterator();
    }
}
