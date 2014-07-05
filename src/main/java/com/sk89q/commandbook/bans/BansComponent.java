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

package com.sk89q.commandbook.bans;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.InfoComponent;
import com.sk89q.commandbook.util.ChatUtil;
import com.sk89q.commandbook.util.InputUtil;
import com.sk89q.minecraft.util.commands.*;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BasePlugin;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

import java.net.InetAddress;
import java.util.UUID;

@ComponentInformation(friendlyName = "Bans", desc = "A system for kicks and bans.")
public class BansComponent extends BukkitComponent implements Listener {
    private BanDatabase bans;
    private LocalConfiguration config;

    @Override
    public void enable() {
        config = configure(new LocalConfiguration());

        // Setup the ban database
        bans = new CSVBanDatabase(CommandBook.inst().getDataFolder());
        bans.load();
        CommandBook.registerEvents(this);
        registerCommands(Commands.class);
    }

    @Override
    public void reload() {
        super.reload();
        getBanDatabase().load();
        configure(config);
    }

    @Override
    public void disable() {
        bans.unload();
    }

    private static class LocalConfiguration extends ConfigurationBase {
        @Setting("message") public String banMessage = "You have been banned";
        @Setting("broadcast-bans") public boolean broadcastBans;
        @Setting("broadcast-kicks") public boolean broadcastKicks;
    }

    /**
     * Get the ban database.
     *
     * @return
     */
    public BanDatabase getBanDatabase() {
        return bans;
    }

