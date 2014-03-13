package com.sk89q.commandbook.util.item.itemstack;

import org.bukkit.inventory.ItemStack;

public class SerializationUtil {
    public static SerializableItemStack[] convert(ItemStack[] stacks) {

        SerializableItemStack[] returnStack = new SerializableItemStack[stacks.length];
        for (int i = 0; i < stacks.length; i++) {
            returnStack[i] = stacks[i] == null ? null : SerializableItemStackFactory.makeItemStack(stacks[i]);
        }
        return returnStack;
    }

    public static ItemStack[] convert(SerializableItemStack[] stacks) {

        ItemStack[] returnStack = new ItemStack[stacks.length];
        for (int i = 0; i < stacks.length; i++) {
            returnStack[i] = stacks[i] == null ? null : stacks[i].toBukkit();
        }
        return returnStack;
    }
}
