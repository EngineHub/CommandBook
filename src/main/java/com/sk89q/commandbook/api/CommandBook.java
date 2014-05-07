package com.sk89q.commandbook.api;

import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

/**
 * The public API for CommandBook.
 *
 * Another plugin can retrieve an instance to the API by casting the
 * plugin instance, avoiding knowledge of any internals.
 */
public interface CommandBook {
    /**
     * This allows another Plugin can use to add an ItemProvider to CommandBook.
     *
     * This will allow admins to add custom items from other plugins in a CommandBook kit.
     *
     * @param plugin The Plugin instance that owns the provider.
     * @param provider The ItemProvider that handles creating items for this Plugin
     */
    public void addItemProvider(Plugin plugin, ItemProvider provider);

    /**
     * Retrieve a warp location by name
     *
     * @param name The name of the warp to lookup
     * @return The Location of the warp, or null if unknown
     */
    public Location getWarp(String name);
}
