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

import com.sk89q.commandbook.components.AbstractComponent;
import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.components.ComponentInformation;
import com.sk89q.commandbook.config.ConfigurationBase;
import com.sk89q.commandbook.config.Setting;
import com.sk89q.commandbook.events.core.BukkitEvent;
import com.sk89q.commandbook.util.PlayerUtil;
import com.sk89q.minecraft.util.commands.*;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

@ComponentInformation(desc = "A system for kicks and bans.")
public class BansComponent extends AbstractComponent implements Listener {
    private BanDatabase bans;
    private LocalConfiguration config;

    @Override
    public void initialize() {
        config = configure(new LocalConfiguration());
        // Setup the ban database
        bans = new FlatFileBanDatabase(CommandBook.inst().getDataFolder(), this);
        bans.load();
        CommandBook.inst().getEventManager().registerEvents(this, this);
        registerCommands(Commands.class);
    }

    @Override
    public void reload() {
        getBanDatabase().load();
        configure(config);
    }
    
    @Override
    public void unload() {
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
    @BukkitEvent(type = Event.Type.PLAYER_LOGIN)
    public void playerLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();

        try {
            if (getBanDatabase().isBannedName(player.getName())
                    || getBanDatabase().isBannedAddress(
                    player.getAddress().getAddress())) {
                event.disallow(PlayerLoginEvent.Result.KICK_BANNED, config.banMessage);
                return;
            }
        } catch (NullPointerException e) {
            // Bug in CraftBukkit
        }
    }

    public class Commands {
        @Command(aliases = {"kick"}, usage = "<target> [reason...]", desc = "Kick a user", min = 1, max = -1)
        @CommandPermissions({"commandbook.kick"})
        public void kick(CommandContext args, CommandSender sender) throws CommandException {
            Iterable<Player> targets = PlayerUtil.matchPlayers(sender, args.getString(0));
            String message = args.argsLength() >= 2 ? args.getJoinedStrings(1)
                    : "Kicked!";

            String broadcastPlayers = "";
            for (Player player : targets) {
                player.kickPlayer(message);
                broadcastPlayers += player.getName() + " ";
                getBanDatabase().logKick(player, sender, message);
            }


            sender.sendMessage(ChatColor.YELLOW + "Player(s) kicked.");
            //Broadcast the Message
            if (config.broadcastKicks) {
                CommandBook.server().broadcastMessage(ChatColor.YELLOW
                        + PlayerUtil.toName(sender) + " has kicked " + broadcastPlayers
                        + " - " + message);
            }
        }

        @Command(aliases = {"ban"}, usage = "<target> [reason...]", desc = "Ban a user", flags = "e", min = 1, max = -1)
        @CommandPermissions({"commandbook.bans.ban"})
        public void ban(CommandContext args, CommandSender sender) throws CommandException {
            String banName;
            String message = args.argsLength() >= 2 ? args.getJoinedStrings(1)
                    : "Banned!";

            // Check if it's a player in the server right now
            try {
                Player player;

                // Exact mode matches names exactly
                if (args.hasFlag('e')) {
                    player = PlayerUtil.matchPlayerExactly(sender, args.getString(0));
                } else {
                    player = PlayerUtil.matchSinglePlayer(sender, args.getString(0));
                }

                // Need to kick + log
                player.kickPlayer(message);
                getBanDatabase().logKick(player, sender, message);

                banName = player.getName();

                sender.sendMessage(ChatColor.YELLOW + player.getName()
                        + " (" + player.getDisplayName() + ChatColor.WHITE
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
            if (config.broadcastKicks) {
                CommandBook.server().broadcastMessage(ChatColor.YELLOW
                        + PlayerUtil.toName(sender) + " has banned " + banName
                        + " - " + message);
            }

            getBanDatabase().banName(banName, sender, message);

            if (!getBanDatabase().save()) {
                sender.sendMessage(ChatColor.RED + "Bans database failed to save. See console.");
            }
        }
        /*
            @Command(aliases = {"banip"},
                    usage = "<target> [reason...]", desc = "Ban an IP address",
                    min = 1, max = -1)
            @CommandPermissions({"commandbook.bans.ban.ip"})
            public static void banIP(CommandContext args, CommandBookPlugin plugin,
                    CommandSender sender) throws CommandException {

                String message = args.argsLength() >= 2 ? args.getJoinedStrings(1)
                        : "Banned!";

                String addr = args.getString(0)
                            .replace("\r", "")
                            .replace("\n", "")
                            .replace("\0", "")
                            .replace("\b", "");

                // Need to kick + log
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    if (player.getAddress().getAddress().getHostAddress().equals(addr)) {
                        player.kickPlayer(message);
                        plugin.getBanDatabase().logKick(player, sender, message);
                    }
                }

                plugin.getBanDatabase().banAddress(addr, sender, message);

                sender.sendMessage(ChatColor.YELLOW + addr + " banned.");

                if (!plugin.getBanDatabase().save()) {
                    sender.sendMessage(ChatColor.RED + "Bans database failed to save. See console.");
                }
            }
        */
        @Command(aliases = {"unban", "pardon"}, usage = "<target>", desc = "Unban a user", min = 1, max = -1)
        @CommandPermissions({"commandbook.bans.unban"})
        public void unban(CommandContext args, CommandSender sender) throws CommandException {
            String message = args.argsLength() >= 2 ? args.getJoinedStrings(1)
                    : "Unbanned!";

            String banName = args.getString(0)
                    .replace("\r", "")
                    .replace("\n", "")
                    .replace("\0", "")
                    .replace("\b", "");

            if (getBanDatabase().unbanName(banName, sender, message)) {
                sender.sendMessage(ChatColor.YELLOW + banName + " unbanned.");

                if (!getBanDatabase().save()) {
                    sender.sendMessage(ChatColor.RED + "Bans database failed to save. See console.");
                }
            } else {
                sender.sendMessage(ChatColor.RED + banName + " was not banned.");
            }
        }
        /*
            @Command(aliases = {"unbanip"},
                    usage = "<target> [reason...]", desc = "Unban an IP address",
                    min = 1, max = -1)
            @CommandPermissions({"commandbook.bans.unban.ip"})
            public static void unbanIP(CommandContext args, CommandBookPlugin plugin,
                    CommandSender sender) throws CommandException {

                String addr = args.getString(0)
                            .replace("\r", "")
                            .replace("\n", "")
                            .replace("\0", "")
                            .replace("\b", "");
                String message = args.argsLength() >= 2 ? args.getJoinedStrings(1)
                        : "Unbanned!";

                if (plugin.getBanDatabase().unbanAddress(addr, sender, message)) {
                    sender.sendMessage(ChatColor.YELLOW + addr + " unbanned.");

                    if (!plugin.getBanDatabase().save()) {
                        sender.sendMessage(ChatColor.RED + "Bans database failed to save. See console.");
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + addr + " was not banned.");
                }
            }
        */
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
            if (getBanDatabase().load()) {
                sender.sendMessage(ChatColor.YELLOW + "Bans database saved.");
            } else {
                throw new CommandException("Bans database failed to save entirely. See server console.");
            }
        }
    }
}
