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

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.util.ServerUtil;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.*;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.*;

import static com.sk89q.commandbook.CommandBook.logger;
import static com.sk89q.commandbook.util.ChatUtil.toUniqueName;

/**
 * Flat file ban database.
 *
 * @author sk89q
 */
public class FlatFileBanDatabase implements BanDatabase {

    protected final Logger auditLogger
            = Logger.getLogger("Minecraft.CommandBook.Bans");

    protected final BansComponent component;
    protected final File dataDirectory;
    protected final File namesFile;

    protected Map<String, Ban> bannedNames;

    public static boolean toImport(File dataDirectory) {
        return new File(dataDirectory, "banned_names.txt").exists();
    }

    public FlatFileBanDatabase(File dataDirectory, BansComponent component) {
        this.dataDirectory = dataDirectory;
        this.component = component;

        namesFile = new File(dataDirectory, "banned_names.txt");

        // Set up an audit trail
        try {
            FileHandler handler = new FileHandler(
                    (new File(dataDirectory, "bans.%g.%u.log")).getAbsolutePath()
                    .replace("\\", "/"), true);

            handler.setFormatter(new Formatter() {
                private final SimpleDateFormat dateFormat =
                        new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

                @Override
                public String format(LogRecord record) {
                    return "[" + dateFormat.format(new Date())
                            + "] " + record.getMessage() + "\r\n";
                }
            });

            auditLogger.addHandler(handler);
        } catch (SecurityException e) {
            logger().warning("Failed to setup audit log for the "
                    + "flat file ban database: " + e.getMessage());
        } catch (IOException e) {
            logger().warning("Failed to setup audit log for the "
                    + "flat file ban database: " + e.getMessage());
        }
    }

    public synchronized boolean load() {
        boolean successful = true;

        try {
            bannedNames = readLowercaseList(namesFile);
            logger().info(bannedNames.size() + " banned name(s) loaded.");
        } catch (IOException e) {
            bannedNames = new HashMap<String, Ban>();
            logger().warning("Failed to load " + namesFile.getAbsolutePath()
                    + ": " + e.getMessage());
            successful = false;
        }
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
    protected synchronized Map<String, Ban> readLowercaseList(File file) throws IOException {
        FileInputStream input = null;
        Map<String, Ban> list = new HashMap<String, Ban>();

        try {
            input = new FileInputStream(file);
            InputStreamReader streamReader = new InputStreamReader(input, "utf-8");
            BufferedReader reader = new BufferedReader(streamReader);
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.length() > 0) {
                    list.put(line.toLowerCase().trim(),
                            new Ban(line.toLowerCase().trim(), null, null, System.currentTimeMillis(), 0L));
                }
            }
        } catch (FileNotFoundException ignored) {
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException ignored) {
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
            logger().warning("Failed to write " + namesFile.getAbsolutePath()
                    + ": " + e.getMessage());
            successful = false;
        }
        return successful;
    }

    protected synchronized void writeList(File file, Map<String, Ban> list)
            throws IOException {
        FileOutputStream output = null;

        try {
            output = new FileOutputStream(file);
            OutputStreamWriter streamWriter = new OutputStreamWriter(output, "utf-8");
            BufferedWriter writer = new BufferedWriter(streamWriter);

            for (String line : list.keySet()) {
                writer.write(line + "\r\n");
            }

            writer.close();
        } catch (FileNotFoundException ignore) {
        } catch (UnsupportedEncodingException e) {
            logger().log(Level.WARNING, "Failed to write list", e);
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException ignore) {
                }
            }
        }
    }

    public synchronized boolean isBannedName(String name) {
        return bannedNames.containsKey(name.toLowerCase().trim());
    }

    public synchronized boolean isBannedAddress(InetAddress address) {
        return false;
    }

    public String getBannedNameMesage(String name) {
        return getBannedNameMessage(name);
    }

    public String getBannedNameMessage(String name) {
        return "You have been banned";
    }

    public String getBannedAddressMessage(String address) {
        return "You have been banned";
    }

    public synchronized void banName(String name, CommandSender source, String reason) {
        auditLogger.info(String.format("BAN: %s (%s) banned name '%s': %s",
                toUniqueName(source),
                ServerUtil.toInetAddressString(source),
                name,
                reason));

        bannedNames.put(name, new Ban(name.toLowerCase(), null, null, System.currentTimeMillis(), 0L));
    }

    public synchronized void banAddress(String address, CommandSender source, String reason) {
        throw new UnsupportedOperationException("Not supported.");
    }

    public void ban(Player player, CommandSender source, String reason, long end) {
        banName(player.getName(), source, reason);
    }

    public void ban(String name, String address, CommandSender source, String reason, long end) {
        banName(name, source, reason);
    }

    public boolean unbanName(String name, CommandSender source, String reason) {
        boolean removed = bannedNames.remove(name.toLowerCase()) != null;

        if (removed) {
            auditLogger.info(String.format("UNBAN: %s (%s) unbanned name '%s': %s",
                    toUniqueName(source),
                    ServerUtil.toInetAddressString(source),
                    name,
                    reason));
        }

        return removed;
    }

    public boolean unbanAddress(String address, CommandSender source, String reason) {
        return false;
    }

    public boolean unban(String name, String address, CommandSender source, String reason) {
        return unbanName(name, source, reason);
    }

    public void logKick(Player player, CommandSender source, String reason) {
        auditLogger.info(String.format("KICKED: %s (%s) kicked player '%s': %s",
                toUniqueName(source),
                ServerUtil.toInetAddressString(source),
                player.getName(),
                reason));
    }

    public void importFrom(BanDatabase bans) {
        throw new UnsupportedOperationException("Importing to legacy ban storage provider not supported.");
    }

    @Override
    public Ban getBannedName(String name) {
        return bannedNames.get(name);
    }

    @Override
    public Ban getBannedAddress(String address) {
        return null;
    }

    public Iterator<Ban> iterator() {
        return bannedNames.values().iterator();
    }
}
