package com.sk89q.commandbook.util.entity.player;

import com.sk89q.commandbook.CommandBook;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class UUIDUtil {

    public static UUID convert(String playerName) {
        OfflinePlayer player = CommandBook.server().getOfflinePlayer(playerName);
        return player != null ? player.getUniqueId() : null;
    }

    public static String toUniqueString(CommandSender sender) {
        if (sender instanceof Player) {
            return ((Player) sender).getUniqueId().toString();
        }
        return sender.getName();
    }
}
