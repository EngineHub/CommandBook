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

package com.sk89q.commandbook.component.inventory;

import com.google.common.collect.Lists;
import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.command.argument.MultiPlayerTarget;
import com.sk89q.commandbook.command.argument.SinglePlayerTarget;
import com.sk89q.commandbook.util.ChatUtil;
import com.sk89q.commandbook.util.PaginatedResult;
import com.sk89q.commandbook.util.entity.player.PlayerUtil;
import com.sk89q.commandbook.util.item.ItemUtil;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.worldedit.blocks.BaseItem;
import com.sk89q.worldedit.command.util.CommandPermissions;
import com.sk89q.worldedit.command.util.CommandPermissionsConditionGenerator;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.annotation.param.Arg;
import org.enginehub.piston.annotation.param.ArgFlag;
import org.enginehub.piston.annotation.param.Switch;

import java.util.Arrays;

import static com.sk89q.commandbook.util.item.InventoryUtil.giveItem;
import static com.sk89q.commandbook.util.item.InventoryUtil.takeItem;

@CommandContainer(superTypes = CommandPermissionsConditionGenerator.Registration.class)
public class InventoryCommands {
    private InventoryComponent component;

    public InventoryCommands(InventoryComponent component) {
        this.component = component;
    }

    private BaseItem matchItem(String name) throws CommandException {
        return ItemUtil.getCommandItem(name);
    }

    @Command(
        name = "item",
        aliases = {"i"},
        desc = "Give an item"
    )
    @CommandPermissions({"commandbook.give"})
    public void itemCmd(Player player,
                        @Switch(name = 'd', desc = "drop")
                            boolean drop,
                        @Switch(name = 'o', desc = "override stack size")
                            boolean overrideStackSize,
                        @Arg(desc = "item description")
                            String itemDescription,
                        @Arg(desc = "amount", def = "0")
                            int amount) throws CommandException {
        BaseItem item = matchItem(itemDescription);
        if (item == null) {
            throw new CommandException("Something went wrong parsing the item info!");
        }

        int amt = component.getConfig().defaultItemStackSize;
        if (amount != 0) {
            amt = amount;
        }

        giveItem(player, item, amt, Lists.newArrayList(player), component, drop, overrideStackSize);
    }

    @Command(
        name = "give",
        desc = "Give an item"
    )
    @CommandPermissions({"commandbook.give", "commandbook.give.other"})
    public void giveCmd(CommandSender sender,
                        @Switch(name = 'd', desc = "drop")
                            boolean drop,
                        @Switch(name = 'o', desc = "override stack size")
                            boolean overrideStackSize,
                        @Arg(desc = "player(s) to target")
                            MultiPlayerTarget targetPlayers,
                        @Arg(desc = "item description")
                            String itemDescription,
                        @Arg(desc = "amount", def = "0")
                            int amount) throws CommandException {
        // Check permissions!
        for (Player player : targetPlayers) {
            if (player == sender) {
                CommandBook.inst().checkPermission(sender, "commandbook.give");
            } else {
                CommandBook.inst().checkPermission(sender, "commandbook.give.other");
            }
        }

        BaseItem item = matchItem(itemDescription);
        if (item == null) {
            throw new CommandException("Something went wrong parsing the item info!");
        }

        int amt = component.getConfig().defaultItemStackSize;
        if (amount != 0) {
            amt = amount;
        }

        giveItem(sender, item, amt, targetPlayers, component, drop, overrideStackSize);
    }

