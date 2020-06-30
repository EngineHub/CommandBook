package com.sk89q.commandbook.command.argument;

import org.bukkit.OfflinePlayer;

public class OfflineSinglePlayerTarget {
    private OfflinePlayer player;

    public OfflineSinglePlayerTarget(OfflinePlayer player) {
        this.player = player;
    }

    public OfflinePlayer get() {
        return player;
    }
}
