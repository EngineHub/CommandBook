package com.sk89q.commandbook.profiles.profile;

public class Profile {

    private final String name;
    private Vitals vitals;
    private Inventory inventory;

    public Profile(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Vitals getVitals() {
        return vitals;
    }

    public void setVitals(Vitals vitals) {
        this.vitals = vitals;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }
}