    @Command(
        name = "clear",
        desc = "Clear your inventory"
    )
    @CommandPermissions({"commandbook.clear", "commandbook.clear.other"})
    public void clearCmd(CommandSender sender,
                         @Switch(name = 'a', desc = "full inventory")
                             boolean clearFullInventory,
                         @Switch(name = 's', desc = "single item")
                             boolean clearSingleItem,
                         @Arg(desc = "player(s) to target", def = "")
                             MultiPlayerTarget targetPlayers) throws CommandException {
        if (targetPlayers == null) {
            targetPlayers = new MultiPlayerTarget(PlayerUtil.checkPlayer(sender));
        }

        // Check permissions!
        for (Player player : targetPlayers) {
            if (player == sender) {
                CommandBook.inst().checkPermission(sender, "commandbook.clear");
            } else {
                CommandBook.inst().checkPermission(sender, "commandbook.clear.other");
            }
        }

        boolean included = false;
        for (Player player : targetPlayers) {
            Inventory inventory = player.getInventory();

            if (clearSingleItem) {
                player.setItemInHand(null);
            } else {
                for (int i = (clearFullInventory ? 0 : 9); i < 36; i++) {
                    inventory.setItem(i, null);
                }

                if (clearFullInventory) {
                    // Armor slots
                    for (int i = 36; i <= 39; i++) {
                        inventory.setItem(i, null);
                    }
                }
            }

            // Tell the user about the given item
            if (player.equals(sender)) {
                if (clearFullInventory) {
                    player.sendMessage(ChatColor.YELLOW
                        + "Your inventory has been cleared.");
                } else {
                    player.sendMessage(ChatColor.YELLOW
                        + "Your inventory has been cleared. Use -a to clear ALL.");
                }

                // Keep track of this
                included = true;
            } else {
                player.sendMessage(ChatColor.YELLOW
                    + "Your inventory has been cleared by "
                    + ChatUtil.toColoredName(sender, ChatColor.YELLOW));

            }
        }

        // The player didn't receive any items, then we need to send the
        // user a message so s/he know that something is indeed working
        if (!included) {
            sender.sendMessage(ChatColor.YELLOW
                + "Inventories cleared.");
        }
    }

    @Command(
        name = "more",
        desc = "Get more of an item"
    )
    @CommandPermissions({"commandbook.more", "commandbook.more.other"})
    public void moreCmd(CommandSender sender,
                        @Switch(name = 'a', desc = "full inventory")
                            boolean allItems,
                        @Switch(name = 'i', desc = "infinite supply")
                            boolean infinite,
                        @Switch(name = 'o', desc = "override stack size")
                            boolean overrideStackSize,
                        @Arg(desc = "player(s) to target", def = "")
                            MultiPlayerTarget targetPlayers) throws CommandException {
        if (targetPlayers == null) {
            targetPlayers = new MultiPlayerTarget(PlayerUtil.checkPlayer(sender));
        }

        // Check permissions!
        if (infinite) {
            CommandBook.inst().checkPermission(sender, "commandbook.more.infinite");
        } else if (overrideStackSize) {
            CommandBook.inst().checkPermission(sender, "commandbook.override.maxstacksize");
        }

        for (Player player : targetPlayers) {
            if (player == sender) {
                CommandBook.inst().checkPermission(sender, "commandbook.more");
            } else {
                CommandBook.inst().checkPermission(sender, "commandbook.more.other");
            }
        }

        boolean included = false;
        for (Player player : targetPlayers) {
            Inventory inventory = player.getInventory();

            if (allItems) {
                for (int i = 0; i < 39; i++) {
                    ItemUtil.expandStack(inventory.getItem(i), infinite, overrideStackSize);
                }
            } else {
                ItemUtil.expandStack(player.getItemInHand(), infinite, overrideStackSize);
            }

            // Tell the user about the given item
            if (player.equals(sender)) {
                player.sendMessage(ChatColor.YELLOW
                    + "Your item(s) has been expanded in stack size.");

                // Keep track of this
                included = true;
            } else {
                player.sendMessage(ChatColor.YELLOW
                    + "Your item(s) has been expanded in stack size by "
                    + ChatUtil.toColoredName(sender, ChatColor.YELLOW));

            }
        }

        // The player didn't receive any items, then we need to send the
        // user a message so s/he know that something is indeed working
        if (!included) {
            sender.sendMessage(ChatColor.YELLOW
                + "Stack sizes increased.");
        }
    }

    @Command(
        name = "take",
        desc = "Take an item"
    )
    @CommandPermissions({"commandbook.take", "commandbook.take.other"})
    public void takeCmd(CommandSender sender,
                        @Arg(desc = "player to target")
                            SinglePlayerTarget targetPlayer,
                        @Arg(desc = "item description")
                            String itemDescription,
                        @Arg(desc = "amount", def = "0")
                            int amount) throws CommandException {
        // Check permissions!
        for (Player player : targetPlayer) {
            if (player == sender) {
                CommandBook.inst().checkPermission(sender, "commandbook.take");
            } else {
                CommandBook.inst().checkPermission(sender, "commandbook.take.other");
            }
        }

        BaseItem item = matchItem(itemDescription);
        if (item == null) {
            throw new CommandException("Something went wrong parsing the item info!");
        }

        int amt = component.getConfig().defaultItemStackSize;
        if (amount != 0) {
            amt = amount;
        }

        takeItem(sender, item, amt, targetPlayer.get());
    }

