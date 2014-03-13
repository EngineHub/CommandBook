package com.sk89q.commandbook.util.item.itemstack;

import com.sk89q.commandbook.util.item.itemstack.implementations.SerializableItemStack_G1;
import org.bukkit.inventory.ItemStack;

public class SerializableItemStackFactory {
    public static SerializableItemStack makeItemStack(ItemStack itemStack) {
        return new SerializableItemStack_G1(itemStack);
    }
}
