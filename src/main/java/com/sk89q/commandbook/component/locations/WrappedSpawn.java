package com.sk89q.commandbook.component.locations;

import org.bukkit.Location;
import org.bukkit.World;

public class WrappedSpawn {
    private final World world;
    private float pitch;
    private float yaw;

    public WrappedSpawn(World world, float pitch, float yaw) {
        this.world = world;
        this.pitch = pitch;
        this.yaw = yaw;
    }

    public Location getLocation() {
        Location loc = world.getSpawnLocation();
        loc.setPitch(getPitch());
        loc.setYaw(getYaw());
        return loc;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public float getYaw() {
        return yaw;
    }

    public String getWorldName() {
        return world.getName();
    }

    public World getWorld() {
        return world;
    }
}
