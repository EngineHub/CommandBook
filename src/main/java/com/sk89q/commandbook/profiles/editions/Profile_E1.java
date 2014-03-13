package com.sk89q.commandbook.profiles.editions;

import com.sk89q.commandbook.profiles.UnversionedProfile;
import com.sk89q.commandbook.profiles.ProfileSettings;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public interface Profile_E1 extends UnversionedProfile {
    public String getCreatorName();
    public UUID getCreatorUUID();

    public ProfileSettings getSettings();

    public ItemStack[] getArmourContents();

    public void setArmourContents(ItemStack[] armourContents);

    public ItemStack[] getInventoryContents();

    public void setInventoryContents(ItemStack[] inventoryContents);

    public double getHealth();
    public void setHealth(double health);

    public int getHunger();
    public void setHunger(int hunger);

    public float getSaturation();
    public void setSaturation(float saturation);

    public float getExhaustion();
    public void setExhaustion(float exhaustion);

    public int getLevel();
    public void setLevel(int level);

    public float getExperience();
    public void setExperience(float experience);

    public Location getLocation();
    public void setLocation(Location location);
}
