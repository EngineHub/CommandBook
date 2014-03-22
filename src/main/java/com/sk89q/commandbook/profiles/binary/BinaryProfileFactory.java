package com.sk89q.commandbook.profiles.binary;

import com.sk89q.commandbook.profiles.ProfileSettings;
import com.sk89q.commandbook.profiles.binary.implementations.BinaryProfile_E1_R0;
import com.sk89q.commandbook.profiles.editions.Profile_E1;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;

public class BinaryProfileFactory {
    public static Profile_E1 createProfile(Player player, ProfileSettings settings) {
        Profile_E1 profile = new BinaryProfile_E1_R0(player.getName());

        // Inventory Storage
        if (settings.storeInventory) {
            PlayerInventory inv = player.getInventory();
            profile.setArmourContents(inv.getArmorContents());
            profile.setInventoryContents(inv.getContents());
        }

        // Player Vital Storage
        if (settings.storeVitals) {
            profile.setHealth(player.getHealth());
            profile.setHunger(player.getFoodLevel());
            profile.setSaturation(player.getSaturation());
            profile.setExhaustion(player.getExhaustion());
        }

        // Experience Storage
        if (settings.storeExperience) {
            profile.setLevel(player.getLevel());
            profile.setExperience(player.getExp());
        }

        // Location Storage
        if (settings.storeLocation) {
            profile.setLocation(player.getLocation());
        }

        return profile;
    }
}
