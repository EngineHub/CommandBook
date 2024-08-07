// $Id$
/*
 * Copyright (C) 2010, 2011 sk89q <http://www.sk89q.com>
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

package com.sk89q.commandbook.component.locations;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.util.entity.player.UUIDUtil;
import org.apache.commons.lang3.Validate;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.io.*;
import java.util.*;

import static com.sk89q.commandbook.CommandBook.logger;
import static com.sk89q.commandbook.util.NestUtil.getNestedList;

public class FlatFileLocationsManager implements LocationManager<NamedLocation> {

    private World castWorld;
    private final File file;
    private Map<String, NamedLocation> locations = new HashMap<String, NamedLocation>();
    private final Map<String, List<NamedLocation>> unloadedLocations = new HashMap<String, List<NamedLocation>>();
    private final String type;

    /**
     * Construct the manager.
     *
     * @param file The file locations are stored in
     * @param type The name for the type of location being loaded
     */
    public FlatFileLocationsManager(File file, String type) {
        this.file = file;
        this.type = type;
    }

    public void castWorld(World world) {
        castWorld = world;
    }

    public void load() throws IOException {
        FileInputStream input = null;
        Map<String, NamedLocation> locs = new HashMap<String, NamedLocation>();
        boolean needsSaved = false;

        if (file.getParentFile().exists() || file.getParentFile().mkdirs()) {
            if (!file.exists()) {
                file.createNewFile();
            }
        }

        try {
            input = new FileInputStream(file);
            InputStreamReader streamReader = new InputStreamReader(input, "utf-8");
            BufferedReader reader = new BufferedReader(streamReader);

            CSVReader csv = new CSVReader(reader);
            String[] line;
            while ((line = csv.readNext()) != null) {
                int lineLen = line.length;
                if (lineLen < 8) {
                    logger().warning(type + " data file has an invalid line with < 8 fields");
                } else {
                    try {
                        int i = 0;
                        String name = line[i++].trim().replace(" ", "");
                        String worldName = line[i++]; // Set to null if the world exists
                        String creator = line[i++];
                        double x = Double.parseDouble(line[i++]);
                        double y = Double.parseDouble(line[i++]);
                        double z = Double.parseDouble(line[i++]);
                        float pitch = Float.parseFloat(line[i++]);
                        float yaw = Float.parseFloat(line[i++]);

                        World world = CommandBook.server().getWorld(worldName);


                        if (world != null) {
                            // We shouldn't have this warp
                            if (castWorld != null && !castWorld.equals(world)) {
                                continue;
                            }
                        }

                        Location loc = new Location(world, x, y, z, yaw, pitch);
                        NamedLocation warp = new NamedLocation(name, loc);
                        warp.setWorldName(worldName);

                        try {
                            warp.setCreatorID(UUID.fromString(creator));
                        } catch (IllegalArgumentException ex) {
                            logger().finest("Converting " + type + " " + name + "'s owner record to UUID...");
                            UUID creatorID = UUIDUtil.convert(creator);
                            if (creatorID != null) {
                                warp.setCreatorID(creatorID);
                                needsSaved = true;
                                logger().finest("Success!");
                            } else {
                                warp.setCreatorName(creator);
                                logger().warning(type + " " + name + "'s owner could not be converted!");
                            }
                        }
                        if (world == null) {
                            getNestedList(unloadedLocations, worldName).add(warp);
                        } else {
                            locs.put(name.toLowerCase(), warp);
                        }
                    } catch (IllegalArgumentException e) {
                        if (e instanceof NumberFormatException) {
                            logger().warning(type + " data file has an invalid line with an invalid UUID field");
                        } else {
                            logger().warning(type + " data file has an invalid line with non-numeric numeric fields");
                        }
                    }
                }
            }

            this.locations = locs;

            if (castWorld != null) {
                logger().info(locs.size() + " " + type + "(s) loaded for "
                        + castWorld.getName());
            } else {
                logger().info(locs.size() + " " + type + "(s) loaded");
            }
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException ignore) {
                }
            }
        }
        if (needsSaved) save();
    }

    public void save() throws IOException {
        FileOutputStream output = null;

        try {
            output = new FileOutputStream(file);
            OutputStreamWriter streamWriter = new OutputStreamWriter(output, "utf-8");
            BufferedWriter writer = new BufferedWriter(streamWriter);

            CSVWriter csv = new CSVWriter(writer);

            synchronized (this) {
                Set<NamedLocation> toStore = new HashSet<NamedLocation>();
                for (List<NamedLocation> locList : unloadedLocations.values()) {
                    toStore.addAll(locList);
                }
                toStore.addAll(locations.values());

                for (NamedLocation warp : toStore) {

                    UUID ID = warp.getCreatorID();
                    csv.writeNext(new String[] {
                            warp.getName(),
                            warp.getWorldName() != null ? warp.getWorldName()
                                    : warp.getLocation().getWorld().getName(),
                            String.valueOf(ID != null ? ID : warp.getCreatorName()),
                            String.valueOf(warp.getLocation().getX()),
                            String.valueOf(warp.getLocation().getY()),
                            String.valueOf(warp.getLocation().getZ()),
                            String.valueOf(warp.getLocation().getPitch()),
                            String.valueOf(warp.getLocation().getYaw()),
                            });
                }

            }

            csv.flush();
            csv.close();
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException ignore) {
                }
            }
        }
    }

    public void updateWorlds() {
        for (Iterator<Map.Entry<String, List<NamedLocation>>> i = unloadedLocations.entrySet().iterator(); i.hasNext();) {
            Map.Entry<String, List<NamedLocation>> entry = i.next();
            World world = CommandBook.server().getWorld(entry.getKey());
            if (world == null) continue;
            i.remove();
            for (NamedLocation warp : entry.getValue()) {
                warp.getLocation().setWorld(world);
                locations.put(warp.getName().toLowerCase(), warp);
            }
        }
        for (Iterator<NamedLocation> i = locations.values().iterator(); i.hasNext();) {
            NamedLocation loc = i.next();
            if (loc.getWorldName() != null) {    
                if (CommandBook.server().getWorld(loc.getWorldName()) == null) {
                    i.remove();
                    loc.getLocation().setWorld(null);
                    getNestedList(unloadedLocations, loc.getWorldName()).add(loc);
                }
            }
        }
    }

    public NamedLocation get(String id) {
        return locations.get(id.toLowerCase());
    }

    public boolean remove(String id) {
        return locations.remove(id.toLowerCase()) != null;
    }

    public List<NamedLocation> getLocations() {
        return new ArrayList<NamedLocation>(locations.values());
    }

    public NamedLocation create(String id, Location loc, Player player) {
        id = id.trim();
        Validate.isTrue(id.matches("^[a-zA-Z0-9-_]*$"), "Location ID contains invalid characters!");
        NamedLocation warp = new NamedLocation(id, loc);
        locations.put(id.toLowerCase(), warp);
        if (player != null) {
            warp.setCreatorName(player.getName());
            warp.setCreatorID(player.getUniqueId());
        } else {
            warp.setCreatorName("");
        }
        return warp;
    }

    public static class LocationsFactory implements LocationManagerFactory<LocationManager<NamedLocation>> {

        private final File rootDir;
        private final String type;

        public LocationsFactory(File rootDir, String type) {
            this.rootDir = rootDir;
            this.type = type;
        }

        public LocationManager<NamedLocation> createManager() {
            return new FlatFileLocationsManager(new File(rootDir, type.toLowerCase() + ".csv"), type);
        }

        public LocationManager<NamedLocation> createManager(World castWorld) {
            return new FlatFileLocationsManager(
                    new File(rootDir, type.toLowerCase() + File.separator + castWorld.getName() + ".csv"), type);
        }
    }

}