    @Command(
        name = "stack",
        desc = "Stack items"
    )
    @CommandPermissions({"commandbook.stack"})
    public void stackCmd(Player player) {
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
                    if (item2.isSimilar(item)) {
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

    @Command(
        name = "repair",
        desc = "Repair items"
    )
    @CommandPermissions({"commandbook.repair", "commandbook.repair.other"})
    public void repairCmd(CommandSender sender,
                          @Switch(name = 'a', desc = "repair all")
                              boolean repairAll,
                          @Switch(name = 'h', desc = "repair hotbar")
                              boolean repairHotbar,
                          @Switch(name = 'e', desc = "repair equipemnt")
                              boolean repairEquipment,
                          @Arg(desc = "player(s) to target", def = "")
                              MultiPlayerTarget targetPlayers) throws CommandException {
        if (targetPlayers == null) {
            targetPlayers = new MultiPlayerTarget(PlayerUtil.checkPlayer(sender));
        }

        // Check permissions!
        for (Player player : targetPlayers) {
            if (player == sender) {
                CommandBook.inst().checkPermission(sender, "commandbook.repair");
            } else {
                CommandBook.inst().checkPermission(sender, "commandbook.repair.other");
            }
        }

        boolean included = false;
        for (Player player : targetPlayers) {
            Inventory inventory = player.getInventory();

            if (!repairAll && !repairHotbar && !repairEquipment) {
                ItemStack stack = player.getItemInHand();
                ItemMeta stackMeta = stack.getItemMeta();
                if (stackMeta instanceof Damageable) {
                    ((Damageable) stackMeta).setDamage(0);
                    stack.setItemMeta(stackMeta);
                }
            } else {
                if (repairAll || repairHotbar) {
                    for (int i = (repairAll ? 36 : 8); i >= 0; --i) {
                        ItemStack stack = inventory.getItem(i);
                        if (stack == null) {
                            continue;
                        }

                        ItemMeta stackMeta = stack.getItemMeta();
                        if (stackMeta instanceof Damageable) {
                            ((Damageable) stackMeta).setDamage(0);
                            stack.setItemMeta(stackMeta);
                        }
                    }
                }

                if (repairAll || repairEquipment) {
                    // Armor slots
                    for (int i = 36; i <= 39; i++) {
                        ItemStack stack = inventory.getItem(i);
                        if (stack == null) {
                            continue;
                        }

                        ItemMeta stackMeta = stack.getItemMeta();
                        if (stackMeta instanceof Damageable) {
                            ((Damageable) stackMeta).setDamage(0);
                            stack.setItemMeta(stackMeta);
                        }
                    }
                }
            }

            // Tell the user about the given item
            if (player.equals(sender)) {
                if (repairAll || repairHotbar || repairEquipment) {
                    player.sendMessage(ChatColor.YELLOW
                        + "Your items have been repaired.");
                } else {
                    player.sendMessage(ChatColor.YELLOW
                        + "Your held item has been repaired. Use -a to repair all.");
                }

                // Keep track of this
                included = true;
            } else {
                player.sendMessage(ChatColor.YELLOW
                    + "One or more of your item(s) has been repaired by "
                    + ChatUtil.toColoredName(sender, ChatColor.YELLOW));

            }
        }

        // The player didn't receive any items, then we need to send the
        // user a message so s/he know that something is indeed working
        if (!included) {
            sender.sendMessage(ChatColor.YELLOW + "Items repaired.");
        }
    }

    @Command(
        name = "enchantments",
        aliases = {"listenchant", "lsenchant"},
        desc = "List available enchantments"
    )
    @CommandPermissions({"commandbook.stack"})
    public void enchantmentsCmd(CommandSender sender,
                                @ArgFlag(name = 'p', desc = "page", def = "1")
                                    int page) throws CommandException {
        new PaginatedResult<Enchantment>(ChatColor.GOLD + "Enchantments") {
            @Override
            public String format(Enchantment entry) {
                return ChatColor.BLUE + entry.getName().toUpperCase() + ChatColor.YELLOW
                    + " (ID: " + ChatColor.WHITE + entry.getKey() + ChatColor.YELLOW
                    + ", Max Level: " + ChatColor.WHITE + entry.getMaxLevel() + ChatColor.YELLOW + ')';
            }
        }.display(sender, Arrays.asList(Enchantment.values()), page);
    }
}
