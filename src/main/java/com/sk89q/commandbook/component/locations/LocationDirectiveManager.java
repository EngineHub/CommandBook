package com.sk89q.commandbook.component.locations;

import java.util.HashSet;
import java.util.Set;

public class LocationDirectiveManager {
    private Set<String> directives = new HashSet<>();

    public Set<String> getSupportedDirectives() {
        return directives;
    }
}
