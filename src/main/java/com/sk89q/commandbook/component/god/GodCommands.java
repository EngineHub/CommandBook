/*
 * CommandBook
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) CommandBook team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.commandbook.component.god;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.command.argument.MultiPlayerTarget;
import com.sk89q.commandbook.util.ChatUtil;
import com.sk89q.commandbook.util.entity.player.PlayerUtil;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.worldedit.command.util.CommandPermissions;
import com.sk89q.worldedit.command.util.CommandPermissionsConditionGenerator;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.annotation.param.Arg;
import org.enginehub.piston.annotation.param.Switch;

@CommandContainer(superTypes = CommandPermissionsConditionGenerator.Registration.class)
public class GodCommands {
    private GodComponent component;

    public GodCommands(GodComponent component) {
        this.component = component;
    }

    private MultiPlayerTarget processTargets(CommandSender sender, MultiPlayerTarget targetPlayers) throws CommandException {
        if (targetPlayers == null) {
            targetPlayers = new MultiPlayerTarget(PlayerUtil.checkPlayer(sender));
        }

        // Check permissions!
        for (Player player : targetPlayers) {
            if (player == sender) {
                CommandBook.inst().checkPermission(sender, "commandbook.god");
            } else {
                CommandBook.inst().checkPermission(sender, "commandbook.god.other");
            }
        }

        return targetPlayers;
    }

    @Command(
        name = "god",
        desc = "Enable godmode on a player"
    )
    @CommandPermissions({"commandbook.god", "commandbook.god.other"})
    public void godCmd(CommandSender sender,
                       @Switch (name = 's', desc = "silent")
                           boolean silent,
                       @Arg(desc = "player(s) to target", def = "")
                           MultiPlayerTarget targetPlayers) throws CommandException {
        targetPlayers = processTargets(sender, targetPlayers);

        boolean included = false;
        for (Player player : targetPlayers) {
            if (!component.hasGodMode(player)) {
                component.enableGodMode(player);
            } else {
                if (player == sender) {
                    player.sendMessage(ChatColor.RED + "You already have god mode!");
                    included = true;
                } else {
                    sender.sendMessage(ChatColor.RED + player.getName() + " already has god mode!");
                }
                continue;
            }

            // Tell the user
            if (player.equals(sender)) {
                player.sendMessage(ChatColor.YELLOW + "God mode enabled! Use /ungod to disable.");

                // Keep track of this
                included = true;
            } else {
                if (!silent)
                    player.sendMessage(ChatColor.YELLOW + "God enabled by "
                            + ChatUtil.toColoredName(sender, ChatColor.YELLOW) + ".");

            }
        }

        // The player didn't receive any items, then we need to send the
        // user a message so s/he know that something is indeed working
        if (!included) {
            sender.sendMessage(ChatColor.YELLOW.toString() + "Players now have god mode.");
        }
    }

    @Command(
        name = "ungod",
        desc = "Disable godmode on a player"
    )
    @CommandPermissions({"commandbook.god", "commandbook.god.other"})
    public void ungodCmd(CommandSender sender,
                         @Switch (name = 's', desc = "silent")
                             boolean silent,
                         @Arg(desc = "player(s) to target", def = "")
                             MultiPlayerTarget targetPlayers) throws CommandException {
        targetPlayers = processTargets(sender, targetPlayers);

        boolean included = false;
        for (Player player : targetPlayers) {
            if (component.hasGodMode(player)) {
                component.disableGodMode(player);
            } else {
                if (player == sender) {
                    player.sendMessage(ChatColor.RED + "You do not have god mode enabled!");
                    included = true;
                } else {
                    sender.sendMessage(ChatColor.RED + player.getName() + " did not have god mode enabled!");
                }
                continue;
            }

            // Tell the user
            if (player.equals(sender)) {
                player.sendMessage(ChatColor.YELLOW + "God mode disabled!");

                // Keep track of this
                included = true;
            } else {
                if (!silent) {
                    player.sendMessage(ChatColor.YELLOW + "God disabled by "
                            + ChatUtil.toColoredName(sender, ChatColor.YELLOW) + ".");
                }
            }
        }

        // The player didn't receive any items, then we need to send the
        // user a message so s/he know that something is indeed working
        if (!included) {
            sender.sendMessage(ChatColor.YELLOW.toString() + "Players no longer have god mode.");
        }
    }
}
