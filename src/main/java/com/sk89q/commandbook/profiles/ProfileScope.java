package com.sk89q.commandbook.profiles;

import java.io.File;

public interface ProfileScope {
    public ProfileType getType();
    public File getDir();
}
