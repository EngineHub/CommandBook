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

import com.sk89q.commandbook.commands.PaginatedResult;
import com.sk89q.commandbook.util.PlayerUtil;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.worldedit.blocks.ItemType;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import static com.sk89q.commandbook.CommandBookUtil.giveItem;
import static com.sk89q.commandbook.CommandBookUtil.takeItem;

@ComponentInformation(friendlyName = "Inventory",
        desc = "Inventory-related commands, such as /give and /clear, are handled in this component.")
public class InventoryComponent extends BukkitComponent {
    protected LocalConfiguration config;

    @Override
    public void enable() {
        config = configure(new LocalConfiguration());
        registerCommands(Commands.class);
    }

    @Override
    public void reload() {
        super.reload();
        configure(config);
    }

    private static class LocalConfiguration extends ConfigurationBase {
        @Setting("item-permissions-only") public boolean useItemPermissionsOnly;
        @Setting("allowed-items") public Set<Integer> allowedItems = Collections.emptySet();
        @Setting("disllowed-items") public Set<Integer> disallowedItems = Collections.emptySet();
        @Setting("default-item-stack-size") public int defaultItemStackSize = 1;
    }

    // -- Helper methods

    /**
     * Checks to see if a user can use an item.
     *
     * @param sender
     * @param id
     * @throws CommandException
     */
    public void checkAllowedItem(CommandSender sender, int id, int damage)
            throws CommandException {

        if (Material.getMaterial(id) == null || id == 0) {
            throw new CommandException("Non-existent item specified.");
        }

        // Check if the user has an override
        if (CommandBook.inst().hasPermission(sender, "commandbook.override.any-item")) {
            return;
        }

        boolean hasPermissions = CommandBook.inst().hasPermission(sender, "commandbook.items." + id)
                || CommandBook.inst().hasPermission(sender, "commandbook.items." + id + "." + damage);

        // Also check the permissions system
        if (hasPermissions) {
            return;
        }

        if (config.useItemPermissionsOnly) {
            throw new CommandException("That item is not allowed.");
        }

        if (config.allowedItems.size() > 0) {
            if (!config.allowedItems.contains(id)) {
                throw new CommandException("That item is not allowed.");
            }
        }

        if (config.disallowedItems.contains((id))) {
            throw new CommandException("That item is disallowed.");
        }
    }

    private ItemStack matchItem(String name) throws CommandException {
        return CommandBook.inst().getCommandItem(name);
    }

    public class Commands {
        @Command(aliases = {"item", "i"},
                usage = "[target] <item[:data][|enchantment:level]> [amount]", desc = "Give an item",
                flags = "do", min = 1, max = 3)
        @CommandPermissions({"commandbook.give"})
        public void item(CommandContext args, CommandSender sender) throws CommandException {
            ItemStack item = null;
            int amt = config.defaultItemStackSize;
            Iterable<Player> targets = null;

            // How this command handles parameters depends on how many there
            // are, so the following code splits the incoming input
            // into three different possibilities

            // One argument: Just the item type and amount 1
            if (args.argsLength() == 1) {
                item = matchItem(args.getString(0));
                targets = PlayerUtil.matchPlayers(PlayerUtil.checkPlayer(sender));
                // Two arguments: Item type and amount
            } else if (args.argsLength() == 2) {
                item = matchItem(args.getString(0));
                amt = args.getInteger(1);
                targets = PlayerUtil.matchPlayers(PlayerUtil.checkPlayer(sender));
                // Three arguments: Player, item type, and item amount
            } else if (args.argsLength() == 3) {
                item = matchItem(args.getString(1));
                amt = args.getInteger(2);
                targets = PlayerUtil.matchPlayers(sender, args.getString(0));

                // Make sure that this player has permission to give items to other
                // players!
                for (Player player : targets) {
                    if (player != sender) {
                        CommandBook.inst().checkPermission(sender, "commandbook.give.other");
                    }
                }
            }

            if (item == null) {
                throw new CommandException("Something went wrong parsing the item info!");
            }
            giveItem(sender, item, amt, targets, InventoryComponent.this, args.hasFlag('d'), args.hasFlag('o'));
        }

        @Command(aliases = {"give"},
                usage = "[-d] <target> <item[:data]> [amount]", desc = "Give an item",
                flags = "do", min = 2, max = 3)
        @CommandPermissions({"commandbook.give", "commandbook.give.other"})
        public void give(CommandContext args, CommandSender sender) throws CommandException {
            ItemStack item = null;
            int amt = config.defaultItemStackSize;
            Iterable<Player> targets = null;

            // How this command handles parameters depends on how many there
            // are, so the following code splits the incoming input
            // into three different possibilities

            // Two arguments: Player, item type
            if (args.argsLength() == 2) {
                targets = PlayerUtil.matchPlayers(sender, args.getString(0));
                item = matchItem(args.getString(1));
                // Three arguments: Player, item type, and item amount
            } else if (args.argsLength() == 3) {
                targets = PlayerUtil.matchPlayers(sender, args.getString(0));
                item = matchItem(args.getString(1));
                amt = args.getInteger(2);
            }

            for (Player target : targets) {
                if (target != sender) {
                    CommandBook.inst().checkPermission(sender, "commandbook.give.other");
                } else {
                    CommandBook.inst().checkPermission(sender, "commandbook.give");
                }
            }

            if (item == null) {
                throw new CommandException("Something went wrong parsing the item info!");
            }
            giveItem(sender, item, amt, targets, InventoryComponent.this, args.hasFlag('d'), args.hasFlag('o'));
        }

