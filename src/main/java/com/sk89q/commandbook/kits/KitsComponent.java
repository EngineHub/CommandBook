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

package com.sk89q.commandbook.kits;

import com.google.common.collect.Lists;
import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.util.ChatUtil;
import com.sk89q.commandbook.util.InputUtil;
import com.sk89q.commandbook.util.entity.player.PlayerUtil;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.File;
import java.util.Map;
import java.util.Map.Entry;

@ComponentInformation(friendlyName = "Kits", desc = "Distributes kits to players on command (with cooldowns)")
public class KitsComponent extends BukkitComponent implements Listener {
    private KitManager kits;

    @Override
    public void enable() {
        CommandBook.inst().createDefaultConfiguration("kits.txt");

        // Setup kits
        kits = new FlatFileKitsManager(new File(CommandBook.inst().getDataFolder(), "kits.txt"));
        kits.load();

        CommandBook.server().getScheduler().scheduleAsyncRepeatingTask(
                CommandBook.inst(), new GarbageCollector(this),
                GarbageCollector.CHECK_FREQUENCY, GarbageCollector.CHECK_FREQUENCY);
        registerCommands(Commands.class);
        CommandBook.registerEvents(this);
    }

    @Override
    public void reload() {
        super.reload();
        kits.load();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (!player.hasPlayedBefore()) {
            for (Entry<String, Kit> map : kits.getKits().entrySet()) {
                if (CommandBook.inst().hasPermission(player, "commandbook.kits.onfirstlogin." + map.getKey())) {
                    map.getValue().distribute(player);
                }
            }
        }
    }

    /**
     * Return the kit manager.
     *
     * @return the kit manager currently active
     */
    public KitManager getKitManager() {
        return kits;
    }

    public class Commands {

        @Command(aliases = {"kit"}, usage = "<id> [target]", desc = "Get a kit", flags = "", min = 0, max = 2)
        public void kit(CommandContext args, CommandSender sender) throws CommandException {
            // List kits
            if (args.argsLength() == 0) {
                CommandBook.inst().checkPermission(sender, "commandbook.kit.list");

                Map<String, Kit> kits = getKitManager().getKits();

                if (kits.size() == 0) {
                    throw new CommandException("No kits are configured.");
                }

                StringBuilder str = new StringBuilder();
                int count = 0;

                for (String id : kits.keySet()) {
                    if (!CommandBook.inst().hasPermission(sender,
                            "commandbook.kit.kits." + id.replace(".", ""))) {
                        continue;
                    }

                    if (str.length() != 0) {
                        str.append(", ");
                    }

                    str.append(id);
                    count++;
                }

                if (count == 0) {
                    throw new CommandException("You have access to no kits.");
                }

                sender.sendMessage(ChatColor.YELLOW + "Kits (" + count + "): "
                        + ChatColor.WHITE + str.toString());
                sender.sendMessage(ChatColor.YELLOW + "Use /kit kitname to get a kit.");

            // Give a kit
            } else {
                Iterable<Player> targets;
                String id = args.getString(0).toLowerCase();
                boolean included = false;

                if (args.argsLength() == 2) {
                    targets = InputUtil.PlayerParser.matchPlayers(sender, args.getString(1));
                } else {
                    targets = Lists.newArrayList(PlayerUtil.checkPlayer(sender));
                }

                for (Player player : targets) {
                    if (player != sender) {
                        // Check permissions!
                        CommandBook.inst().checkPermission(sender, "commandbook.kit.other");
                    }
                }

                Kit kit = getKitManager().getKit(id);

                if (kit == null) {
                    throw new CommandException("No kit by that name exists.");
                }

                CommandBook.inst().checkPermission(sender, "commandbook.kit.kits." + id.replace(".", ""));

                for (Player player : targets) {
                    boolean success = kit.distribute(player);

                    // Tell the user
                    if (player.equals(sender)) {
                        if (success) {
                            player.sendMessage(ChatColor.YELLOW + "Kit '" + id + "' given!");
                        } else {
                            player.sendMessage(ChatColor.RED + "You have to wait before you can get this kit again.");
                        }

                        included = true;
                    } else {
                        if (success) {
                            player.sendMessage(ChatColor.YELLOW + "You've been given " +
                                    "the '" + id + "' kit by "
                                    + ChatUtil.toColoredName(sender, ChatColor.YELLOW) + ".");
                        } else {
                            player.sendMessage(ChatColor.RED + "A kit could not be given to you because it has been too soon.");
                        }

                    }
                }

                // The player didn't receive any items, then we need to send the
                // user a message so s/he know that something is indeed working
                if (!included) {
                    sender.sendMessage(ChatColor.YELLOW + "Kits given.");
                }
            }
        }
    }
}
