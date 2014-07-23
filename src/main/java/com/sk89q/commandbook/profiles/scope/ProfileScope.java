package com.sk89q.commandbook.profiles.scope;

import com.sk89q.commandbook.profiles.ProfileType;

import java.io.File;

public interface ProfileScope {
    public ProfileType getType();
    public File getDir();
}