    /**
     * Called on player login.
     *
     * @param event Relevant event details
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void playerLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();

        Ban ban = null;

        if (getBanDatabase().isBanned(player.getUniqueId())) {
            ban = getBanDatabase().getBanned(player.getUniqueId());
        } else if (getBanDatabase().isBanned(event.getAddress())) {
            ban = getBanDatabase().getBanned(event.getAddress().getHostAddress());
        }

        if (ban != null) {
            String reason = ban.getReason();
            boolean hasReason = reason != null;
            String how = "You are " + (ban.getAddress() != null ? "IP " : "") + "banned" + (hasReason ? " for:" : ".");
            String end = "Expires: " + (ban.getEnd() == 0L ? ChatColor.DARK_RED + "Never" : ChatUtil.getFriendlyTime(ban.getEnd()));

            event.disallow(PlayerLoginEvent.Result.KICK_BANNED, how + (hasReason ? "\n" + reason : "") + "\n" + end);
        }
    }

    @EventHandler
    public void playerWhois(InfoComponent.PlayerWhoisEvent event) {
        if (CommandBook.inst().hasPermission(event.getSource(), "commandbook.bans.isbanned")) {
            event.addWhoisInformation(null, "Player " +
                    (getBanDatabase().isBanned(event.getPlayer().getUniqueId()) ? "is"
                            : "is not") + " banned.");
        }
    }

    public class Commands {
        @Command(aliases = {"kick"}, usage = "<target> [reason...]", desc = "Kick a user",
                flags = "os", min = 1, max = -1)
        @CommandPermissions({"commandbook.kick"})
        public void kick(CommandContext args, CommandSender sender) throws CommandException {
            Iterable<Player> targets = InputUtil.PlayerParser.matchPlayers(sender, args.getString(0));
            String message = args.argsLength() >= 2 ? args.getJoinedStrings(1)
                    : "Kicked!";

            String broadcastPlayers = "";
            for (Player player : targets) {
                if (CommandBook.inst().hasPermission(player, "commandbook.kick.exempt")
                        && !(args.hasFlag('o') && CommandBook.inst().hasPermission(sender,
                        "commandbook.kick.exempt.override"))) {
                    sender.sendMessage(ChatColor.RED + "Player " + player.getName() + ChatColor.RED + " is exempt from being kicked!");
                    continue;
                }
                player.kickPlayer(message);
                broadcastPlayers += ChatUtil.toColoredName(player, ChatColor.YELLOW) + " ";
                getBanDatabase().logKick(player, sender, message);
            }

            if (broadcastPlayers.length() > 0) {
                sender.sendMessage(ChatColor.YELLOW + "Player(s) kicked.");
                //Broadcast the Message
                if (config.broadcastKicks && !args.hasFlag('s')) {
                    BasePlugin.server().broadcastMessage(ChatColor.YELLOW
                            + ChatUtil.toColoredName(sender, ChatColor.YELLOW) + " has kicked " + broadcastPlayers
                            + " - " + message);
                }
            }
        }

        @Command(aliases = {"ban"}, usage = "[-t end ] <target> [reason...]",
                desc = "Ban a user (and their address with the -i flag)", flags = "iset:o", min = 1, max = -1)
        @CommandPermissions({"commandbook.bans.ban"})
        public void ban(CommandContext args, CommandSender sender) throws CommandException {
            UUID banID;
            String playerName = args.getString(0);
            InetAddress banAddress = null;
            long endDate = args.hasFlag('t') ? InputUtil.TimeParser.matchFutureDate(args.getFlag('t')) : 0L;
            String message = args.argsLength() >= 2 ? args.getJoinedStrings(1) : null;
            boolean kicked = false;

            final boolean hasExemptOverride = args.hasFlag('o')
                    && CommandBook.inst().hasPermission(sender, "commandbook.bans.exempt.override");
            Player player;

            // Exact mode matches names exactly
            try {

                if (args.hasFlag('e')) {
                    player = InputUtil.PlayerParser.matchPlayerExactly(sender, playerName);
                } else {
                    player = InputUtil.PlayerParser.matchSinglePlayer(sender, playerName);
                }
            } catch (CommandException ex) {
                player = null;
            }

            // Grab their UUID
            if (player == null) {
                banID = CommandBook.server().getOfflinePlayer(playerName).getUniqueId();

                if (args.hasFlag('i')) {
                    throw new CommandException("This player must be online to ban their IP address as well.");
                }
            } else {
                banID = player.getUniqueId();

                if (CommandBook.inst().hasPermission(player, "commandbook.bans.exempt") && !hasExemptOverride) {
                    throw new CommandException("This player is exempt from being banned! " +
                            "(use -o flag to override if you have commandbook.bans.exempt.override)");
                }

                kicked = true;

                // Need to kick & log
                if (args.hasFlag('i')) {
                    CommandBook.inst().checkPermission(sender, "commandbook.bans.ban.ip");
                    banAddress = player.getAddress().getAddress();
                    for (Player aPlayer : CommandBook.server().getOnlinePlayers()) {
                        if (aPlayer.getAddress().getAddress().equals(banAddress)) {
                            player.kickPlayer(message == null ? "Banned!" : message);
                            getBanDatabase().logKick(player, sender, message);
                        }
                    }
                } else {
                    player.kickPlayer(message == null ? "Banned!" : message);
                    getBanDatabase().logKick(player, sender, message);
                }
            }

            sender.sendMessage(ChatColor.YELLOW + playerName + " banned" + (!kicked ? "" : " and kicked") + '.');

            // Broadcast the Message
            if (config.broadcastBans && !args.hasFlag('s')) {
                CommandBook.server().broadcastMessage(ChatColor.YELLOW
                        + ChatUtil.toColoredName(sender, ChatColor.YELLOW) + " has banned " + playerName
                        + (message == null ? "" : " - " + message));
            }

            getBanDatabase().ban(banID, playerName, banAddress != null ? banAddress.getHostAddress() : null, sender, message, endDate);

            if (!getBanDatabase().save()) {
                sender.sendMessage(ChatColor.RED + "Bans database failed to save. See console.");
            }
        }

        @Command(aliases = {"banip", "ipban"},
                usage = "<target> [reason...]", desc = "Ban an IP address", flags = "st:",
                min = 1, max = -1)
        @CommandPermissions({"commandbook.bans.ban.ip"})
        public void banIP(CommandContext args, CommandSender sender) throws CommandException {

            String message = args.argsLength() >= 2 ? args.getJoinedStrings(1)
                    : null;
            long endDate = args.hasFlag('t') ? InputUtil.TimeParser.matchFutureDate(args.getFlag('t')) : 0L;

            String addr = args.getString(0)
                        .replace("\r", "")
                        .replace("\n", "")
                        .replace("\0", "")
                        .replace("\b", "");

            // Need to kick + log
            for (Player player : CommandBook.server().getOnlinePlayers()) {
                if (player.getAddress().getAddress().getHostAddress().equals(addr)) {
                    player.kickPlayer(message == null ? "Banned!" : message);
                    getBanDatabase().logKick(player, sender, message);
                }
            }

            getBanDatabase().ban(null, null, addr, sender, message, endDate);

            sender.sendMessage(ChatColor.YELLOW + addr + " banned.");

            if (!getBanDatabase().save()) {
                sender.sendMessage(ChatColor.RED + "Bans database failed to save. See console.");
            }
        }

        @Command(aliases = {"unban"}, usage = "<target>", desc = "Unban a user", min = 1, max = -1)
        @CommandPermissions({"commandbook.bans.unban"})
        public void unban(CommandContext args, CommandSender sender) throws CommandException {
            String message = args.argsLength() >= 2 ? args.getJoinedStrings(1) : "Unbanned!";

            String banName = args.getString(0)
                    .replace("\r", "")
                    .replace("\n", "")
                    .replace("\0", "")
                    .replace("\b", "");

            UUID ID = CommandBook.server().getOfflinePlayer(banName).getUniqueId();

            if (getBanDatabase().unban(ID, null, sender, message) || getBanDatabase().unbanName(banName, sender, message)) {
                sender.sendMessage(ChatColor.YELLOW + banName + " unbanned.");

                if (!getBanDatabase().save()) {
                    sender.sendMessage(ChatColor.RED + "Bans database failed to save. See console.");
                }
            } else {
                sender.sendMessage(ChatColor.RED + banName + " was not banned.");
            }
        }

        @Command(aliases = {"unbanip", "unipban"},
                usage = "<target> [reason...]", desc = "Unban an IP address",
                min = 1, max = -1)
        @CommandPermissions({"commandbook.bans.unban.ip"})
        public void unbanIP(CommandContext args,
                                   CommandSender sender) throws CommandException {

            String addr = args.getString(0)
                    .replace("\r", "")
                    .replace("\n", "")
                    .replace("\0", "")
                    .replace("\b", "");
            String message = args.argsLength() >= 2 ? args.getJoinedStrings(1)
                    : "Unbanned!";

            if (getBanDatabase().unban(null, addr, sender, message)) {
                sender.sendMessage(ChatColor.YELLOW + addr + " unbanned.");

                if (!getBanDatabase().save()) {
                    sender.sendMessage(ChatColor.RED + "Bans database failed to save. See console.");
                }
            } else {
                sender.sendMessage(ChatColor.RED + addr + " was not banned.");
            }
        }

        @Command(aliases = {"baninfo", "isbanned"}, usage = "<target>", desc = "Check if a user is banned", min = 1, max = 1)
        @CommandPermissions({"commandbook.bans.isbanned", "commandbook.bans.baninfo"})
        public void banInfo(CommandContext args,  CommandSender sender) throws CommandException {
            String banName = args.getString(0)
                    .replace("\r", "")
                    .replace("\n", "")
                    .replace("\0", "")
                    .replace("\b", "");

            UUID ID = CommandBook.server().getOfflinePlayer(banName).getUniqueId();

            Ban ban = getBanDatabase().getBanned(ID);

            if (ban == null) {
                sender.sendMessage(ChatColor.YELLOW + banName + " is " + ChatColor.RED + "not " + ChatColor.YELLOW + "banned.");
            } else {
                String add = ban.getAddress() == null ? "" : " (Address: " + ban.getAddress() + ")";
                sender.sendMessage(ChatColor.YELLOW + banName + add + ChatColor.RED + " is" + ChatColor.YELLOW + " banned.");
                if (!CommandBook.inst().hasPermission(sender, "commandbook.bans.baninfo")) return;
                sender.sendMessage(ChatColor.YELLOW + "Duration: " + (ban.getEnd() == 0L ? "Indefinite"
                                              : "Until " + ChatUtil.getFriendlyTime(ban.getEnd())));
                if (ban.getReason() != null) {
                    sender.sendMessage(ChatColor.YELLOW + "Reason: " + ban.getReason());
                }
            }
        }


        @Command(aliases = {"bans"}, desc = "Ban management")
        @NestedCommand({ManagementCommands.class})
        public void bans() throws CommandException {
        }
    }

    public class ManagementCommands {

        @Command(aliases = {"load", "reload", "read"}, usage = "", desc = "Reload bans from disk", min = 0, max = 0)
        @CommandPermissions({"commandbook.bans.load"})
        public void loadBans(CommandContext args, CommandSender sender) throws CommandException {
            if (getBanDatabase().load()) {
                sender.sendMessage(ChatColor.YELLOW + "Bans database reloaded.");
            } else {
                throw new CommandException("Bans database failed to load entirely. See server console.");
            }
        }

        @Command(aliases = {"save", "write"}, usage = "", desc = "Save bans to disk", min = 0, max = 0)
        @CommandPermissions({"commandbook.bans.save"})
        public void saveBans(CommandContext args, CommandSender sender) throws CommandException {
            if (getBanDatabase().save()) {
                sender.sendMessage(ChatColor.YELLOW + "Bans database saved.");
            } else {
                throw new CommandException("Bans database failed to save entirely. See server console.");
            }
        }
    }
}
