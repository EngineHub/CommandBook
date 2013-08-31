// $Id$
/*
 * Copyright (C) 2010, 2011 sk89q <http://www.sk89q.com>
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

package com.sk89q.commandbook;

import com.sk89q.commandbook.util.ItemUtil;
import com.sk89q.commandbook.util.PlayerUtil;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.worldedit.blocks.BlockType;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.sk89q.commandbook.util.PlayerUtil.*;

/**
 * Utility methods for CommandBook, borrowed from Tetsuuuu (the plugin
 * for SK's server).
 *
 * @author sk89q
 */
public class CommandBookUtil {
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
     * Get the cardinal compass direction of a player.
     *
     * @param player
     * @return
     */
    public static String getCardinalDirection(Player player) {
        double rot = (player.getLocation().getYaw() - 90) % 360;
        if (rot < 0) {
            rot += 360.0;
        }
        return getDirection(rot);
    }

    /**
     * Converts a rotation to a cardinal direction name.
     *
     * @param rot
     * @return
     */
    private static String getDirection(double rot) {
        if (0 <= rot && rot < 22.5) {
            return "West";
        } else if (22.5 <= rot && rot < 67.5) {
            return "Northwest";
        } else if (67.5 <= rot && rot < 112.5) {
            return "North";
        } else if (112.5 <= rot && rot < 157.5) {
            return "Northeast";
        } else if (157.5 <= rot && rot < 202.5) {
            return "East";
        } else if (202.5 <= rot && rot < 247.5) {
            return "Southeast";
        } else if (247.5 <= rot && rot < 292.5) {
            return "South";
        } else if (292.5 <= rot && rot < 337.5) {
            return "Southwest";
        } else if (337.5 <= rot && rot < 360.0) {
            return "West";
        } else {
            return null;
        }
    }

    /**
     * Process an item give request.
     *
     * @param sender
     * @param item
     * @param amt
     * @param targets
     * @param component
     * @param drop
     * @throws CommandException
     */
    @SuppressWarnings("deprecation")
    public static void giveItem(CommandSender sender, ItemStack item, int amt,
            Iterable<Player> targets, InventoryComponent component, boolean drop, boolean overrideStackSize)
            throws CommandException {

        boolean included = false; // Is the command sender also receiving items?

        int maxStackSize = overrideStackSize ? 64 : item.getType().getMaxStackSize();

        component.checkAllowedItem(sender, item.getTypeId(), item.getDurability());

        // Check for invalid amounts
        if (amt == 0 || amt < -1) {
            throw new CommandException("Invalid item amount!");
        } else if (amt == -1) {
            // Check to see if the player can give infinite items
            CommandBook.inst().checkPermission(sender, "commandbook.give.infinite");
        } else if (overrideStackSize) {
            CommandBook.inst().checkPermission(sender, "commandbook.override.maxstacksize");
        } else if (amt > maxStackSize * 5) {
            // Check to see if the player can give stacks of this size
            if (!CommandBook.inst().hasPermission(sender, "commandbook.give.stacks.unlimited")) {
                throw new CommandException("More than 5 stacks is too excessive.");
            }
        } else if (amt > maxStackSize /* && amt < max * 5 */) {
            // Check to see if the player can give stacks
            CommandBook.inst().checkPermission(sender, "commandbook.give.stacks");
        }

        if(amt > 2240 && !drop) amt = 2240;

        // Get a nice amount name
        String amtText = amt == -1 ? "an infinite stack of" : String.valueOf(amt);

        for (Player player : targets) {
            int left = amt;

            // Give individual stacks
            while (left > 0 || amt == -1) {
                int givenAmt = Math.min(maxStackSize, left);
                item.setAmount(givenAmt);
                left -= givenAmt;

                // The -d flag drops the items naturally on the ground instead
                // of directly giving the player the item
                if (drop) {
                    player.getWorld().dropItemNaturally(player.getLocation(), item);
                } else {
                    player.getInventory().addItem(item);
                }

                if (amt == -1) {
                    break;
                }
            }

            // workaround for having inventory open while giving items (eg TMI mod)
            player.updateInventory();

            // Tell the user about the given item
            if (player.equals(sender)) {
                player.sendMessage(ChatColor.YELLOW + "You've been given " + amtText + " "
                        + ItemUtil.toItemName(item.getTypeId()) + ".");

                // Keep track of this
                included = true;
            } else {
                player.sendMessage(ChatColor.YELLOW + "Given from "
                        + PlayerUtil.toColoredName(sender, ChatColor.YELLOW) + ": "
                        + amtText + " "
                        + ItemUtil.toItemName(item.getTypeId()) + ".");

            }
        }

        // The player didn't receive any items, then we need to send the
        // user a message so s/he know that something is indeed working
        if (!included) {
            sender.sendMessage(ChatColor.YELLOW.toString() + amtText + " "
                    + ItemUtil.toItemName(item.getTypeId()) + " has been given.");
        }
    }

