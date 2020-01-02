package com.sk89q.commandbook.component.locations;

import org.bukkit.Location;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class LocationTarget {
    private Location location;
    private boolean[] relative;

    public LocationTarget(Location location, boolean[] relative) {
        checkNotNull(location);
        checkArgument(relative.length == 3);

        this.location = location;
        this.relative = relative;
    }

    public Location get() {
        return location;
    }

    public boolean isRelativeX() {
        return relative[0];
    }

    public boolean isRelativeY() {
        return relative[1];
    }

    public boolean isRelativeZ() {
        return relative[2];
    }
}
