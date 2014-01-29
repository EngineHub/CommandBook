package com.sk89q.commandbook.util.item;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.InventoryComponent;
import com.sk89q.commandbook.util.ChatUtil;
import com.sk89q.minecraft.util.commands.CommandException;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class InventoryUtil {

    /**
     * Process an item give request.
     *
     * @param sender
     * @param item
     * @param amt
     * @param targets
     * @param component
     * @param drop
     * @throws com.sk89q.minecraft.util.commands.CommandException
     */
    @SuppressWarnings("deprecation")
    public static void giveItem(CommandSender sender, ItemStack item, int amt,
                                Iterable<Player> targets, InventoryComponent component, boolean drop, boolean overrideStackSize)
            throws CommandException {

        boolean included = false; // Is the command sender also receiving items?

        int maxStackSize = overrideStackSize ? 64 : item.getType().getMaxStackSize();

        component.checkAllowedItem(sender, item.getTypeId(), item.getDurability());

        // Check for invalid amounts
        if (amt == 0 || amt < -1) {
            throw new CommandException("Invalid item amount!");
        } else if (amt == -1) {
            // Check to see if the player can give infinite items
            CommandBook.inst().checkPermission(sender, "commandbook.give.infinite");
        } else if (overrideStackSize) {
            CommandBook.inst().checkPermission(sender, "commandbook.override.maxstacksize");
        } else if (amt > maxStackSize * 5) {
            // Check to see if the player can give stacks of this size
            if (!CommandBook.inst().hasPermission(sender, "commandbook.give.stacks.unlimited")) {
                throw new CommandException("More than 5 stacks is too excessive.");
            }
        } else if (amt > maxStackSize /* && amt < max * 5 */) {
            // Check to see if the player can give stacks
            CommandBook.inst().checkPermission(sender, "commandbook.give.stacks");
        }

        if(amt > 2240 && !drop) amt = 2240;

        // Get a nice amount name
        String amtText = amt == -1 ? "an infinite stack of" : String.valueOf(amt);

        for (Player player : targets) {
            int left = amt;

            // Give individual stacks
            while (left > 0 || amt == -1) {
                int givenAmt = Math.min(maxStackSize, left);
                item.setAmount(givenAmt);
                left -= givenAmt;

                // The -d flag drops the items naturally on the ground instead
                // of directly giving the player the item
                if (drop) {
                    player.getWorld().dropItemNaturally(player.getLocation(), item);
                } else {
                    player.getInventory().addItem(item);
                }

                if (amt == -1) {
                    break;
                }
            }

            // workaround for having inventory open while giving items (eg TMI mod)
            player.updateInventory();

            // Tell the user about the given item
            if (player.equals(sender)) {
                player.sendMessage(ChatColor.YELLOW + "You've been given " + amtText + " "
                        + ItemUtil.toItemName(item.getTypeId()) + ".");

                // Keep track of this
                included = true;
            } else {
                player.sendMessage(ChatColor.YELLOW + "Given from "
                        + ChatUtil.toColoredName(sender, ChatColor.YELLOW) + ": "
                        + amtText + " "
                        + ItemUtil.toItemName(item.getTypeId()) + ".");

            }
        }

        // The player didn't receive any items, then we need to send the
        // user a message so s/he know that something is indeed working
        if (!included) {
            sender.sendMessage(ChatColor.YELLOW.toString() + amtText + " "
                    + ItemUtil.toItemName(item.getTypeId()) + " has been given.");
        }
    }

    /**
     * Process an item give request.
     *
     * @param sender
     * @param item
     * @param amt
     * @param target
     * @throws CommandException
     */
    public static void takeItem(CommandSender sender, ItemStack item, int amt,
                                Player target)
            throws CommandException {

        // Check for invalid amounts
        if (amt <= 0) {
            throw new CommandException("Invalid item amount!");
        }


        item.setAmount(amt);
        if (target.getInventory().contains(item.getTypeId())) {
            target.getInventory().removeItem(item);

            target.sendMessage(ChatColor.YELLOW + "Taken from "
                    + ChatUtil.toColoredName(sender, ChatColor.YELLOW) + ": "
                    + amt + " "
                    + ItemUtil.toItemName(item.getTypeId()) + ".");

            sender.sendMessage(ChatColor.YELLOW.toString() + amt + " "
                    + ItemUtil.toItemName(item.getTypeId()) + " has been taken.");
        } else {
            sender.sendMessage(ChatColor.YELLOW.toString() + target.getName()
                    + " has no " + ItemUtil.toItemName(item.getTypeId()) + ".");
        }
    }
}
