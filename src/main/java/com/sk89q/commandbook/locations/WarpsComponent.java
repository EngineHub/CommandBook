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

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.commands.PaginatedResult;
import com.sk89q.commandbook.components.ComponentInformation;
import com.sk89q.commandbook.util.LocationUtil;
import com.sk89q.commandbook.util.PlayerUtil;
import com.sk89q.commandbook.util.TeleportPlayerIterator;
import com.sk89q.minecraft.util.commands.*;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@ComponentInformation(friendlyName = "Warps", desc = "Provides warps functionality")
public class WarpsComponent extends LocationsComponent {
    public WarpsComponent() {
        super("Warp");
    }

    public void initialize() {
        super.initialize();
        registerCommands(Commands.class);
    }

    public class Commands {
        @Command(aliases = {"warp"},
                usage = "[world] [target] <warp>", desc = "Teleport to a warp",
                flags = "s", min = 1, max = 3)
        @CommandPermissions({"commandbook.warp.teleport"})
        public void warp(CommandContext args, CommandSender sender) throws CommandException {
            Iterable<Player> targets = null;
            NamedLocation warp = null;
            Location loc = null;

            // Detect arguments based on the number of arguments provided
            if (args.argsLength() == 1) {
                Player player = PlayerUtil.checkPlayer(sender);
                targets = PlayerUtil.matchPlayers(player);
                warp = getManager().get(player.getWorld(), args.getString(0));
            } else if (args.argsLength() == 2) {
                targets = PlayerUtil.matchPlayers(sender, args.getString(0));
                if (getManager().isPerWorld()) {
                    Player player = PlayerUtil.checkPlayer(sender);
                    warp = getManager().get(player.getWorld(), args.getString(1));
                } else {
                    warp = getManager().get(null, args.getString(1));
                }

                // Check permissions!
                CommandBook.inst().checkPermission(sender, "commandbook.warp.teleport.other");
            } else if (args.argsLength() == 3) {
                targets = PlayerUtil.matchPlayers(sender, args.getString(1));
                warp = getManager().get(
                        LocationUtil.matchWorld(sender, args.getString(0)), args.getString(2));

                // Check permissions!
                CommandBook.inst().checkPermission(sender, "commandbook.warp.teleport.other");
            }

            if (warp != null) {
                loc = warp.getLocation();
            } else {
                throw new CommandException("A warp by the given name does not exist.");
            }

            boolean silent = args.hasFlag('s');
            (new TeleportPlayerIterator(sender, loc, silent)).iterate(targets);
        }

        @Command(aliases = {"setwarp"}, usage = "<warp> [location]", desc = "Set a warp", min = 1, max = 2)
        @CommandPermissions({"commandbook.warp.set"})
        public void setWarp(CommandContext args, CommandSender sender) throws CommandException {
            String warpName = args.getString(0);
            Location loc;
            Player player = null;

            // Detect arguments based on the number of arguments provided
            if (args.argsLength() == 1) {
                player = PlayerUtil.checkPlayer(sender);
                loc = player.getLocation();
            } else {
                loc = LocationUtil.matchLocation(sender, args.getString(1));
            }

            getManager().create(warpName, loc, player);

            sender.sendMessage(ChatColor.YELLOW + "Warp '" + warpName + "' created.");
        }

        @Command(aliases = {"warps"}, desc = "Warp management")
        @NestedCommand({ManagementCommands.class})
        public void warps(CommandContext args, CommandSender sender) throws CommandException {
        }
    }
    
    public class ManagementCommands {
        @Command(aliases = {"del", "delete", "remove", "rem"}, usage = "<warpname> [world]",
                desc = "Remove a warp", min = 1, max = 2 )
        @CommandPermissions({"commandbook.remove"})
        public void removeCmd(CommandContext args, CommandSender sender) throws CommandException {
            World world;
            String warpName = args.getString(0);
            if (args.argsLength() == 2) {
                world = LocationUtil.matchWorld(sender, args.getString(1));
            } else {
                world = PlayerUtil.checkPlayer(sender).getWorld();
            }
            remove(warpName, world, sender);
        }


        @Command(aliases = {"list", "show"}, usage = "[ -p owner] [-w world] [page]",
                desc = "List warps", flags = "p:w:", min = 0, max = 1 )
        @CommandPermissions({"commandbook.warp.list"})
        public void listCmd(CommandContext args, CommandSender sender) throws CommandException {
            list(args, sender);
        }
    }

    @Override
    public PaginatedResult<NamedLocation> getListResult() {
        final String defaultWorld = CommandBook.server().getWorlds().get(0).getName();
        return new PaginatedResult<NamedLocation>("Name - Owner - World  - Location") {
            @Override
            public String format(NamedLocation entry) {
                return entry.getName()
                        + " - " + entry.getCreatorName()
                        + " - " + (entry.getWorldName() == null ? defaultWorld : entry.getWorldName())
                        + " - " + entry.getLocation().getBlockX() + "," + entry.getLocation().getBlockY()
                        + "," + entry.getLocation().getBlockZ();
            }
        };
    }
}
