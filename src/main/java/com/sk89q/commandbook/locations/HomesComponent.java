/*
 * CommandBook
 * Copyright (C) 2011 sk89q <http://www.sk89q.com>
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

package com.sk89q.commandbook.locations;

import com.google.common.collect.Lists;
import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.commands.PaginatedResult;
import com.sk89q.commandbook.util.InputUtil;
import com.sk89q.commandbook.util.entity.player.PlayerUtil;
import com.sk89q.commandbook.util.entity.player.iterators.TeleportPlayerIterator;
import com.sk89q.minecraft.util.commands.*;
import com.zachsthings.libcomponents.ComponentInformation;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

@ComponentInformation(friendlyName = "Homes", desc = "Home management system")
public class HomesComponent extends LocationsComponent {
    public HomesComponent() {
        super("Home");
    }

    public void enable() {
        super.enable();
        registerCommands(Commands.class);
    }

    public class Commands {
        @Command(aliases = {"home"}, usage = "[world] [target] [owner]", desc = "Teleport to a home", min = 0, max = 3)
        @CommandPermissions({"commandbook.home.teleport"})
        public void home(CommandContext args, CommandSender sender) throws CommandException {
            Iterable<Player> targets = null;
            Map<String, NamedLocation> homes = null;
            Location loc;

            // Detect arguments based on the number of arguments provided
            if (args.argsLength() == 0) {
                Player player = PlayerUtil.checkPlayer(sender);
                targets = Lists.newArrayList(player);
                homes = getManager().get(player.getWorld(), player.getUniqueId());
            } else if (args.argsLength() == 1) {
                Player player = PlayerUtil.checkPlayer(sender);
                targets = Lists.newArrayList(player);
                OfflinePlayer target = Bukkit.getOfflinePlayer(args.getString(0));
                if (target == null) {
                    throw new CommandException("No homes for that player could be found.");
                }
                homes = getManager().get(player.getWorld(), target.getUniqueId());
            } else if (args.argsLength() == 2) {
                targets = InputUtil.PlayerParser.matchPlayers(sender, args.getString(0));
                OfflinePlayer target = Bukkit.getOfflinePlayer(args.getString(1));
                if (target == null) {
                    throw new CommandException("No homes for that player could be found.");
                }
                if (getManager().isPerWorld()) {
                    Player player = PlayerUtil.checkPlayer(sender);
                    homes = getManager().get(player.getWorld(), target.getUniqueId());
                } else {
                    homes = getManager().get(null, target.getUniqueId());
                }
            } else if (args.argsLength() == 3) {
                targets = InputUtil.PlayerParser.matchPlayers(sender, args.getString(1));
                OfflinePlayer target = Bukkit.getOfflinePlayer(args.getString(1));
                if (target == null) {
                    throw new CommandException("No homes for that player could be found.");
                }
                homes = getManager().get(
                        InputUtil.LocationParser.matchWorld(
                                sender,
                                args.getString(0)
                        ),
                        target.getUniqueId()
                );
            }

            if (homes != null && !homes.isEmpty()) {

                // Check permissions!
                for (Player aTarget : targets) {
                    if (!aTarget.equals(sender)) {
                        CommandBook.inst().checkPermission(sender, "commandbook.home.teleport.other");
                        break;
                    }
                }

                NamedLocation home = homes.values().iterator().next();
                if (!(sender instanceof Player) || !home.getOwnerID().equals(PlayerUtil.checkPlayer(sender).getUniqueId())) {
                    CommandBook.inst().checkPermission(sender, "commandbook.home.other");
                }
                loc = home.getLocation();
            } else {
                throw new CommandException("A home for the given player does not exist.");
            }

            (new TeleportPlayerIterator(sender, loc)).iterate(targets);
        }

        @Command(aliases = {"sethome"}, usage = "[owner] [location]", desc = "Set a home", min = 0, max = 2)
        @CommandPermissions({"commandbook.home.set"})
        public void setHome(CommandContext args, CommandSender sender) throws CommandException {
            OfflinePlayer offlinePlayer = null;
            Location loc;

            // Detect arguments based on the number of arguments provided
            if (args.argsLength() == 0) {
                Player player = PlayerUtil.checkPlayer(sender);
                offlinePlayer = player;
                loc = player.getLocation();
            } else if (args.argsLength() == 1) {
                Player player = PlayerUtil.checkPlayer(sender);
                offlinePlayer = Bukkit.getOfflinePlayer(args.getString(0));
                loc = player.getLocation();

                // Check permissions!
                if (!player.getUniqueId().equals(offlinePlayer.getUniqueId())) {
                    CommandBook.inst().checkPermission(sender, "commandbook.home.set.other");
                }
            } else {
                offlinePlayer = Bukkit.getOfflinePlayer(args.getString(1));
                loc = InputUtil.LocationParser.matchLocation(sender, args.getString(0));

                // Check permissions!
                if (!CommandBook.inst().hasPermission(sender, "commandbook.home.set.other")) {
                    Player player = PlayerUtil.checkPlayer(sender);
                    if (!player.getUniqueId().equals(offlinePlayer.getUniqueId())) {
                        throw new CommandPermissionsException();
                    }
                }
            }

            try {
                getManager().remove(loc.getWorld(), offlinePlayer.getUniqueId());
                getManager().create(offlinePlayer.getUniqueId().toString(), loc, offlinePlayer);
            } catch (IllegalArgumentException ex) {
                throw new CommandException("Invalid home name!");
            }

            sender.sendMessage(ChatColor.YELLOW + "Home set.");
        }

        @Command(aliases = {"homes"}, desc = "Home management")
        @NestedCommand({ManagementCommands.class})
        public void homes() throws CommandException {
        }
    }

    public class ManagementCommands {
        @Command(aliases = {"info", "inf"}, usage = "<name> [world]",
                desc = "Get information about a home", min = 1, max = 2
        )
        @CommandPermissions({"commandbook.home.info"})
        public void infoCmd(CommandContext args, CommandSender sender) throws CommandException {
            OfflinePlayer target = Bukkit.getOfflinePlayer(args.getString(0));
            if (target == null) {
                throw new CommandException("No homes for that player could be found.");
            }

            World targetWorld;
            if (args.argsLength() == 2) {
                targetWorld = InputUtil.LocationParser.matchWorld(sender, args.getString(1));
            } else {
                targetWorld = PlayerUtil.checkPlayer(sender).getWorld();
            }

            Map<String, NamedLocation> homes = getManager().get(
                    targetWorld,
                    target.getUniqueId()
            );

            if (homes != null && homes.isEmpty()) {
                NamedLocation home = homes.values().iterator().next();
                info(home.getName(), targetWorld, sender);
            } else {
                throw new CommandException("A home for the given player does not exist.");
            }
        }

        @Command(aliases = {"del", "delete", "remove", "rem"},
                usage = "[name] [world]", desc = "Remove a home", min = 0, max = 2 )
        @CommandPermissions({"commandbook.home.remove"})
        public void removeCmd(CommandContext args, CommandSender sender) throws CommandException {
            OfflinePlayer target;
            if (args.argsLength() > 0) {
                target = Bukkit.getOfflinePlayer(args.getString(0));
            } else {
                target = PlayerUtil.checkPlayer(sender);
            }
            if (target == null) {
                throw new CommandException("No homes for that player could be found.");
            }

            World targetWorld;
            if (args.argsLength() == 2) {
                targetWorld = InputUtil.LocationParser.matchWorld(sender, args.getString(1));
            } else {
                targetWorld = PlayerUtil.checkPlayer(sender).getWorld();
            }

            Map<String, NamedLocation> homes = getManager().get(
                    targetWorld,
                    target.getUniqueId()
            );

            if (homes != null && homes.isEmpty()) {
                NamedLocation home = homes.values().iterator().next();
                remove(home.getName(), targetWorld, sender);
            } else {
                throw new CommandException("A home for the given player does not exist.");
            }
        }

        @Command(aliases = {"list", "show"}, usage = "[-w world] [page]", desc = "List homes",
                flags = "w:", min = 0, max = 1 )
        @CommandPermissions({"commandbook.home.list"})
        public void listCmd(CommandContext args, CommandSender sender) throws CommandException {
            list(args, sender);
        }
    }

    @Override
    public PaginatedResult<NamedLocation> getListResult() {
        final String defaultWorld = CommandBook.server().getWorlds().get(0).getName();
        return new PaginatedResult<NamedLocation>(ChatColor.GOLD + "Homes") {
            @Override
            public String format(NamedLocation entry) {
                return ChatColor.BLUE + entry.getName().toUpperCase() + ChatColor.YELLOW
                        + " (Owner: " + ChatColor.WHITE + entry.getCreatorName()
                        + ChatColor.YELLOW + ", World: "
                        + ChatColor.WHITE + (entry.getWorldName() == null ? defaultWorld : entry.getWorldName())
                        + ChatColor.YELLOW + ")";
            }
        };
    }
}
