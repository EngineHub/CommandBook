package com.sk89q.commandbook.profiles;

import com.sk89q.commandbook.profiles.editions.Profile_E1;
import org.bukkit.entity.Player;

public abstract class ProfileManager {

    private String defaultDomain;

    public ProfileManager() {
        defaultDomain = "default";
    }

    public Profile_E1 getProfile(String profileName) {
        return getProfile(defaultDomain, profileName);
    }

    public boolean saveProfile(String profileName, Profile_E1 profile) {
        return saveProfile(defaultDomain, profileName, profile);
    }

    public boolean deleteProfile(String profileName) {
        return deleteProfile(defaultDomain, profileName);
    }

    public abstract Profile_E1 createProfile(Player player, ProfileSettings settings);
    public abstract Profile_E1 getProfile(String domain, String profileName);
    public abstract boolean saveProfile(String domain, String profileName, Profile_E1 profile);
    public abstract boolean deleteProfile(String domain, String profileName);
}