    /**
     * Process an item give request.
     *
     * @param sender
     * @param item
     * @param amt
     * @param target
     * @throws CommandException
     */
    public static void takeItem(CommandSender sender, ItemStack item, int amt,
            Player target)
            throws CommandException {

        // Check for invalid amounts
        if (amt <= 0) {
            throw new CommandException("Invalid item amount!");
        }


        item.setAmount(amt);
        if (target.getInventory().contains(item.getTypeId())) {
            target.getInventory().removeItem(item);

            target.sendMessage(ChatColor.YELLOW + "Taken from "
                            + PlayerUtil.toColoredName(sender, ChatColor.YELLOW) + ": "
                            + amt + " "
                            + ItemUtil.toItemName(item.getTypeId()) + ".");

            sender.sendMessage(ChatColor.YELLOW.toString() + amt + " "
                        + ItemUtil.toItemName(item.getTypeId()) + " has been taken.");
        } else {
            sender.sendMessage(ChatColor.YELLOW.toString() + target.getName()
                    + " has no " + ItemUtil.toItemName(item.getTypeId()) + ".");
        }
    }

    /**
     * Find a position for the player to stand that is not inside a block.
     * Blocks above the player will be iteratively tested until there is
     * a series of two free blocks. The player will be teleported to
     * that free position.
     *
     * @param searchPos search position
     * @return
     */
    public static Location findFreePosition(Location searchPos) {
        World world = searchPos.getWorld();
        Location loc = searchPos.clone();
        int x = searchPos.getBlockX();
        int y = Math.max(0, searchPos.getBlockY());
        int origY = y;
        int z = searchPos.getBlockZ();

        byte free = 0;

        while (y <= world.getMaxHeight() + 2) {
            if (BlockType.canPassThrough(world.getBlockTypeIdAt(x, y, z))) {
                free++;
            } else {
                free = 0;
            }

            if (free == 2) {
                if (y - 1 != origY) {
                    loc.setX(x + 0.5);
                    loc.setY(y - 1);
                    loc.setZ(z + 0.5);
                }

                return loc;
            }

            y++;
        }

        return null;
    }

    /**
     * Send an arrow from a player eye level.
     *
     * @param player
     * @param dir
     * @param speed
     */
    public static void sendArrowFromPlayer(Player player,
            Vector dir, float speed) {
        Location loc = player.getEyeLocation();
        Vector actualDir = dir.clone().normalize();
        Vector finalVecLoc = loc.toVector().add(actualDir.multiply(2));
        loc.setX(finalVecLoc.getX());
        loc.setY(finalVecLoc.getY());
        loc.setZ(finalVecLoc.getZ());
        Arrow arrow = player.getWorld().spawn(loc, Arrow.class);
        arrow.setShooter(player);
        arrow.setVelocity(dir.multiply(speed));
    }

