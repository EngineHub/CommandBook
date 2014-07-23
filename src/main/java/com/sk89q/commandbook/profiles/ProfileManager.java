package com.sk89q.commandbook.profiles;

import com.sk89q.commandbook.profiles.profile.Profile;
import com.sk89q.util.yaml.YAMLFormat;
import com.sk89q.util.yaml.YAMLProcessor;

import java.io.File;

public class ProfileManager {

    public void saveProfile(ProfileScope scope, Profile profile) {
        write(profile, new File(scope.getDir(), profile.getName() + ".yml"));
    }

    private void write(Profile profile, File file) {
        YAMLProcessor processor = new YAMLProcessor(file, false, YAMLFormat.EXTENDED);
        throw new UnsupportedOperationException();
    }

    public Profile getProfile(ProfileScope scope, String name) {
        return load(new File(scope.getDir(), name + ".yml"));
    }

    private Profile load(File file) {
        YAMLProcessor processor = new YAMLProcessor(file, false, YAMLFormat.EXTENDED);
        throw new UnsupportedOperationException();
    }
}
