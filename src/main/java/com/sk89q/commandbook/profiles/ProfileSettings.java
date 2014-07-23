package com.sk89q.commandbook.profiles;

public class ProfileSettings {

    private final String name;

    private boolean vitals = false;
    private boolean inventory = true;
    private boolean experience = false;
    private boolean location = false;

    public ProfileSettings(String name, boolean vitals, boolean inventory, boolean expirence, boolean location) {
        this.name = name;
        this.vitals = vitals;
        this.inventory = inventory;
        this.experience = expirence;
        this.location = location;
    }

    public String getName() {
        return name;
    }

    public boolean hasVitals() {
        return vitals;
    }

    public boolean hasInventory() {
        return inventory;
    }

    public boolean hasExperience() {
        return experience;
    }

    public boolean hasLocation() {
        return location;
    }
}
