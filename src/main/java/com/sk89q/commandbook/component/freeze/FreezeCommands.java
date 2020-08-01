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

package com.sk89q.commandbook.component.freeze;

import com.sk89q.commandbook.command.argument.SinglePlayerTarget;
import com.sk89q.commandbook.util.ChatUtil;
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
public class FreezeCommands {
    private FreezeComponent component;

    public FreezeCommands(FreezeComponent component) {
        this.component = component;
    }

    @Command(
        name = "freeze",
        desc = "Freeze a player"
    )
    @CommandPermissions("commandbook.freeze")
    public void freezeCmd(CommandSender sender,
                          @Switch(name = 's', desc = "silent")
                              boolean silent,
                          @Arg(desc = "player to target")
                              SinglePlayerTarget targetPlayer) throws CommandException {
        Player player = targetPlayer.get();

        if (!component.freezePlayer(player)) {
            if (!silent) {
                player.sendMessage(ChatColor.YELLOW + "You've been frozen by "
                    + ChatUtil.toColoredName(sender, ChatColor.YELLOW));
            }

            sender.sendMessage(ChatColor.YELLOW + "You've frozen "
                    + ChatUtil.toColoredName(player, ChatColor.YELLOW));
        } else {
            if (!silent) {
                player.sendMessage(ChatColor.YELLOW + "Your freeze location has been updated by "
                    + ChatUtil.toColoredName(sender, ChatColor.YELLOW));
            }

            sender.sendMessage(ChatColor.YELLOW + "You have updated the freeze location of "
                    + ChatUtil.toColoredName(player, ChatColor.YELLOW));
        }
    }

    @Command(
        name = "unfreeze",
        desc = "Unfreeze a player"
    )
    @CommandPermissions("commandbook.freeze")
    public void unfreezeCmd(CommandSender sender,
                            @Switch(name = 's', desc = "silent")
                                boolean silent,
                            @Arg(desc = "player to target")
                                SinglePlayerTarget targetPlayer) throws CommandException {
        Player player = targetPlayer.get();

        if (component.unfreezePlayer(player)) {
            if (!silent) {
                player.sendMessage(ChatColor.YELLOW + "You've been unfrozen by "
                    + ChatUtil.toColoredName(sender, ChatColor.YELLOW));
            }

            sender.sendMessage(ChatColor.YELLOW + "You've unfrozen "
                + ChatUtil.toColoredName(player, ChatColor.YELLOW));
        } else {
            throw new CommandException(ChatUtil.toName(player) + " was not frozen");
        }
    }
}