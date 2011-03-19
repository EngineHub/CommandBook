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

package com.sk89q.commandbook.kits;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.inventory.ItemStack;
import com.sk89q.commandbook.CommandBookPlugin;

/**
 * Manages kits.
 * 
 * @author sk89q
 */
public class FlatFileKitsManager implements KitManager {
    
    protected static final Logger logger = Logger.getLogger("Minecraft.CommandBook");
    
    protected CommandBookPlugin plugin;
    protected File file;
    protected Map<String, Kit> kits = new HashMap<String, Kit>();
    
    /**
     * Construct the manager.
     * 
     * @param plugin
     * @param file 
     */
    public FlatFileKitsManager(File file, CommandBookPlugin plugin) {
        this.plugin = plugin;
        this.file = file;
    }
    
    public void load() {
        FileInputStream input = null;
        Map<String, Kit> kits = new HashMap<String, Kit>();
        
        try {
            input = new FileInputStream(file);
            InputStreamReader streamReader = new InputStreamReader(input, "utf-8");
            BufferedReader reader = new BufferedReader(streamReader);
            Kit kit = null;
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                
                if (line.length() == 0
                        || line.charAt(0) == '#'
                        || line.charAt(0) == ';') {
                    continue;
                }
                
                // Match a kit's name
                if (line.matches("^\\[[^\\]]+\\]$")) {
                    String id = line.substring(1, line.length() - 1).trim().toLowerCase();
                    kit = new Kit();
                    kits.put(id, kit);
                    continue;
                }
                
                // No kit defined yet!
                if (kit == null) {
                    logger.warning("CommandBook: Missing \"[kitname]\" section for "
                            + line);
                    continue;
                }
                
                String[] parts = line.split(",");
                ItemStack item = plugin.getItem(parts[0]);
                
                if (item == null) {
                    logger.warning("CommandBook: Unknown kit item '" + item + "'");
                    continue;
                }
                
                // Attempt to parse an amount
                if (parts.length >= 2) {
                    try {
                        item.setAmount(Integer.parseInt(parts[1]));
                    } catch (NumberFormatException e) {
                        logger.warning("CommandBook: Invalid amount: '" + parts[1] + "'");
                    }
                }
                
                kit.addItem(item);
            }
            
            logger.info("CommandBook: " + kits.size() + " kit(s) loaded.");
        } catch (FileNotFoundException e) {
        } catch (UnsupportedEncodingException e) {
        } catch (IOException e) {
            logger.warning("CommandBook: Failed to load kits.txt: "
                    + e.getMessage());
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                }
            }
        }
        
        this.kits = kits;
    }
    
    public Kit getKit(String id) {
        return kits.get(id.toLowerCase());
    }

    public Map<String, Kit> getKits() {
        return kits;
    }
    
}
