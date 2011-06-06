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

package com.sk89q.commandbook.warps;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import com.sk89q.commandbook.CommandBookPlugin;

public class FlatFileWarpsManager implements WarpsManager {
    
    private static final Logger logger = Logger.getLogger("Minecraft.CommandBook");
    
    private CommandBookPlugin plugin;
    private World castWorld;
    private File file;
    private Map<String, Warp> warps = new HashMap<String, Warp>();
    
    /**
     * Construct the manager.
     * 
     * @param plugin
     * @param file 
     */
    public FlatFileWarpsManager(File file, CommandBookPlugin plugin) {
        this.plugin = plugin;
        this.file = file;
    }

    public void castWorld(World world) {
        castWorld = world;
    }
    
    public void load() throws IOException {
        FileInputStream input = null;
        Map<String, Warp> warps = new HashMap<String, Warp>();
        
        file.getParentFile().mkdirs();
        
        try {
            input = new FileInputStream(file);
            InputStreamReader streamReader = new InputStreamReader(input, "utf-8");
            BufferedReader reader = new BufferedReader(streamReader);
            
            CSVReader csv = new CSVReader(reader);
            String[] line;
            while ((line = csv.readNext()) != null) {
                if (line.length < 7) {
                    logger.warning("CommandBook: Warps data file has an invalid line with < 7 fields");
                } else {
                    try {
                        String name = line[0].trim().replace(" ", "");
                        String worldName = line[1]; // Set to null if the world exists
                        String creator = line[2];
                        double x = Double.parseDouble(line[3]);
                        double y = Double.parseDouble(line[4]);
                        double z = Double.parseDouble(line[5]);
                        float pitch = Float.parseFloat(line[6]);
                        float yaw = Float.parseFloat(line[7]);
                        
                        World world = plugin.getServer().getWorld(worldName);
                        
                        if (world == null) {
                            // We shouldn't have this warp
                            if (castWorld != null) {
                                continue;
                            }
                            
                            world = plugin.getServer().getWorlds().get(0);
                        } else {
                            // We shouldn't have this warp
                            if (castWorld != null && !castWorld.equals(world)) {
                                continue;
                            }
                            
                            worldName = null;
                        }
                        
                        Location loc = new Location(world, x, y, z, pitch, yaw);
                        Warp warp = new Warp(name, loc);
                        warp.setWorldName(worldName);
                        warp.setCreatorName(creator);
                        warps.put(name.toLowerCase(), warp);
                    } catch (NumberFormatException e) {
                        logger.warning("CommandBook: Warps data file has an invalid line with non-numeric numeric fields");
                    }
                }
            }
            
            this.warps = warps;
            
            if (castWorld != null) {
                logger.warning("CommandBook: " + warps.size() + " warp(s) loaded for "
                        + castWorld.getName());
            } else {
                logger.warning("CommandBook: " + warps.size() + " warp(s) loaded");
            }
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public void save() throws IOException {
        FileOutputStream output = null;
        
        try {
            output = new FileOutputStream(file);
            OutputStreamWriter streamWriter = new OutputStreamWriter(output, "utf-8");
            BufferedWriter writer = new BufferedWriter(streamWriter);
            
            CSVWriter csv = new CSVWriter(writer);
            
            synchronized (this) {
                for (Map.Entry<String, Warp> entry : warps.entrySet()) {
                    Warp warp = entry.getValue();
                    
                    csv.writeNext(new String[] {
                            warp.getName(),
                            warp.getWorldName() != null ? warp.getWorldName()
                                    : warp.getLocation().getWorld().getName(),
                            warp.getCreatorName(),
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
                } catch (IOException e) {
                }
            }
        }
    }

    public Warp getWarp(String id) {
        return warps.get(id.toLowerCase());
    }

    public boolean removeWarp(String id) {
        return warps.remove(id.toLowerCase()) != null;
    }

    public Warp createWarp(String id, Location loc, Player player) {
        id = id.trim();
        Warp warp = new Warp(id, loc);
        warps.put(id.toLowerCase(), warp);
        if (player != null) {
            warp.setCreatorName(player.getName());
        } else {
            warp.setCreatorName("");
        }
        return warp;
    }
    
    public static class Factory implements WarpsManagerFactory {

        private CommandBookPlugin plugin;
        public File rootDir;
        
        public Factory(File rootDir, CommandBookPlugin plugin) {
            this.rootDir = rootDir;
            this.plugin = plugin;
        }

        public WarpsManager createManager() {
            return new FlatFileWarpsManager(new File(rootDir, "warps.csv"), plugin);
        }

        public WarpsManager createManager(World castWorld) {
            return new FlatFileWarpsManager(
                    new File(rootDir, "warps/" + castWorld.getName() + ".csv"), plugin);
        }
        
    }

}
