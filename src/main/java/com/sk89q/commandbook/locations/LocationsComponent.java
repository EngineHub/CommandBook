/*
 * CommandBook
 * Copyright (C) 2011 sk89q <http://www.sk89q.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.commandbook.locations;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.commands.PaginatedResult;
import com.sk89q.commandbook.util.InputUtil;
import com.sk89q.commandbook.util.entity.player.PlayerUtil;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.zachsthings.libcomponents.TemplateComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

import java.util.*;

/**
 * Parent class for components that use a RootLocationManager<NamedLocation> and deal with locations
 */
@TemplateComponent
public abstract class LocationsComponent extends BukkitComponent {

    private final String name;

    private LocalConfiguration config;
    private RootLocationManager<NamedLocation> manager;

    protected LocationsComponent(String name) {
        this.name = name;
    }

    @Override
    public void enable() {
        config = configure(new LocalConfiguration());
        LocationManagerFactory<LocationManager<NamedLocation>> warpsFactory =
                new FlatFileLocationsManager.LocationsFactory(CommandBook.inst().getDataFolder(), name + "s");
        manager = new RootLocationManager<NamedLocation>(warpsFactory, config.perWorld);
        CommandBook.registerEvents(new WorldListener());
    }

    @Override
    public void reload() {
        super.reload();
        configure(config);
    }

    private static class LocalConfiguration extends ConfigurationBase {
        @Setting("per-world") public boolean perWorld;
        @Setting("limits") public List<String> limits = new ArrayList<String>();
    }


    public RootLocationManager<NamedLocation> getManager() {
        return manager;
    }

    private class WorldListener implements Listener {
        @EventHandler
        public void loadWorld(WorldLoadEvent event) {
            manager.updateWorlds(event.getWorld());
        }

        @EventHandler
        public void unloadWorld(WorldUnloadEvent event) {
            manager.updateWorlds(event.getWorld());
        }
    }

    // -- Command helper methods

    public void info(String name, World world, CommandSender sender) throws CommandException {

        NamedLocation loc = getManager().get(world, name);
        if (loc == null) {
            throw new CommandException("No " + this.name.toLowerCase() + " by that name could be found in " + world.getName() + ".");
        }

        // Resolve the world name
        String worldN = loc.getWorldName();
        if (worldN == null) {
            worldN = CommandBook.server().getWorlds().get(0).getName();
        }

        // Resolve the quards
        Location l = loc.getLocation();
        int x = l.getBlockX();
        int y = l.getBlockY();
        int z = l.getBlockZ();

        // Print the header
        sender.sendMessage(ChatColor.GOLD + this.name + " Information for: "
                + ChatColor.BLUE + loc.getName().toUpperCase());
        // Print the owner details
        sender.sendMessage(ChatColor.YELLOW + "Owner:");
        sender.sendMessage(ChatColor.YELLOW + " - " + ChatColor.WHITE + loc.getOwnerName());
        // Print the Location details
        sender.sendMessage(ChatColor.YELLOW + "Location: ");
        sender.sendMessage(ChatColor.YELLOW + " - World: " + ChatColor.WHITE + worldN);
        sender.sendMessage(ChatColor.YELLOW + " - X: " + ChatColor.WHITE + x
                + ChatColor.YELLOW + ", Y: " + ChatColor.WHITE + y
                + ChatColor.YELLOW + ", Z: " + ChatColor.WHITE + z);
    }

    public void create(String name, Location location, Player player) throws CommandException {
        if (!config.limits.isEmpty()) {
            CommandBook inst = CommandBook.inst();
            if (!inst.hasPermission(player,
                    location.getWorld(), "commandbook." + this.name.toLowerCase() + ".limits.unlimited")) {
                int held = getManager().get(location.getWorld(), player.getUniqueId()).size();
                found: {
                    for (String entry : config.limits) {
                        String[] parts = entry.split(":");
                        if (parts.length < 2) continue;
                        try {
                            int val = Integer.parseInt(parts[1]);
                            if (val > held) {
                                if (inst.hasPermission(player, location.getWorld(),
                                        "commandbook." + this.name.toLowerCase() + ".limits." + parts[0])) {
                                    break found;
                                }
                            }
                        } catch (NumberFormatException ignored) { }
                    }
                    throw new CommandException("You don't have permission to create any more " + this.name.toLowerCase() + "s!");
                }
            }
        }
        try {
            getManager().create(name, location, player);
        } catch (IllegalArgumentException ex) {
            throw new CommandException("Invalid " + this.name.toLowerCase() + " name!");
        }
    }

    public void remove(String name, World world, CommandSender sender) throws CommandException {
        NamedLocation loc = getManager().get(world, name);
        if (loc == null) {
            throw new CommandException("No " + this.name.toLowerCase() + " by that name could be found in " + world.getName() + ".");
        }
        if (!(sender instanceof Player) || !((Player) sender).getUniqueId().equals(loc.getOwnerID())) {
            CommandBook.inst().checkPermission(sender, "commandbook." + this.name.toLowerCase() + ".remove.other");
        }

        getManager().remove(world, name);
        sender.sendMessage(ChatColor.YELLOW + this.name + ": " + name.toUpperCase() + " removed.");
    }

    public void list(CommandContext args, CommandSender sender) throws CommandException {
        World world = null;
        if (getManager().isPerWorld()) {
            if (args.hasFlag('w')) {
                world = InputUtil.LocationParser.matchWorld(sender, args.getFlag('w'));
            } else {
                world = PlayerUtil.checkPlayer(sender).getWorld();
            }
            if (world == null) throw new CommandException("Error finding world to use!");
        }
        UUID targetID = null;
        if (args.hasFlag('o')) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(args.getFlag('o'));
            if (player != null) {
                targetID = player.getUniqueId();
            }
            if (targetID == null) throw new CommandException("No owner by that name found!");
        }
        List<NamedLocation> locations = getManager().getLocations(world);
        Iterator<NamedLocation> it = locations.iterator();
        UUID senderID = null;
        boolean canSeeOther = CommandBook.inst().hasPermission(sender, world, "commandbook.warp.list.other");
        try {
            senderID = PlayerUtil.checkPlayer(sender).getUniqueId();
        } catch (CommandException e) {
            canSeeOther = true;
        }
        while (it.hasNext()) {
            NamedLocation next = it.next();
            if (targetID != null && !next.getOwnerID().equals(targetID)) {
                it.remove();
                continue;
            }
            if (!canSeeOther && !next.getOwnerID().equals(senderID)) {
                it.remove();
                continue;
            }
        }
        getListResult().display(sender, locations, args.getFlagInteger('p', 1));
    }

    public abstract PaginatedResult<NamedLocation> getListResult();
}
