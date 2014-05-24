package com.sk89q.commandbook.item;

import com.sk89q.commandbook.util.item.ItemUtil;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.regex.Pattern;

@ComponentInformation(friendlyName = "Item Manager", desc = "Item management system.")
public class ItemComponent extends BukkitComponent {

    private static HashMap<Pattern, ItemProvider> items = new HashMap<Pattern, ItemProvider>();
    private LocalConfiguration config;

    static {
        addItem(new DefaultProvider());
    }
    @Override
    public void enable() {
        config = configure(new LocalConfiguration());
    }

    @Override
    public void reload() {
        super.reload();
        configure(config);
    }

    // TODO Implement configurable string -> id mapping
    private static class LocalConfiguration extends ConfigurationBase {
        @Setting("data-split")
        public String dataSplit = ":";
    }

    public static void addItem(ItemProvider... newItems) {
        for (ItemProvider p : newItems) {
            items.put(p.getPattern(), p);
        }
    }

    public ItemIdentifier parse(String input) {
        String[] split = input.split(config.dataSplit);
        String data = "";
        if (split.length > 1) {
            data = split[1];
        }
        return ItemUtil.identify(split[0], data);
    }

    /**
     * Finds the first item suitable item provider and request it make an item
     * of the specified name with the specified details.
     *
     * @param id The item identifier to process
     * @return   The found {@link org.bukkit.inventory.ItemStack} if one exist,
     *           or null if one is not
     */
    public ItemStack request(ItemIdentifier id) {
        for (Map.Entry<Pattern, ItemProvider> entry : items.entrySet()) {
            if (entry.getKey().matcher(id.getName()).matches()) {
                ItemStack result = entry.getValue().build(id);
                if (result != null) return result;
            }
        }
        return null;
    }

    /**
     * Finds all suitable item providers and request they make an item
     * of the specified name with the specified details.
     *
     * This method takes extra precaution to ensure that all items in the
     * returned list are not null. Any null values will be removed.
     *
     * @param id The item identifier to process
     * @return   A {@link java.util.HashSet} of all found {@link org.bukkit.inventory.ItemStack}
     */
    public Set<ItemStack> requestAll(ItemIdentifier id) {
        Set<ItemStack> created = new HashSet<ItemStack>();
        for (Map.Entry<Pattern, ItemProvider> entry : items.entrySet()) {
            if (entry.getKey().matcher(id.getName()).matches()) {
                created.add(entry.getValue().build(id));
            }
        }
        created.removeAll(Collections.singleton(null));
        return created;
    }
}
