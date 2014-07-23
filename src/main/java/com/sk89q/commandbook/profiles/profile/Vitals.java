package com.sk89q.commandbook.profiles.profile;

public class Vitals {
    private final double health;
    private final double hunger;
    private final double saturation;
    private final double exhaustion;

    public Vitals(double health, double hunger, double saturation, double exhaustion) {
        this.health = health;
        this.hunger = hunger;
        this.saturation = saturation;
        this.exhaustion = exhaustion;
    }

    public double getHealth() {
        return health;
    }

    public double getHunger() {
        return hunger;
    }

    public double getSaturation() {
        return saturation;
    }

    public double getExhaustion() {
        return exhaustion;
    }
}