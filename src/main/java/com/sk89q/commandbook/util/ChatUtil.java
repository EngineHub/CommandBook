package com.sk89q.commandbook.util;

import com.sk89q.commandbook.CommandBook;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatUtil {

    /**
     * Send a complex message properly.
     *
     * @param sender
     * @param message
     */
    public static void sendMessage(CommandSender sender, String message) {
        for (String line : message.split("\n")) {
            sender.sendMessage(line.replaceAll("[\r\n]", ""));
        }
    }

    /**
     * Gets the name of a command sender. This may be a display name.
     *
     * @param sender
     * @return
     */
    public static String toName(CommandSender sender) {
        return ChatColor.stripColor(toColoredName(sender, null));
    }

    /**
     * Gets the name of a command sender. This may be a display name.
     *
     * @param sender
     * @param endColor
     * @return
     */
    public static String toColoredName(CommandSender sender, ChatColor endColor) {
        if (sender instanceof Player) {
            String name = CommandBook.inst().useDisplayNames
                    ? ((Player) sender).getDisplayName()
                    : (sender).getName();
            if (endColor != null && name.contains("\u00A7")) {
                name = name + endColor;
            }
            return name;
        } else if (sender instanceof ConsoleCommandSender) {
            return "*Console*";
        } else {
            return sender.getName();
        }
    }

    /**
     * Gets the name of a command sender. This is a unique name and this
     * method should never return a "display name".
     *
     * @param sender
     * @return
     */
    public static String toUniqueName(CommandSender sender) {
        if (sender instanceof Player) {
            return (sender).getName();
        } else {
            return "*Console*";
        }
    }

    public static String toFriendlyString(Location location) {
        return location.getBlockX() + "," +
                location.getBlockY() + "," +
                location.getBlockZ() + "@" +
                location.getWorld().getName();
    }

    /**
     * Replace macros in the text.
     *
     * @param sender
     * @param message
     * @return
     */
    public static String replaceMacros(CommandSender sender, String message) {
        Player[] online = CommandBook.server().getOnlinePlayers();

        message = message.replace("%name%", toName(sender));
        message = message.replace("%cname%", toColoredName(sender, null));
        message = message.replace("%id%", toUniqueName(sender));
        message = message.replace("%online%", String.valueOf(online.length));

        // Don't want to build the list unless we need to
        if (message.contains("%players%")) {
            message = message.replace("%players%", ServerUtil.getOnlineList(online, null));
        }

        if (sender instanceof Player) {
            Player player = (Player) sender;
            World world = player.getWorld();

            message = message.replace("%time%", getTimeString(world.getTime()));
            message = message.replace("%world%", world.getName());
        }

        final Pattern cmdPattern = Pattern.compile("%cmd:([^%]+)%");
        final Matcher matcher = cmdPattern.matcher(message);
        try {
            StringBuffer buff = new StringBuffer();
            while (matcher.find()) {
                Process p = new ProcessBuilder(matcher.group(1).split(" ")).start();
                BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String s;
                StringBuilder build = new StringBuilder();
                while ((s = stdInput.readLine()) != null) {
                    build.append(s).append(" ");
                }
                stdInput.close();
                build.delete(build.length() - 1, build.length());
                matcher.appendReplacement(buff, build.toString());
                p.destroy();
            }
            matcher.appendTail(buff);
            message = buff.toString();
        } catch (IOException e) {
            sender.sendMessage(ChatColor.RED + "Error replacing macros: " + e.getMessage());
        }
        return message;
    }

    /**
     * Replace color macros in a string. The macros are in the form of `[char]
     * where char represents the color. R is for red, Y is for yellow,
     * G is for green, C is for cyan, B is for blue, and P is for purple.
     * The uppercase versions of those are the darker shades, while the
     * lowercase versions are the lighter shades. For white, it's 'w', and
     * 0-2 are black, dark grey, and grey, respectively.
     *
     * @param str
     * @return color-coded string
     */
    public static String replaceColorMacros(String str) {
        str = str.replace("`r", ChatColor.RED.toString());
        str = str.replace("`R", ChatColor.DARK_RED.toString());

        str = str.replace("`y", ChatColor.YELLOW.toString());
        str = str.replace("`Y", ChatColor.GOLD.toString());

        str = str.replace("`g", ChatColor.GREEN.toString());
        str = str.replace("`G", ChatColor.DARK_GREEN.toString());

        str = str.replace("`c", ChatColor.AQUA.toString());
        str = str.replace("`C", ChatColor.DARK_AQUA.toString());

        str = str.replace("`b", ChatColor.BLUE.toString());
        str = str.replace("`B", ChatColor.DARK_BLUE.toString());

        str = str.replace("`p", ChatColor.LIGHT_PURPLE.toString());
        str = str.replace("`P", ChatColor.DARK_PURPLE.toString());

        str = str.replace("`0", ChatColor.BLACK.toString());
        str = str.replace("`1", ChatColor.DARK_GRAY.toString());
        str = str.replace("`2", ChatColor.GRAY.toString());
        str = str.replace("`w", ChatColor.WHITE.toString());

        // use mojang's symbols where we can, make new ones up when they are already used
        str = str.replace("`k", ChatColor.MAGIC.toString());

        str = str.replace("`l", ChatColor.BOLD.toString());
        str = str.replace("`m", ChatColor.STRIKETHROUGH.toString());
        str = str.replace("`n", ChatColor.UNDERLINE.toString());
        str = str.replace("`o", ChatColor.ITALIC.toString());

        str = str.replace("`x", ChatColor.RESET.toString());

        return str;
    }

    /**
     * Get the 24-hour time string for a given Minecraft time.
     *
     * @param time
     * @return
     */
    public static String getTimeString(long time) {
        int hours = (int) ((time / 1000 + 8) % 24);
        int minutes = (int) (60 * (time % 1000) / 1000);
        return String.format("%02d:%02d (%d:%02d %s)",
                hours, minutes, (hours % 12) == 0 ? 12 : hours % 12, minutes,
                hours < 12 ? "am" : "pm");
    }
}
