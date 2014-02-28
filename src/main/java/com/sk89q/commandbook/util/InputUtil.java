package com.sk89q.commandbook.util;

import com.google.common.collect.Lists;
import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.locations.*;
import com.sk89q.minecraft.util.commands.CommandException;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.sk89q.commandbook.util.entity.player.PlayerUtil.checkPlayer;

public class InputUtil {

    public static class TimeParser {

        /**
         * A pattern that matches time given in 12-hour form (xx:xx(am|pm))
         */
        protected static final Pattern TWELVE_HOUR_TIME = Pattern.compile("^([0-9]+(?::[0-9]+)?)([apmAPM\\.]+)$");

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

        /**
         * Parse a time string into the MC world time.
         *
         * @param timeStr the time string to parse
         * @return the world time
         * @throws com.sk89q.minecraft.util.commands.CommandException
         */
        public static int matchMCWorldTime(String timeStr) throws CommandException {
            Matcher matcher;

            try {
                int time = Integer.parseInt(timeStr);

                // People tend to enter just a number of the hour
                if (time <= 24) {
                    return ((time - 8) % 24) * 1000;
                }

                return time;
            } catch (NumberFormatException e) {
                // Not an integer!
            }

            // Tick time
            if (timeStr.matches("^*[0-9]+$")) {
                return Integer.parseInt(timeStr.substring(1));

                // Allow 24-hour time
            } else if (timeStr.matches("^[0-9]+:[0-9]+$")) {
                String[] parts = timeStr.split(":");
                int hours = Integer.parseInt(parts[0]);
                int mins = Integer.parseInt(parts[1]);
                return (int) (((hours - 8) % 24) * 1000
                        + Math.round((mins % 60) / 60.0 * 1000));

                // Or perhaps 12-hour time
            } else if ((matcher = TWELVE_HOUR_TIME.matcher(timeStr)).matches()) {
                String time = matcher.group(1);
                String period = matcher.group(2);
                int shift;

                if (period.equalsIgnoreCase("am")
                        || period.equalsIgnoreCase("a.m.")) {
                    shift = 0;
                } else if (period.equalsIgnoreCase("pm")
                        || period.equalsIgnoreCase("p.m.")) {
                    shift = 12;
                } else {
                    throw new CommandException("'am' or 'pm' expected, got '"
                            + period + "'.");
                }

                String[] parts = time.split(":");
                int hours = Integer.parseInt(parts[0]);
                int mins = parts.length >= 2 ? Integer.parseInt(parts[1]) : 0;
                return (int) ((((hours % 12) + shift - 8) % 24) * 1000
                        + (mins % 60) / 60.0 * 1000);

                // Or some shortcuts
            } else if (timeStr.equalsIgnoreCase("dawn")) {
                return (6 - 8 + 24) * 1000;
            } else if (timeStr.equalsIgnoreCase("sunrise")) {
                return (7 - 8 + 24) * 1000;
            } else if (timeStr.equalsIgnoreCase("morning")) {
                return (24) * 1000;
            } else if (timeStr.equalsIgnoreCase("day")) {
                return (24) * 1000;
            } else if (timeStr.equalsIgnoreCase("midday")
                    || timeStr.equalsIgnoreCase("noon")) {
                return (12 - 8 + 24) * 1000;
            } else if (timeStr.equalsIgnoreCase("afternoon")) {
                return (14 - 8 + 24) * 1000;
            } else if (timeStr.equalsIgnoreCase("evening")) {
                return (16 - 8 + 24) * 1000;
            } else if (timeStr.equalsIgnoreCase("sunset")) {
                return (21 - 8 + 24) * 1000;
            } else if (timeStr.equalsIgnoreCase("dusk")) {
                return (21 - 8 + 24) * 1000 + (int) (30 / 60.0 * 1000);
            } else if (timeStr.equalsIgnoreCase("night")) {
                return (22 - 8 + 24) * 1000;
            } else if (timeStr.equalsIgnoreCase("midnight")) {
                return (0 - 8 + 24) * 1000;
            }

            throw new CommandException("Time input format unknown.");
        }
    }

    public static class PlayerParser {

