package com.sk89q.commandbook.util.item.itemstack;

import org.bukkit.inventory.ItemStack;

import java.io.Serializable;

public interface SerializableItemStack extends Serializable {
    public ItemStack toBukkit();
}
