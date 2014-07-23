package com.sk89q.commandbook.profiles;

import com.sk89q.commandbook.profiles.profile.Profile;
import com.sk89q.commandbook.profiles.scope.ProfileScope;
import com.sk89q.util.yaml.YAMLFormat;
import com.sk89q.util.yaml.YAMLProcessor;

import java.io.File;
import java.io.IOException;

public class YAMLProfileManager implements ProfileManager {
    @Override
    public boolean saveProfile(ProfileScope scope, Profile profile) {
        return write(profile, new File(scope.getDir(), profile.getName() + ".yml"));
    }

    private boolean write(Profile profile, File file) {

        if (!file.exists()) {
            File parent = file.getParentFile();
            if (!parent.exists()) {
                if (!parent.mkdirs()) {
                    return false;
                }
            }
        }

        YAMLProcessor processor = new YAMLProcessor(file, false, YAMLFormat.EXTENDED);
        try {
            processor.load();
            processor.clear();
            processor.setProperty("name", profile.getName());
            // TODO A lot of persistence stuff
            return processor.save();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean remProfile(ProfileScope scope, Profile profile) {
        return delete(new File(scope.getDir(), profile.getName() + ".yml"));
    }

    private boolean delete(File file) {
        return file.exists() && file.delete();
    }

    @Override
    public Profile getProfile(ProfileScope scope, String name) {
        return load(new File(scope.getDir(), name + ".yml"));
    }

    private Profile load(File file) {
        if (!file.exists()) return null;
        YAMLProcessor processor = new YAMLProcessor(file, false, YAMLFormat.EXTENDED);
        try {
            processor.load();
            String name = processor.getString("name");
            Profile profile = new Profile(name);
            // TODO A lot of persistence stuff
            return profile;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