        /**
         * Match player names.
         *
         * @param source
         * @param filter
         * @return
         */
        public static List<Player> matchPlayerNames(CommandSender source, String filter) {

            Player[] players = CommandBook.server().getOnlinePlayers();
            boolean useDisplayNames = CommandBook.inst().lookupWithDisplayNames;

            filter = filter.toLowerCase();

            // Allow exact name matching
            if (filter.charAt(0) == '@' && filter.length() >= 2) {
                filter = filter.substring(1);

                for (Player player : players) {
                    if (player.getName().equalsIgnoreCase(filter)
                            || (useDisplayNames
                            && ChatColor.stripColor(player.getDisplayName()).equalsIgnoreCase(filter))) {
                        return Lists.newArrayList(player);
                    }
                }

                return new ArrayList<Player>();
                // Allow partial name matching
            } else if (filter.charAt(0) == '*' && filter.length() >= 2) {
                filter = filter.substring(1);

                List<Player> list = new ArrayList<Player>();

                for (Player player : players) {
                    if (player.getName().toLowerCase().contains(filter)
                            || (useDisplayNames
                            && ChatColor.stripColor(player.getDisplayName().toLowerCase()).contains(filter))) {
                        list.add(player);
                    }
                }

                return list;

                // Start with name matching
            } else {
                List<Player> list = new ArrayList<Player>();

                for (Player player : players) {
                    if (player.getName().toLowerCase().startsWith(filter)
                            || (useDisplayNames
                            && ChatColor.stripColor(player.getDisplayName().toLowerCase()).startsWith(filter))) {
                        // Do this to maintain the behavior of the deprecated version of this method
                        if (source != null) {
                            if (player.equals(source)) {
                                list.add(player);
                            } else {
                                list.add(0, player);
                            }
                        } else {
                            list.add(player);
                        }
                    }
                }

                return list;
            }
        }

        /**
         * Checks permissions and throws an exception if permission is not met.
         *
         * @param source
         * @param filter
         * @return iterator for players
         * @throws CommandException no matches found
         */
        public static List<Player> matchPlayers(CommandSender source, String filter) throws CommandException {

            if (CommandBook.server().getOnlinePlayers().length == 0) {
                throw new CommandException("No players matched query.");
            }

            if (filter.equals("*")) {
                CommandBook.inst().checkPermission(source, "commandbook.targets.everyone");
                return checkPlayerMatch(Arrays.asList(CommandBook.server().getOnlinePlayers()));
            }

            // Handle special hash tag groups
            if (filter.charAt(0) == '#') {
                // Handle #world, which matches player of the same world as the
                // calling source
                if (filter.equalsIgnoreCase("#world")) {
                    List<Player> players = new ArrayList<Player>();
                    Player sourcePlayer = checkPlayer(source);
                    World sourceWorld = sourcePlayer.getWorld();
                    CommandBook.inst().checkPermission(source, "commandbook.targets.world." + sourceWorld.getName());

                    for (Player player : CommandBook.server().getOnlinePlayers()) {
                        if (player.getWorld().equals(sourceWorld)) {
                            players.add(player);
                        }
                    }

                    return checkPlayerMatch(players);

                    // Handle #near, which is for nearby players.
                } else if (filter.equalsIgnoreCase("#near")) {
                    CommandBook.inst().checkPermission(source, "commandbook.targets.near");
                    List<Player> players = new ArrayList<Player>();
                    Player sourcePlayer = checkPlayer(source);
                    World sourceWorld = sourcePlayer.getWorld();
                    org.bukkit.util.Vector sourceVector
                            = sourcePlayer.getLocation().toVector();

                    for (Player player : CommandBook.server().getOnlinePlayers()) {
                        if (player.getWorld().equals(sourceWorld)
                                && player.getLocation().toVector().distanceSquared(
                                sourceVector) < 900) { // 30 * 30
                            players.add(player);
                        }
                    }

                    return checkPlayerMatch(players);

                } else {
                    throw new CommandException("Invalid group '" + filter + "'.");
                }
            }

            List<Player> players = matchPlayerNames(source, filter);

            return checkPlayerMatch(players);
        }

        /**
         * Match a single player exactly.
         *
         * @param sender
         * @param filter
         * @return
         * @throws CommandException
         */
        public static Player matchPlayerExactly(CommandSender sender, String filter) throws CommandException {
            Player[] players = CommandBook.server().getOnlinePlayers();
            for (Player player : players) {
                if (player.getName().equalsIgnoreCase(filter)
                        || (CommandBook.inst().lookupWithDisplayNames
                        && player.getDisplayName().equalsIgnoreCase(filter))) {
                    return player;
                }
            }

            throw new CommandException("No player found!");
        }

        /**
         * Match only a single player.
         *
         * @param sender
         * @param filter
         * @return
         * @throws CommandException
         */
        public static Player matchSinglePlayer(CommandSender sender, String filter) throws CommandException {
            return checkSinglePlayerMatch(matchPlayers(sender, filter));
        }

