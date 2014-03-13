package com.sk89q.commandbook.profiles.binary;

import com.sk89q.commandbook.profiles.ProfileSettings;
import com.sk89q.commandbook.profiles.binary.implementations.BinaryProfile_E1_R0;
import com.sk89q.commandbook.profiles.editions.Profile_E1;
import org.bukkit.entity.Player;

public class BinaryProfileFactory {
    public static Profile_E1 createProfile(Player player, ProfileSettings settings) {
        Profile_E1 profile = new BinaryProfile_E1_R0(player.getName());
        // TODO do things with settings
        return profile;
    }
}
