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

import com.sk89q.commandbook.util.item.ItemUtil;
import org.bukkit.inventory.ItemStack;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.sk89q.commandbook.CommandBook.logger;

/**
 * Manages kits.
 *
 * @author sk89q
 */
public class FlatFileKitsManager implements KitManager {

    private static final Pattern kitPattern =
            Pattern.compile("^\\[([^\\]=]+)(?:= *([0-9]+) *)?\\]$");

    private final File file;
    private Map<String, Kit> kits = new HashMap<String, Kit>();

    /**
     * Construct the manager.
     *
     * @param file The file to read kits from
     */
    public FlatFileKitsManager(File file) {
        this.file = file;
    }

    public synchronized void load() {
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
                Matcher m = kitPattern.matcher(line);

                if (m.matches()) {
                    String id = m.group(1).replace(" ", "").trim().toLowerCase();
                    kit = new Kit();
                    kits.put(id, kit);

                    String coolDownTime = m.group(2);
                    if (coolDownTime != null) {
                        try {
                            kit.setCoolDown((long) (Double.parseDouble(coolDownTime) * 1000));
                        } catch (NumberFormatException e) {
                            logger().warning("Invalid cool down for "
                                    + line);
                            continue;
                        }
                    }

                    continue;
                }

                // No kit defined yet!
                if (kit == null) {
                    logger().warning("Missing \"[kitname]\" section for "
                            + line);
                    continue;
                }

                String[] parts = line.split(",");
                ItemStack item = ItemUtil.getItem(parts[0].replace(" ", ""));

                if (item == null) {
                    logger().warning(" Unknown kit item '" + parts[0].replaceAll(" ", "") + "'");
                    continue;
                }

                // Attempt to parse an amount
                if (parts.length >= 2) {
                    try {
                        item.setAmount(Integer.parseInt(parts[1]));
                    } catch (NumberFormatException e) {
                        logger().warning("Invalid amount: '" + parts[1] + "'");
                    }
                }

                kit.addItem(item);
            }

            logger().info(kits.size() + " kit(s) loaded.");
        } catch (FileNotFoundException ignore) {
        } catch (UnsupportedEncodingException ignore) {
        } catch (IOException e) {
            logger().warning("Failed to load kits.txt: "
                    + e.getMessage());
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException ignore) {
                }
            }
        }

        this.kits = kits;
    }

    public synchronized Kit getKit(String id) {
        return kits.get(id.toLowerCase());
    }

    public synchronized Map<String, Kit> getKits() {
        return kits;
    }

    public synchronized void flush() {
        for (Kit kit : kits.values()) {
            kit.flush();
        }
    }

}