        /**
         * Match only a single player or console.
         *
         * @param sender
         * @param filter
         * @return
         * @throws CommandException
         */
        public static CommandSender matchPlayerOrConsole(CommandSender sender, String filter) throws CommandException {

            // Let's see if console is wanted
            if (filter.equalsIgnoreCase("#console")
                    || filter.equalsIgnoreCase("*console*")
                    || filter.equalsIgnoreCase("!")) {
                return CommandBook.server().getConsoleSender();
            }

            return matchSinglePlayer(sender, filter);
        }

        /**
         * Checks if the given list of players is greater than size 0, otherwise
         * throw an exception.
         *
         * @param players
         * @return
         * @throws CommandException
         */
        public static List<Player> checkPlayerMatch(List<Player> players) throws CommandException {
            // Check to see if there were any matches
            if (players.isEmpty()) {
                throw new CommandException("No players matched query.");
            }
            return players;
        }

        /**
         * Checks if the given list of players contains only one player, otherwise
         * throw an exception.
         *
         * @param players
         * @return
         * @throws CommandException
         */
        public static Player checkSinglePlayerMatch(List<Player> players) throws CommandException {
            if (players.size() > 1) {
                throw new CommandException("More than one player found! Use @<name> for exact matching.");
            }
            return players.get(0);
        }
    }

    /**
     * Match a world.
     * @param sender
     *
     * @param filter
     * @return
     * @throws com.sk89q.minecraft.util.commands.CommandException
     */
    public static World matchWorld(CommandSender sender, String filter) throws CommandException {
        List<World> worlds = CommandBook.server().getWorlds();

        // Handle special hash tag groups
        if (filter.charAt(0) == '#') {
            // #main for the main world
            if (filter.equalsIgnoreCase("#main")) {
                return worlds.get(0);

                // #normal for the first normal world
            } else if (filter.equalsIgnoreCase("#normal")) {
                for (World world : worlds) {
                    if (world.getEnvironment() == World.Environment.NORMAL) {
                        return world;
                    }
                }

                throw new CommandException("No normal world found.");

                // #nether for the first nether world
            } else if (filter.equalsIgnoreCase("#nether")) {
                for (World world : worlds) {
                    if (world.getEnvironment() == World.Environment.NETHER) {
                        return world;
                    }
                }

                throw new CommandException("No nether world found.");

                // #theend for the first end world
            } else if (filter.equalsIgnoreCase("#theend") || filter.equalsIgnoreCase("#end")) {
                for (World world : worlds) {
                    if (world.getEnvironment() == World.Environment.THE_END) {
                        return world;
                    }
                }

                throw new CommandException("No end world found.");
                // Handle getting a world from a player
            } else if (filter.matches("^#player$")) {
                String parts[] = filter.split(":", 2);

                // They didn't specify an argument for the player!
                if (parts.length == 1) {
                    throw new CommandException("Argument expected for #player.");
                }

                return PlayerParser.matchPlayers(sender, parts[1]).iterator().next().getWorld();
            } else {
                throw new CommandException("Invalid identifier '" + filter + "'.");
            }
        }

        for (World world : worlds) {
            if (world.getName().equals(filter)) {
                return world;
            }
        }

        throw new CommandException("No world by that exact name found.");
    }

    /**
     * Match a target.
     *
     * @param source
     * @param filter
     * @return first result of matchLocations
     * @throws CommandException no matches found
     */
    public static Location matchLocation(CommandSender source, String filter)
            throws CommandException {

        return matchLocations(source, filter, true).get(0);
    }

    /**
     * Match multiple targets.
     *
     * @param source
     * @param filter
     * @return list of locations
     * @throws CommandException no matches found
     */
    public static List<Location> matchLocations(CommandSender source, String filter)
            throws CommandException {

        return matchLocations(source, filter, false);
    }

