package com.sk89q.commandbook.profiles.profile;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class Inventory {
    private List<ItemStack> inventory = new ArrayList<ItemStack>();

    public Inventory() { }

    public Inventory(List<ItemStack> inventory) {
        this.inventory = inventory;
    }

    public List<ItemStack> getItems() {
        return inventory;
    }
}
