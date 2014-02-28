package com.sk89q.commandbook.util.entity.player.comparison;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class PlayerComparisonUtil {

    public static List<Player> proritySort(CommandSender source, List<Player> players) {
        if (source instanceof Player) {
            Collections.sort(players, new PlayerPrioritySort((Player) source));
        }
        return players;
    }
}
