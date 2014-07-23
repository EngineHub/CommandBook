package com.sk89q.commandbook.profiles;

import com.sk89q.commandbook.profiles.profile.Inventory;
import com.sk89q.commandbook.profiles.profile.Profile;
import com.sk89q.commandbook.profiles.profile.Vitals;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ProfileFactory {
    public Profile create(Player player, ProfileSettings settings) {
        Profile profile = new Profile(settings.getName());
        vitals(profile, player, settings);
        inventory(profile, player, settings);
        return profile;
    }

    private void vitals(Profile profile, Player player, ProfileSettings settings) {
        if (!settings.hasVitals()) return;
        profile.setVitals(new Vitals(
                player.getHealth(),
                player.getFoodLevel(),
                player.getSaturation(),
                player.getExhaustion()
        ));
    }

    private void inventory(Profile profile, Player player, ProfileSettings settings) {
        if (!settings.hasInventory()) return;
        List<ItemStack> inv = new ArrayList<ItemStack>();
        for (ItemStack item : player.getInventory()) {
            inv.add(item);
        }
        profile.setInventory(new Inventory(inv));
    }
}
