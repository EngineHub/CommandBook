package com.sk89q.commandbook.profiles.scope;

import com.sk89q.commandbook.profiles.ProfileType;

import java.io.File;

public class TagScope extends ProfileScope {
    @Override
    public ProfileType getType() {
        return ProfileType.TAG;
    }

    @Override
    public File getDir() {
        return new File(super.getDir(), "tag/");
    }
}
