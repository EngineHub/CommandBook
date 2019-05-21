package com.sk89q.commandbook.util.item;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.InventoryComponent;
import com.sk89q.commandbook.util.ChatUtil;
import com.sk89q.minecraft.util.commands.CommandException;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;

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
                                Collection<Player> targets, InventoryComponent component, boolean drop, boolean overrideStackSize)
            throws CommandException {

        boolean infinite = false; // Is the stack infinite?

        int maxStackSize = overrideStackSize ? 64 : item.getType().getMaxStackSize();

        component.checkAllowedItem(sender, item.getType());

        // Check for invalid amounts
        if (amt == 0 || amt < -1) {
            throw new CommandException("Invalid item amount!");
        } else if (amt == -1) {
            // Check to see if the player can give infinite items
            CommandBook.inst().checkPermission(sender, "commandbook.give.infinite");
            infinite = true;
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

        int targetQuantity = targets.size();
        // Send the message ahead of time so that we can follow up with any errors
        if (targetQuantity > 1 || !targets.contains(sender)) {
            sender.sendMessage(ChatColor.YELLOW.toString() + targetQuantity + " player(s)"
                    + " have been given " + getAmountText(false, infinite, amt)
                    + ' ' + item.getType().name() + '.');
        }


        for (Player player : targets) {
            int left = amt;

            // Give individual stacks
            while (left > 0 || infinite) {
                int givenAmt = Math.min(maxStackSize, left);
                item = item.clone(); // This prevents the possibility of a linked ItemStack issue
                item.setAmount(givenAmt);
                left -= givenAmt;

                // The -d flag drops the items naturally on the ground instead
                // of directly giving the player the item
                if (drop) {
                    player.getWorld().dropItemNaturally(player.getLocation(), item);
                } else {
                    Collection<ItemStack> result = player.getInventory().addItem(item).values();
                    // Check for items that couldn't be added
                    if (!result.isEmpty()) {
                        for (ItemStack stack : result) {
                            left += stack.getAmount();
                            sender.sendMessage(ChatColor.RED + getAmountText(true, infinite, left)
                                    + ' ' + ItemUtil.toItemName(stack.getType())
                                    + " could not be given to "
                                    + player.getName() + " (their inventory is probably full)!");
                        }
                        // End the loop so we don't waste time, seeing as the item cannot be added
                        break;
                    }
                }

                if (amt == -1) {
                    break;
                }
            }

            // workaround for having inventory open while giving items (eg TMI mod)
            player.updateInventory();

            String amtString = getAmountText(false, infinite, amt - left);
            // Tell the user about the given item
            if (player.equals(sender)) {
                player.sendMessage(ChatColor.YELLOW + "You've been given " + amtString + " "
                        + ItemUtil.toItemName(item.getType()) + ".");
            } else {
                player.sendMessage(ChatColor.YELLOW + "Given from "
                        + ChatUtil.toColoredName(sender, ChatColor.YELLOW) + ": "
                        + amtString + " "
                        + ItemUtil.toItemName(item.getType()) + ".");

            }
        }
    }

    private static String getAmountText(boolean sentenceStart, boolean infinite, int amount) {
        return infinite ? (sentenceStart ? "An" : "an") + " infinite stack of" : String.valueOf(amount);
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
        if (target.getInventory().contains(item.getType())) {
            target.getInventory().removeItem(item);

            target.sendMessage(ChatColor.YELLOW + "Taken from "
                    + ChatUtil.toColoredName(sender, ChatColor.YELLOW) + ": "
                    + amt + " "
                    + ItemUtil.toItemName(item.getType()) + ".");

            sender.sendMessage(ChatColor.YELLOW.toString() + amt + " "
                    + ItemUtil.toItemName(item.getType()) + " has been taken.");
        } else {
            sender.sendMessage(ChatColor.YELLOW.toString() + target.getName()
                    + " has no " + ItemUtil.toItemName(item.getType()) + ".");
        }
    }
}
