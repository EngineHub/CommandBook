package com.sk89q.commandbook.util;

import com.sk89q.commandbook.CommandBook;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ServerUtil {

    public static String getOnlineList(Player[] online) {
        return getOnlineList(online, null);
    }

    /**
     * Returns a comma-delimited list of players.
     *
     * @param online
     * @param color
     * @return
     */
    public static String getOnlineList(Player[] online, ChatColor color) {
        StringBuilder out = new StringBuilder();

        // To keep track of commas
        boolean first = true;

        for (Player player : online) {
            if (!first) {
                out.append(", ");
            }

            if (CommandBook.inst().useDisplayNames) {
                out.append(player.getDisplayName());
            } else {
                out.append(player.getName());
            }

            if (color != null) {
                out.append(color);
            }

            first = false;
        }

        return out.toString();
    }

    /**
     * Gets the IP address of a command sender.
     *
     * @param sender The sender to get an address for
     * @return The address string of the sender
     */
    public static String toInetAddressString(CommandSender sender) {
        if (sender instanceof Player) {
            return ((Player) sender).getAddress().getAddress().getHostAddress();
        } else {
            return "127.0.0.1";
        }
    }
}
