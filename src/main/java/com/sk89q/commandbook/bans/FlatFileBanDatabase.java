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

package com.sk89q.commandbook.bans;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.sk89q.commandbook.CommandBookPlugin;

/**
 * Flat file ban database.
 * 
 * @author sk89q
 */
public class FlatFileBanDatabase implements BanDatabase {
    
    protected static final Logger logger = Logger.getLogger("Minecraft.CommandBook");
    
    protected final Logger auditLogger
            = Logger.getLogger("Minecraft.CommandBook.Bans");
    
    protected CommandBookPlugin plugin;
    protected File dataDirectory;
    protected File namesFile;
    protected File ipFile;

    protected Set<String> bannedNames;
    protected Set<String> bannedIP;

    public FlatFileBanDatabase(File dataDirectory, CommandBookPlugin plugin) {
        this.dataDirectory = dataDirectory;
        this.plugin = plugin;

        namesFile = new File(dataDirectory, "banned_names.txt");
        ipFile = new File(dataDirectory, "banned_ip.txt");
        
        // Set up an audit trail
        try {
            FileHandler handler = new FileHandler(
                    (new File(dataDirectory, "bans.%g.%u.log")).getAbsolutePath()
                    .replace("\\", "/"), true);
            
            handler.setFormatter(new Formatter() {
                private SimpleDateFormat dateFormat =
                        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                
                @Override
                public String format(LogRecord record) {
                    return "[" + dateFormat.format(new Date())
                            + "] " + record.getMessage() + "\r\n";
                }
            });
            
            auditLogger.addHandler(handler);
        } catch (SecurityException e) {
            logger.warning("CommandBook: Failed to setup audit log for the "
                    + "flat file ban database: " + e.getMessage());
        } catch (IOException e) {
            logger.warning("CommandBook: Failed to setup audit log for the "
                    + "flat file ban database: " + e.getMessage());
        }
    }

    public synchronized boolean load() {
        boolean successful = true;
        
        try {
            bannedNames = readLowercaseList(namesFile);
            logger.info("CommandBook: " + bannedNames.size() + " banned name(s) loaded.");
        } catch (IOException e) {
            bannedNames = new HashSet<String>();
            logger.warning("CommandBook: Failed to load " + namesFile.getAbsolutePath()
                    + ": " + e.getMessage());
            successful = false;
        }
/*
        try {
            bannedIP = readList(ipFile);
            logger.info("CommandBook: " + bannedIP.size() + " banned IP(s) loaded.");
        } catch (IOException e) {
            bannedIP = new HashSet<String>();
            logger.warning("CommandBook: Failed to load " + ipFile.getAbsolutePath()
                    + ": " + e.getMessage());
            successful = false;
        }
*/
        return successful;
    }

    public synchronized boolean unload() {
        for (Handler handler : auditLogger.getHandlers()) {
            if (handler instanceof FileHandler) {
                handler.flush();
                handler.close();
                auditLogger.removeHandler(handler);
                return true;
            }
        }
        return false;
    }
    
    /**
     * Read a list from file. Each line is trimmed and made lower case.
     * 
     * @param file
     * @return
     * @throws IOException
     */
    protected synchronized Set<String> readLowercaseList(File file) throws IOException {
        FileInputStream input = null;
        Set<String> list = new HashSet<String>();
        
        try {
            input = new FileInputStream(file);
            InputStreamReader streamReader = new InputStreamReader(input, "utf-8");
            BufferedReader reader = new BufferedReader(streamReader);
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                
                if (line.length() > 0) {
                    list.add(line.toLowerCase().trim());
                }
            }
        } catch (FileNotFoundException e) {
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                }
            }
        }
        
        return list;
    }

    public synchronized boolean save() {
        boolean successful = true;
        
        try {
            writeList(namesFile, bannedNames);
            //logger.info("CommandBook: " + bannedNames.size() + " banned names written.");
        } catch (IOException e) {
            logger.warning("CommandBook: Failed to write " + namesFile.getAbsolutePath()
                    + ": " + e.getMessage());
            successful = false;
        }
/*
        try {
            writeList(ipFile, bannedIP);
            //logger.info("CommandBook: " + bannedIP.size() + " banned IPs written.");
        } catch (IOException e) {
            logger.warning("CommandBook: Failed to write " + ipFile.getAbsolutePath()
                    + ": " + e.getMessage());
            successful = false;
        }
*/
        return successful;
    }
    
    protected synchronized void writeList(File file, Set<String> list)
            throws IOException {
        FileOutputStream output = null;
        
        try {
            output = new FileOutputStream(file);
            OutputStreamWriter streamWriter = new OutputStreamWriter(output, "utf-8");
            BufferedWriter writer = new BufferedWriter(streamWriter);
            
            for (String line : list) {
                writer.write(line + "\r\n");
            }
            
            writer.close();
        } catch (FileNotFoundException e) {
        } catch (UnsupportedEncodingException e) {
            logger.log(Level.WARNING, "Failed to write list", e);
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public synchronized boolean isBannedName(String name) {
        return bannedNames.contains(name.toLowerCase().trim());
    }

    public synchronized boolean isBannedAddress(InetAddress address) {
        return bannedIP.contains(address.getHostAddress());
    }

    public synchronized void banName(String name, CommandSender source, String reason) {
        auditLogger.info(String.format("BAN: %s (%s) banned name '%s': %s",
                plugin.toUniqueName(source), 
                plugin.toInetAddressString(source),
                name,
                reason));
        
        bannedNames.add(name.toLowerCase());
    }

    public synchronized void banAddress(String address, CommandSender source, String reason) {
        auditLogger.info(String.format("BAN: %s (%s) banned address '%s': %s",
                plugin.toUniqueName(source), 
                plugin.toInetAddressString(source),
                address,
                reason));
        
        bannedIP.add(address);
    }

    public boolean unbanName(String name, CommandSender source, String reason) {
        boolean removed = bannedNames.remove(name.toLowerCase());
        
        if (removed) {
            auditLogger.info(String.format("UNBAN: %s (%s) unbanned name '%s': %s",
                    plugin.toUniqueName(source), 
                    plugin.toInetAddressString(source),
                    name,
                    reason));
        }
        
        return removed;
    }

    public boolean unbanAddress(String address, CommandSender source, String reason) {
        boolean removed = bannedIP.remove(address);
        
        if (removed) {
            auditLogger.info(String.format("UNBAN: %s (%s) unbanned ADDRESS '%s'",
                    plugin.toUniqueName(source), 
                    plugin.toInetAddressString(source),
                    address,
                    reason));
        }
        
        return removed;
    }

    public void logKick(Player player, CommandSender source, String reason) {
        auditLogger.info(String.format("KICKED: %s (%s) kicked player '%s': %s",
                plugin.toUniqueName(source), 
                plugin.toInetAddressString(source),
                player.getName(),
                reason));
    }

}
