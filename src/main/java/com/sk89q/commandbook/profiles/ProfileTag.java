package com.sk89q.commandbook.profiles;

import java.util.Collections;
import java.util.List;

public class ProfileTag {

    private final String name;
    private final List<String> groups;

    public ProfileTag(String name, List<String> groups) {
        this.name = name;
        this.groups = groups;
    }

    public String getName() {
        return name;
    }

    public List<String> getGroups() {
        return Collections.unmodifiableList(groups);
    }
}