    /**
     * Send fireballs from a player eye level.
     *
     * @param player
     * @param amt number of fireballs to shoot (evenly spaced)
     */
    public static void sendFireballsFromPlayer(Player player, int amt) {
        Location loc = player.getEyeLocation();
        final double tau = 2 * Math.PI;
        double arc = tau / amt;
        for (double a = 0; a < tau; a += arc) {
            Vector dir = new Vector(Math.cos(a), 0, Math.sin(a));
            Location spawn = loc.toVector().add(dir.multiply(2)).toLocation(loc.getWorld(), 0.0F, 0.0F);
            Fireball fball = player.getWorld().spawn(spawn, Fireball.class);
            fball.setShooter(player);
            fball.setDirection(dir.multiply(10));
        }
    }

    public static void sendCannonToPlayer(Player player) {
    	Location loc = player.getEyeLocation();
    	loc.setX(loc.getX());
    	loc.setY(loc.getY());
    	loc.setZ(loc.getZ());
    	player.getWorld().spawn(loc, Fireball.class);
    }

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
     * Expand a stack of items.
     *
     * @param item
     * @param infinite
     */
    public static void expandStack(ItemStack item, boolean infinite, boolean overrideStackSize) {
        if (item == null || item.getAmount() == 0 || item.getTypeId() <= 0) {
            return;
        }

        int stackSize = overrideStackSize ? 64 : item.getType().getMaxStackSize();

        if (item.getType().getMaxStackSize() == 1) {
            return;
        }

        if (infinite) {
            item.setAmount(-1);
        } else if (item.getAmount() < stackSize){
            item.setAmount(stackSize);
        }
    }

    public static World.Environment getSkylandsEnvironment() {
        try {
            return World.Environment.THE_END;
        } catch (Throwable t) {
            return World.Environment.getEnvironment(1);
        }
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
            message = message.replace("%players%",
                    CommandBookUtil.getOnlineList(online, null));
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

    public static <T, K, V> Map<K, V> getNestedMap(Map<T, Map<K, V>> source, T key) {
        Map<K,V> value = source.get(key);
        if (value == null) {
            value = new HashMap<K, V>();
            source.put(key, value);
        }
        return value;
    }

    public static <T, V> Set<V> getNestedSet(Map<T, Set<V>> source, T key) {
        Set<V> value = source.get(key);
        if (value == null) {
            value = new HashSet<V>();
            source.put(key, value);
        }
        return value;
    }

    public static <T, V> List<V> getNestedList(Map<T, List<V>> source, T key) {
        List<V> value = source.get(key);
        if (value == null) {
            value = new ArrayList<V>();
            source.put(key, value);
        }
        return value;
    }

    public static long matchDate(String filter) throws CommandException {
        if (filter == null) return 0L;
        if (filter.equalsIgnoreCase("now")) return System.currentTimeMillis();
        String[] groupings = filter.split("-");
        if (groupings.length == 0) throw new CommandException("Invalid date specified");
        Calendar cal = new GregorianCalendar();
        cal.setTimeInMillis(0);
        for (String str : groupings) {
            int type;
            switch (str.charAt(str.length() - 1)) {
                case 'm':
                    type = Calendar.MINUTE;
                    break;
                case 'h':
                    type = Calendar.HOUR;
                    break;
                case 'd':
                    type = Calendar.DATE;
                    break;
                case 'w':
                    type = Calendar.WEEK_OF_YEAR;
                    break;
                case 'y':
                    type = Calendar.YEAR;
                    break;
                default:
                    throw new CommandException("Unknown date value specified");
            }
            cal.add(type, Integer.valueOf(str.substring(0, str.length() -1)));
        }
        return cal.getTimeInMillis();
    }

    public static long matchFutureDate(String filter) throws CommandException {
        return matchDate(filter) + System.currentTimeMillis();
    }
}