    private static List<Location> matchLocations(CommandSender source, String filter,
                                                 boolean strictMatching)
            throws CommandException {

        // Handle coordinates
        if (filter.matches("^[\\-0-9\\.]+,[\\-0-9\\.]+,[\\-0-9\\.]+(?:.+)?$")) {
            CommandBook.inst().checkPermission(source, "commandbook.locations.coords");

            String[] args = filter.split(":");
            String[] parts = args[0].split(",");
            double x, y, z;

            try {
                x = Double.parseDouble(parts[0]);
                y = Double.parseDouble(parts[1]);
                z = Double.parseDouble(parts[2]);
            } catch (NumberFormatException e) {
                throw new CommandException("Coordinates expected numbers!");
            }

            if (args.length > 1) {
                return Lists.newArrayList(new Location(matchWorld(source, args[1]), x, y, z));
            } else {
                Player player = checkPlayer(source);
                return Lists.newArrayList(new Location(player.getWorld(), x, y, z));
            }

            // Handle special hash tag groups
        } else if (filter.charAt(0) == '#') {
            String[] args = filter.split(":");

            // Handle #world, which matches player of the same world as the
            // calling source
            if (args[0].equalsIgnoreCase("#spawn")) {
                CommandBook.inst().checkPermission(source, "commandbook.spawn");
                if (args.length > 1) {
                    return Lists.newArrayList(matchWorld(source, args[1]).getSpawnLocation());
                } else {
                    Player sourcePlayer = checkPlayer(source);
                    return Lists.newArrayList(sourcePlayer.getLocation().getWorld().getSpawnLocation());
                }

                // Handle #target, which matches the player's target position
            } else if (args[0].equalsIgnoreCase("#target")) {
                CommandBook.inst().checkPermission(source, "commandbook.locations.target");
                Player player = checkPlayer(source);
                Location playerLoc = player.getLocation();
                Block targetBlock = player.getTargetBlock(null, 100);

                if (targetBlock == null) {
                    throw new CommandException("Failed to find a block in your target!");
                } else {
                    Location loc = targetBlock.getLocation();
                    playerLoc.setX(loc.getX());
                    playerLoc.setY(loc.getY());
                    playerLoc.setZ(loc.getZ());
                    return Lists.newArrayList(LocationUtil.findFreePosition(playerLoc));
                }
                // Handle #home and #warp, which matches a player's home or a warp point
            } else if (args[0].equalsIgnoreCase("#home")
                    || args[0].equalsIgnoreCase("#warp")) {
                String type = args[0].substring(1);
                CommandBook.inst().checkPermission(source, "commandbook.locations." + type);
                LocationsComponent component = type.equalsIgnoreCase("warp")
                        ? CommandBook.inst().getComponentManager().getComponent(WarpsComponent.class)
                        : CommandBook.inst().getComponentManager().getComponent(HomesComponent.class);
                if (component == null)  {
                    throw new CommandException("This type of location is not enabled!");
                }
                RootLocationManager<NamedLocation> manager = component.getManager();
                if (args.length == 1) {
                    if (type.equalsIgnoreCase("warp")) {
                        throw new CommandException("Please specify a warp name.");
                    }
                    // source player home
                    Player ply = checkPlayer(source);
                    NamedLocation loc = manager.get(ply.getWorld(), ply.getName());
                    if (loc == null) {
                        throw new CommandException("You have not set your home yet.");
                    }
                    return Lists.newArrayList(loc.getLocation());
                } else if (args.length == 2) {
                    if (source instanceof Player) {
                        Player player = (Player) source;
                        NamedLocation loc = manager.get(player.getWorld(), args[1]);
                        if (loc != null && !(loc.getCreatorName().equalsIgnoreCase(player.getName()))) {
                            CommandBook.inst().checkPermission(source, "commandbook.locations." + type + ".other");
                        }
                    }
                    return Lists.newArrayList(LocationUtil.getManagedLocation(manager, checkPlayer(source).getWorld(), args[1]));
                } else if (args.length == 3) {
                    if (source instanceof Player) {
                        Player player = (Player) source;
                        NamedLocation loc = manager.get(matchWorld(source, args[2]), args[1]);
                        if (loc != null && !(loc.getCreatorName().equalsIgnoreCase(player.getName()))) {
                            CommandBook.inst().checkPermission(source, "commandbook.locations." + type + ".other");
                        }
                    }
                    return Lists.newArrayList(LocationUtil.getManagedLocation(manager, matchWorld(source, args[2]), args[1]));
                }
                // Handle #me, which is for when a location argument is required
            } else if (args[0].equalsIgnoreCase("#me")) {
                return Lists.newArrayList(checkPlayer(source).getLocation());
            } else {
                throw new CommandException("Invalid group '" + filter + "'.");
            }
        }

        List<Player> players;

        if (strictMatching) {
            players = PlayerParser.matchPlayerNames(source, filter);
        } else {
            players = PlayerParser.matchPlayers(source, filter);
        }

        // Check to see if there were any matches
        if (players.size() == 0) {
            throw new CommandException("No players matched query.");
        }

        List<Location> locations = new ArrayList<Location>();
        for (Player player : players) {
            locations.add(player.getLocation());
        }
        return locations;
    }
}
