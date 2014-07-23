package com.sk89q.commandbook.profiles;

import com.sk89q.commandbook.profiles.profile.Profile;

public interface ProfileManager {
    public boolean saveProfile(ProfileScope scope, Profile profile);
    public Profile getProfile(ProfileScope scope, String name);
}
