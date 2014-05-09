package com.sk89q.commandbook.item;

import org.bukkit.inventory.ItemStack;

import java.util.regex.Pattern;

public interface ItemProvider {
    public Pattern getPattern();
    public ItemStack build(ItemIdentifier details);
}
