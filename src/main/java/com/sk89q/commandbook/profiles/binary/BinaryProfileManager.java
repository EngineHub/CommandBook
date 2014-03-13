package com.sk89q.commandbook.profiles.binary;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.profiles.ProfileManager;
import com.sk89q.commandbook.profiles.ProfileSettings;
import com.sk89q.commandbook.profiles.UnversionedProfile;
import com.sk89q.commandbook.profiles.editions.Profile_E1;
import org.bukkit.entity.Player;

public class BinaryProfileManager extends ProfileManager {

    private final String workingDir = CommandBook.inst().getDataFolder().getPath() + "/profiles/";

    @Override
    public Profile_E1 createProfile(Player player, ProfileSettings settings) {
        return BinaryProfileFactory.createProfile(player, settings);
    }

    @Override
    public Profile_E1 getProfile(String domain, String profileName) {
        // TODO add the code to input a binary file from the disk
        Object o = null;
        if (!(o instanceof UnversionedProfile)) {
            // TODO throw new something
        }

        if (o instanceof Profile_E1) {
            return (Profile_E1) o;
        }
        return null;
    }

    @Override
    public boolean saveProfile(String domain, String profileName, Profile_E1 profile) {
        // TODO add the code to write a binary file to the disk
        return false;
    }

    @Override
    public boolean deleteProfile(String domain, String profileName) {
        // TODO add the code to delete a binary file from the disk
        return false;
    }
}
