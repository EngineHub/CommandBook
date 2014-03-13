package com.sk89q.commandbook.profiles.binary.implementations;

import com.sk89q.commandbook.profiles.ProfileSettings;
import com.sk89q.commandbook.profiles.editions.Profile_E1;
import com.sk89q.commandbook.util.item.ItemUtil;
import com.sk89q.commandbook.util.item.itemstack.SerializableItemStack;
import com.sk89q.commandbook.util.item.itemstack.SerializationUtil;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * A binary profile class:
 *  - Edition            1
 *  - Revision           0
 */
public class BinaryProfile_E1_R0 implements Profile_E1 {

    // Owner
    private String creatorName;
    private UUID creatorUUID;

    // Vitals
    private double health = 20;
    private int hunger = 20;
    private float saturation = 5;
    private float exhaustion = 0;

    // Inventory Storage
    // For Serialization
    private SerializableItemStack[] armourContents = null;
    private SerializableItemStack[] inventoryContents = null;
    // For Usage
    private transient ItemStack[] cacheArmourContents = null;
    private transient ItemStack[] cacheInventoryContents = null;

    // Experience
    private int level = 0;
    private float experience = 0;

    // Location
    // For Serialization
    private String worldName = "";
    private double x, y, z;
    private float yaw, pitch;
    // For Usage
    private transient Location location = null;


    public BinaryProfile_E1_R0(String creatorName) {
        this.creatorName = creatorName;
    }

    public BinaryProfile_E1_R0(UUID creatorUUID) {
        this.creatorUUID = creatorUUID;
    }

    @Override
    public String getCreatorName() {
        return creatorName;
    }

    @Override
    public UUID getCreatorUUID() {
        return creatorUUID;
    }

    @Override
    public ProfileSettings getSettings() {
        return null;
    }

    @Override
    public ItemStack[] getArmourContents() {
        if (cacheArmourContents == null) {
            cacheArmourContents = SerializationUtil.convert(armourContents);
        }
        return ItemUtil.clone(cacheArmourContents);
    }

    @Override
    public void setArmourContents(ItemStack[] armourContents) {
        Validate.notNull(armourContents);
        this.armourContents = SerializationUtil.convert(armourContents);
    }

    @Override
    public ItemStack[] getInventoryContents() {
        if (cacheInventoryContents == null) {
            cacheInventoryContents = SerializationUtil.convert(inventoryContents);
        }
        return ItemUtil.clone(cacheInventoryContents);
    }

    @Override
    public void setInventoryContents(ItemStack[] inventoryContents) {
        Validate.notNull(inventoryContents);
        this.inventoryContents = SerializationUtil.convert(inventoryContents);
    }

    @Override
    public double getHealth() {
        return health;
    }

    @Override
    public void setHealth(double health) {
        this.health = health;
    }

    @Override
    public int getHunger() {
        return hunger;
    }

    @Override
    public void setHunger(int hunger) {
        this.hunger = hunger;
    }

    @Override
    public float getSaturation() {
        return saturation;
    }

    @Override
    public void setSaturation(float saturation) {
        this.saturation = saturation;
    }

    @Override
    public float getExhaustion() {
        return exhaustion;
    }

    @Override
    public void setExhaustion(float exhaustion) {
        this.exhaustion = exhaustion;
    }

    @Override
    public int getLevel() {
        return level;
    }

    @Override
    public void setLevel(int level) {
        this.level = level;
    }

    @Override
    public float getExperience() {
        return experience;
    }

    @Override
    public void setExperience(float experience) {
        this.experience = experience;
    }

    @Override
    public Location getLocation() {
        if (location == null) {
            if (worldName.isEmpty()) {
                return null;
            } else {
                location = new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
            }
        }
        return location.clone();
    }

    @Override
    public void setLocation(Location location) {
        if (location == null) {
            this.location = null;
            this.worldName = "";
            return;
        }
        this.location = location.clone();
        this.worldName = location.getWorld().getName();
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
        this.yaw = location.getYaw();
        this.pitch = location.getPitch();
    }

}
