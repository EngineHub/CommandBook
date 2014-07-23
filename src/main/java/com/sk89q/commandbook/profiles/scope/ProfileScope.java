package com.sk89q.commandbook.profiles.scope;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.profiles.ProfileType;

import java.io.File;

public abstract class ProfileScope {
    public abstract ProfileType getType();
    public File getDir() {
        return new File(CommandBook.inst().getDataFolder(), "profiles/");
    }
}
