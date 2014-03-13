package com.sk89q.commandbook.util.entity.player.comparison;

import org.bukkit.entity.Player;

import java.util.Comparator;

public class PlayerPrioritySort implements Comparator<Player> {

    private Player sender;

    public PlayerPrioritySort(Player sender) {
        this.sender = sender;
    }

    @Override
    public int compare(Player o1, Player o2) {
        // If they are the same return such
        if (o1.equals(o2)) {
            return 0;
        }
        // If the sender is player 1 return 1
        // to push it after player 2, otherwise
        // sort by name
        if (sender.equals(o1)) {
            return 1;
        } else {
            return o1.getName().compareToIgnoreCase(o2.getName());
        }
    }
}
