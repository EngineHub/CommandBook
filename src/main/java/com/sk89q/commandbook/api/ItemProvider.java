package com.sk89q.commandbook.api;

import org.bukkit.inventory.ItemStack;

/**
 * An interface that another Plugin may implement to provide
 * items to CommandBook for use in kits
 */
public interface ItemProvider {
    /**
     * Return a newly-created ItemStack for the given key.
     *
     * If the key is not something this provider handles, it should
     * return null.
     *
     * @param itemKey An identifying key for this item, as entered into a kit configuration
     * @return A new ItemStack, or null if this item is not handled by the provider.
     */
    public ItemStack getItem(String itemKey);
}
