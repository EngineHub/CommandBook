package com.sk89q.commandbook.profiles.scope;

import java.io.File;
import java.util.UUID;

public class PersonalTagScope extends PersonalScope {
    public PersonalTagScope(UUID player) {
        super(player);
    }

    @Override
    public File getDir() {
        return new File(super.getDir(), "tags/");
    }
}
