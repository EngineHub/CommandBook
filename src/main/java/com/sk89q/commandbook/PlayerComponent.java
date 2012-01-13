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
import com.sk89q.worldedit.blocks.ItemType;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

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
                    player = PlayerUtil.matchSinglePlayer(sender, args.getString(0));
                    if (player != sender) {
                        CommandBook.inst().checkPermission(sender, "commandbook.gamemode.check.other");
                    } else {
                        CommandBook.inst().checkPermission(sender, "commandbook.gamemode.check");
                    }
                    mode = player.getGameMode();
                } else {
                    change = true;

                    // we're going to assume that the first arg of one is mode, but the first of two is player
                    // if they want to check another player, they should use -c instead, since we can't guess
                    // reliably whether (with a single arg) they meant a player or a mode
                    String modeString = null;
                    if (args.argsLength() == 1) { // self mode
                        modeString = args.getString(0);
                        player = PlayerUtil.checkPlayer(sender);
                    } else { // 2 - first is player, second mode
                        player = PlayerUtil.matchSinglePlayer(sender, args.getString(0));
                        modeString = args.getString(1);
                    }

                    if (player != sender) {
                        CommandBook.inst().checkPermission(sender, "commandbook.gamemode.change.other");
                    } else {
                        CommandBook.inst().checkPermission(sender, "commandbook.gamemode.change");
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
        }
        @Command(aliases = {"heal"}, 
        		usage = "[player]", desc = "Heal a player",
        		flags = "s", min = 0, max = 1)
        @CommandPermissions({"commandbook.heal", "commandbook.heal.other"})
        public void heal(CommandContext args,CommandSender sender) throws CommandException {
            
            Iterable<Player> targets = null;
            boolean included = false;
            
            // Detect arguments based on the number of arguments provided
            if (args.argsLength() == 0) {
                targets = PlayerUtil.matchPlayers(PlayerUtil.checkPlayer(sender));
                
            } else if (args.argsLength() == 1) {            
                targets = PlayerUtil.matchPlayers(sender, args.getString(0));
                
            }
            
            for (Player player : targets) {
                if (player != sender) {
                    // Check permissions!
                    CommandBook.inst().checkPermission(sender, "commandbook.heal.other");
                } else {
                    CommandBook.inst().checkPermission(sender, "commandbook.heal");
                }
            }

            for (Player player : targets) {
                player.setHealth(20);
                player.setFoodLevel(20);
                
                // Tell the user
                if (player.equals(sender)) {
                    player.sendMessage(ChatColor.YELLOW + "Healed!");
                    
                    // Keep track of this
                    included = true;
                } else {
                    player.sendMessage(ChatColor.YELLOW + "Healed by "
                            + PlayerUtil.toName(sender) + ".");
                    
                }
            }
            
            // The player didn't receive any items, then we need to send the
            // user a message so s/he know that something is indeed working
            if (!included && args.hasFlag('s')) {
                sender.sendMessage(ChatColor.YELLOW.toString() + "Players healed.");
            }
        }
        @Command(aliases = {"slay"}, 
        		usage = "[player]", desc = "Slay a player", 
        		flags = "s", min = 0, max = 1)
        @CommandPermissions({"commandbook.slay"})
        
        public void slay(CommandContext args, CommandSender sender) throws CommandException {
            
            Iterable<Player> targets = null;
            boolean included = false;
            
            // Detect arguments based on the number of arguments provided
            if (args.argsLength() == 0) {
                targets = PlayerUtil.matchPlayers(PlayerUtil.checkPlayer(sender));
            } else if (args.argsLength() == 1) {            
                targets = PlayerUtil.matchPlayers(sender, args.getString(0));
            }

            for (Player player : targets) {
                if (player != sender) {
                    // Check permissions!
                    CommandBook.inst().checkPermission(sender, "commandbook.slay.other");
                } else {
                    CommandBook.inst().checkPermission(sender, "commandbook.slay");
                }
            }

            for (Player player : targets) {
                player.setHealth(0);
                
                // Tell the user
                if (player.equals(sender)) {
                    player.sendMessage(ChatColor.YELLOW + "Slain!");
                    
                    // Keep track of this
                    included = true;
                } else {
                    player.sendMessage(ChatColor.YELLOW + "Slain by "
                            + PlayerUtil.toName(sender) + ".");
                    
                }
            }
            
            // The player didn't receive any items, then we need to send the
            // user a message so s/he know that something is indeed working
            if (!included && args.hasFlag('s')) {
                sender.sendMessage(ChatColor.YELLOW.toString() + "Players slain.");
            }
        }   
        @Command(aliases = {"locate"}, usage = "[player]", desc = "Locate a player", max = 1)
        @CommandPermissions({"commandbook.locate"})
        public void locate(CommandContext args, CommandSender sender) throws CommandException {
            
            Player player = PlayerUtil.checkPlayer(sender);
            
            if (args.argsLength() == 0) {
                player.setCompassTarget(player.getWorld().getSpawnLocation());
                
                sender.sendMessage(ChatColor.YELLOW.toString() + "Compass reset to spawn.");
            } else {
                Player target = PlayerUtil.matchSinglePlayer(sender, args.getString(0));
                player.setCompassTarget(target.getLocation());
                
                sender.sendMessage(ChatColor.YELLOW.toString() + "Compass repointed.");
            }
        }
        @Command(aliases = {"stack"}, 
        		usage = "", desc = "Stack items",
        		max = 0)
        @CommandPermissions({"commandbook.stack"})
        public void stack(CommandContext args, CommandSender sender) throws CommandException {
            
            Player player = PlayerUtil.checkPlayer(sender);
            boolean ignoreMax = CommandBook.inst().hasPermission(player, "commandbook.stack.illegitimate");
            boolean ignoreDamaged = CommandBook.inst().hasPermission(player, "commandbook.stack.damaged");
            
            ItemStack[] items = player.getInventory().getContents();
            int len = items.length;

            int affected = 0;
            
            for (int i = 0; i < len; i++) {
                ItemStack item = items[i];

                // Avoid infinite stacks and stacks with durability
                if (item == null || item.getAmount() <= 0
                        || (!ignoreMax && item.getMaxStackSize() == 1)) {
                    continue;
                }

                int max = ignoreMax ? 64 : item.getMaxStackSize();

                if (item.getAmount() < max) {
                    int needed = max - item.getAmount(); // Number of needed items until max

                    // Find another stack of the same type
                    for (int j = i + 1; j < len; j++) {
                        ItemStack item2 = items[j];

                        // Avoid infinite stacks and stacks with durability
                        if (item2 == null || item2.getAmount() <= 0
                                || (!ignoreMax && item.getMaxStackSize() == 1)) {
                            continue;
                        }

                        // Same type?
                        // Blocks store their color in the damage value
                        if (item2.getTypeId() == item.getTypeId() &&
                                ((!ItemType.usesDamageValue(item.getTypeId()) && ignoreDamaged)
                                        || item.getDurability() == item2.getDurability()) &&
                                item.getEnchantments().equals(item2.getEnchantments())) {
                            // This stack won't fit in the parent stack
                            if (item2.getAmount() > needed) {
                                item.setAmount(64);
                                item2.setAmount(item2.getAmount() - needed);
                                break;
                            // This stack will
                            } else {
                                items[j] = null;
                                item.setAmount(item.getAmount() + item2.getAmount());
                                needed = 64 - item.getAmount();
                            }

                            affected++;
                        }
                    }
                }
            }

            if (affected > 0) {
                player.getInventory().setContents(items);
            }

            player.sendMessage(ChatColor.YELLOW + "Items compacted into stacks!");
        }
    }
}
