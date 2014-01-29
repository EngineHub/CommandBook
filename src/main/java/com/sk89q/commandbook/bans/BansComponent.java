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

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

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
        if (FlatFileBanDatabase.toImport(CommandBook.inst().getDataFolder())) {
            BanDatabase banDb = new FlatFileBanDatabase(CommandBook.inst().getDataFolder(), this);
            banDb.load();
            bans.importFrom(banDb);
            final File oldBansFile = new File(CommandBook.inst().getDataFolder(), "banned_names.txt");
            oldBansFile.renameTo(new File(oldBansFile.getAbsolutePath() + ".old"));
        }
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
        final Player player = event.getPlayer();

        if (!CommandBook.inst().hasPermission(player, "commandbook.bans.exempt")) {
            if (getBanDatabase().isBannedName(player.getName())) {
                event.disallow(PlayerLoginEvent.Result.KICK_BANNED,
                        getBanDatabase().getBannedNameMessage(player.getName()));
            } else if (getBanDatabase().isBannedAddress(
                    event.getAddress())) {
                event.disallow(PlayerLoginEvent.Result.KICK_BANNED, getBanDatabase().getBannedAddressMessage(
                        event.getAddress().getHostAddress()));
            }
        }
    }

    @EventHandler
    public void playerWhois(InfoComponent.PlayerWhoisEvent event) {
        if (CommandBook.inst().hasPermission(event.getSource(), "commandbook.bans.isbanned")) {
            event.addWhoisInformation(null, "Player " +
                    (getBanDatabase().isBannedName(event.getPlayer().getName()) ? "is"
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
                desc = "Ban a user or IP address (with the -i flag)", flags = "set:o", min = 1, max = -1)
        @CommandPermissions({"commandbook.bans.ban"})
        public void ban(CommandContext args, CommandSender sender) throws CommandException {
            String banName;
            String banAddress = null;
            long endDate = args.hasFlag('t') ? InputUtil.TimeParser.matchFutureDate(args.getFlag('t')) : 0L;
            String message = args.argsLength() >= 2 ? args.getJoinedStrings(1)
                    : "Banned!";

            final boolean hasExemptOverride = args.hasFlag('o')
                    && CommandBook.inst().hasPermission(sender, "commandbook.bans.exempt.override");
            // Check if it's a player in the server right now
            try {
                Player player;

                // Exact mode matches names exactly
                if (args.hasFlag('e')) {
                    player = InputUtil.PlayerParser.matchPlayerExactly(sender, args.getString(0));
                } else {
                    player = InputUtil.PlayerParser.matchSinglePlayer(sender, args.getString(0));
                }

                if (CommandBook.inst().hasPermission(player, "commandbook.bans.exempt") && !hasExemptOverride) {
                    throw new CommandException("This player is exempt from being banned! " +
                            "(use -o flag to override if you have commandbook.bans.exempt.override)");
                }

                // Need to kick + log
                player.kickPlayer(message);
                getBanDatabase().logKick(player, sender, message);

                banName = player.getName();

                sender.sendMessage(ChatColor.YELLOW + player.getName()
                        + " (" + player.getDisplayName() + ChatColor.YELLOW
                        + ") banned and kicked.");
            } catch (CommandException e) {
                banName = args.getString(0)
                        .replace("\r", "")
                        .replace("\n", "")
                        .replace("\0", "")
                        .replace("\b", "");

                sender.sendMessage(ChatColor.YELLOW + banName
                        + " banned.");
            }

            //Broadcast the Message
            if (config.broadcastBans && !args.hasFlag('s')) {
                CommandBook.server().broadcastMessage(ChatColor.YELLOW
                        + ChatUtil.toColoredName(sender, ChatColor.YELLOW) + " has banned " + banName
                        + " - " + message);
            }

            getBanDatabase().ban(banName, banAddress, sender, message, endDate);

            if (!getBanDatabase().save()) {
                sender.sendMessage(ChatColor.RED + "Bans database failed to save. See console.");
            }
        }

            @Command(aliases = {"banip", "ipban"},
                    usage = "<target> [reason...]", desc = "Ban an IP address", flags = "st:",
                    min = 1, max = -1)
            @CommandPermissions({"commandbook.bans.ban.ip"})
            public void banIP(CommandContext args,
                    CommandSender sender) throws CommandException {

                String message = args.argsLength() >= 2 ? args.getJoinedStrings(1)
                        : "Banned!";
                long endDate = args.hasFlag('t') ? InputUtil.TimeParser.matchFutureDate(args.getFlag('t')) : 0L;

                String addr = args.getString(0)
                            .replace("\r", "")
                            .replace("\n", "")
                            .replace("\0", "")
                            .replace("\b", "");

                // Need to kick + log
                for (Player player : CommandBook.server().getOnlinePlayers()) {
                    if (player.getAddress().getAddress().getHostAddress().equals(addr)) {
                        player.kickPlayer(message);
                        getBanDatabase().logKick(player, sender, message);
                    }
                }

                getBanDatabase().ban(null, addr, sender, message, endDate);

                sender.sendMessage(ChatColor.YELLOW + addr + " banned.");

                if (!getBanDatabase().save()) {
                    sender.sendMessage(ChatColor.RED + "Bans database failed to save. See console.");
                }
            }

        @Command(aliases = {"unban"}, usage = "<target>", desc = "Unban a user", min = 1, max = -1)
        @CommandPermissions({"commandbook.bans.unban"})
        public void unban(CommandContext args, CommandSender sender) throws CommandException {
            String message = args.argsLength() >= 2 ? args.getJoinedStrings(1)
                    : "Unbanned!";

            String banName = args.getString(0)
                    .replace("\r", "")
                    .replace("\n", "")
                    .replace("\0", "")
                    .replace("\b", "");

            if (getBanDatabase().unban(banName, null, sender, message)) {
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

        @Command(aliases = {"isbanned"}, usage = "<target>", desc = "Check if a user is banned", min = 1, max = 1)
        @CommandPermissions({"commandbook.bans.isbanned"})
        public void isBanned(CommandContext args,  CommandSender sender) throws CommandException {
            String banName = args.getString(0)
                    .replace("\r", "")
                    .replace("\n", "")
                    .replace("\0", "")
                    .replace("\b", "");

            if (getBanDatabase().isBannedName(banName)) {
                sender.sendMessage(ChatColor.YELLOW + banName + " is banned.");
            } else {
                sender.sendMessage(ChatColor.YELLOW + banName + " NOT banned.");
            }
        }

        private final SimpleDateFormat dateFormat =
                new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

        @Command(aliases = {"baninfo"}, usage = "<target>", desc = "Check if a user is banned", min = 1, max = 1)
        @CommandPermissions({"commandbook.bans.baninfo"})
        public void banInfo(CommandContext args,  CommandSender sender) throws CommandException {
            String banName = args.getString(0)
                    .replace("\r", "")
                    .replace("\n", "")
                    .replace("\0", "")
                    .replace("\b", "");

            Ban ban = getBanDatabase().getBannedName(banName);

            if (ban == null) {
                sender.sendMessage(ChatColor.YELLOW + banName + " is NOT banned.");
            } else {
                sender.sendMessage(ChatColor.YELLOW + "Ban for " + banName + ":"  + ban.getAddress()
                        + " for reason: '" + ban.getReason() + "' until " +
                        (ban.getEnd() == 0L ? " forever" : dateFormat.format(new Date(ban.getEnd()))));

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
