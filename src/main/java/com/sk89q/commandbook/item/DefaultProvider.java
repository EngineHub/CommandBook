package com.sk89q.commandbook.item;

import com.sk89q.worldedit.blocks.ItemType;
import org.bukkit.inventory.ItemStack;

import java.util.regex.Pattern;

public class DefaultProvider implements ItemProvider {

    private Pattern pattern = Pattern.compile("^[A-Za-z]*$");

    @Override
    public Pattern getPattern() {
        return pattern;
    }

    @Override
    public ItemStack build(ItemIdentifier id) {
        ItemType type = ItemType.lookup(id.getName());
        if (type == null) return null;
        return new ItemStack(type.getID());
    }
}
