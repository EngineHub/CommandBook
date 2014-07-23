package com.sk89q.commandbook.profiles.scope;

import com.sk89q.commandbook.profiles.ProfileType;

import java.io.File;
import java.util.UUID;

public class PersonalScope extends ProfileScope {

    private final UUID player;

    public PersonalScope(UUID player) {
        this.player = player;
    }

    @Override
    public ProfileType getType() {
        return ProfileType.PERSONAL;
    }

    @Override
    public File getDir() {
        return new File(super.getDir(), "personal/" + player + '/');
    }
}
