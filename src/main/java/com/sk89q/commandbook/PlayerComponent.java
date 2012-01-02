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

package com.sk89q.commandbook;

import com.sk89q.commandbook.components.AbstractComponent;
import com.sk89q.commandbook.components.ComponentInformation;
import com.sk89q.commandbook.util.PlayerUtil;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@ComponentInformation(friendlyName = "Player Commands", desc = "Various player-related commands.")
public class PlayerComponent extends AbstractComponent {
    @Override
    public void initialize() {
        registerCommands(Commands.class);
    }

    public class Commands {
        @Command(aliases = {"gamemode", "gm"},
                usage = "[player] [gamemode]", desc = "Change a player's gamemode",
                flags = "c", min = 0, max = 2)
        @CommandPermissions({"commandbook.gamemode"})
        public void gamemode(CommandContext args, CommandSender sender) throws CommandException {

            Player player = null;
            GameMode mode = null;
            boolean change = false;

            if (args.argsLength() == 0) { // check self
                // check current player
                CommandBook.inst().checkPermission(sender, "commandbook.gamemode.check");
                player = PlayerUtil.checkPlayer(sender);
                mode = player.getGameMode();
            } else {
                if (args.hasFlag('c')) { //check other player
                    CommandBook.inst().checkPermission(sender, "commandbook.gamemode.check.other");
                    player = PlayerUtil.matchSinglePlayer(sender, args.getString(0));
                    mode = player.getGameMode();
                } else {
                    change = true;

                    // we're going to assume that the first arg of one is mode, but the first of two is player
                    // if they want to check another player, they should use -c instead, since we can't guess
                    // reliably whether (with a single arg) they meant a player or a mode
                    String modeString = null;
                    if (args.argsLength() == 1) { // self mode
                        CommandBook.inst().checkPermission(sender, "commandbook.gamemode.change");
                        modeString = args.getString(0);
                        player = PlayerUtil.checkPlayer(sender);
                    } else { // 2 - first is player, second mode
                        CommandBook.inst().checkPermission(sender, "commandbook.gamemode.change.other");
                        player = PlayerUtil.matchSinglePlayer(sender, args.getString(0));
                        modeString = args.getString(1);
                    }

                    try {
                        mode = GameMode.valueOf(modeString.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        try {
                            mode = GameMode.getByValue(Integer.parseInt(modeString));
                        } catch (NumberFormatException ex) {}
                    }
                    if (mode == null) {
                        throw new CommandException("Unrecognized gamemode: " + modeString + ".");
                    }
                }
            }

            if (player == null || mode == null) {
                throw new CommandException("Something went wrong, please try again.");
            }

            String message = null;
            if (change) {
                if (player.getGameMode() == mode) {
                    message = " already had gamemode " + mode.toString();
                    change = false;
                } else {
                    message = " changed to gamemode " + mode.toString();
                }
            } else {
                message = " currently has gamemode " + mode.toString();
            }
            if (change) {
                player.setGameMode(mode);
            }
            sender.sendMessage("Player " + (CommandBook.inst().useDisplayNames ? player.getDisplayName() : player.getName())
                    + ChatColor.YELLOW + message + ".");
            return;
        }
    }
}