        @Command(aliases = {"clear"},
                usage = "[target]", desc = "Clear your inventory",
                flags = "as", min = 0, max = 1)
        @CommandPermissions({"commandbook.clear"})
        public void clear(CommandContext args, CommandSender sender) throws CommandException {

            Iterable<Player> targets;
            boolean clearAll = args.hasFlag('a');
            boolean clearSingle = args.hasFlag('s');
            boolean included = false;

            if (args.argsLength() == 0) {
                targets = PlayerUtil.matchPlayers(PlayerUtil.checkPlayer(sender));
                // A different player
            } else {
                targets = PlayerUtil.matchPlayers(sender, args.getString(0));
            }

            for (Player player : targets) {
                if (sender != player) {
                    // Make sure that this player can clear other players!
                    CommandBook.inst().checkPermission(sender, "commandbook.clear.other");
                    break;
                }
            }

            for (Player player : targets) {
                Inventory inventory = player.getInventory();

                if (clearSingle) {
                    player.setItemInHand(null);
                } else {
                    for (int i = (clearAll ? 0 : 9); i < 36; i++) {
                        inventory.setItem(i, null);
                    }

                    if (clearAll) {
                        // Armor slots
                        for (int i = 36; i <= 39; i++) {
                            inventory.setItem(i, null);
                        }
                    }
                }

                // Tell the user about the given item
                if (player.equals(sender)) {
                    if (clearAll) {
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
                            + PlayerUtil.toColoredName(sender, ChatColor.YELLOW));

                }
            }

            // The player didn't receive any items, then we need to send the
            // user a message so s/he know that something is indeed working
            if (!included) {
                sender.sendMessage(ChatColor.YELLOW
                        + "Inventories cleared.");
            }
        }

        @Command(aliases = {"more"},
                usage = "[player]", desc = "Gets more of an item",
                flags = "aio", min = 0, max = 1)
        @CommandPermissions({"commandbook.more"})
        public void more(CommandContext args, CommandSender sender) throws CommandException {

            Iterable<Player> targets;
            boolean moreAll = args.hasFlag('a');
            boolean infinite = args.hasFlag('i');
            boolean overrideStackSize = args.hasFlag('o');
            if (infinite) {
                CommandBook.inst().checkPermission(sender, "commandbook.more.infinite");
            } else if (overrideStackSize) {
                CommandBook.inst().checkPermission(sender, "commandbook.override.maxstacksize");
            }

            boolean included = false;

            if (args.argsLength() == 0) {
                targets = PlayerUtil.matchPlayers(PlayerUtil.checkPlayer(sender));
            // A different player
            } else {
                targets = PlayerUtil.matchPlayers(sender, args.getString(0));
            }

            for (Player player : targets) {
                if (player != sender) {
                    // Check permissions!
                    CommandBook.inst().checkPermission(sender, "commandbook.more.other");
                } else {
                    CommandBook.inst().checkPermission(sender, "commandbook.more");
                }
            }

            for (Player player : targets) {
                Inventory inventory = player.getInventory();

                if (moreAll) {
                    for (int i = 0; i < 39; i++) {
                        CommandBookUtil.expandStack(inventory.getItem(i), infinite, overrideStackSize);
                    }
                } else {
                    CommandBookUtil.expandStack(player.getItemInHand(), infinite, overrideStackSize);
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
                            + PlayerUtil.toColoredName(sender, ChatColor.YELLOW));

                }
            }

            // The player didn't receive any items, then we need to send the
            // user a message so s/he know that something is indeed working
            if (!included) {
                sender.sendMessage(ChatColor.YELLOW
                        + "Stack sizes increased.");
            }
        }

        @Command(aliases = {"take"},
                usage = "<target> <item[:data]> [amount]", desc = "Take an item",
                flags = "", min = 2, max = 3)
        @CommandPermissions({"commandbook.take"})
        public void take(CommandContext args, CommandSender sender) throws CommandException {
            ItemStack item = null;
            int amt = config.defaultItemStackSize;
            Player target = null;

            // Two arguments: Player, item type
            if (args.argsLength() == 2) {
                target = PlayerUtil.matchSinglePlayer(sender, args.getString(0));
                item = matchItem(args.getString(1));
                // Three arguments: Player, item type, and item amount
            } else if (args.argsLength() == 3) {
                target = PlayerUtil.matchSinglePlayer(sender, args.getString(0));
                item = matchItem(args.getString(1));
                amt = args.getInteger(2);
            }

            if (target != sender) {
                // Check permissions!
                CommandBook.inst().checkPermission(sender, "commandbook.take.other");
            } else {
                CommandBook.inst().checkPermission(sender, "commandbook.take");
            }

            if (item == null) {
                throw new CommandException("Something went wrong parsing the item info!");
            }
            takeItem(sender, item, amt, target);
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
                                    ((item.getItemMeta() == null && item2.getItemMeta() == null)
                                            || (item.getItemMeta() != null &&
                                                item.getItemMeta().equals(item2.getItemMeta())))) {
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

        @Command(aliases = {"enchantments", "listenchant", "lsenchant"}, desc = "List available enchantments", usage = "[-p page]", flags = "p:")
        public void enchantments(CommandContext args, CommandSender sender) throws CommandException {
            new PaginatedResult<Enchantment>("Name - ID - Max Level") {
                @Override
                public String format(Enchantment entry) {
                    return entry.getName() + " - " + entry.getId() + " - " + entry.getMaxLevel();
                }
            }.display(sender, Arrays.asList(Enchantment.values()), args.getFlagInteger('p', 1));
        }
    }
}
