package com.sk89q.commandbook.bans;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import com.sk89q.commandbook.util.ChatUtil;
import com.sk89q.commandbook.util.ServerUtil;
import com.sk89q.commandbook.util.entity.player.UUIDUtil;
import org.apache.commons.lang.Validate;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.*;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.*;
import java.util.logging.Formatter;

import static com.sk89q.commandbook.CommandBook.logger;

public class CSVBanDatabase implements BanDatabase {

    protected final Logger auditLogger
            = Logger.getLogger("Minecraft.CommandBook.Bans");

    protected final File storageFile;

    /**
     * Used to lookup bans by UUID
     */
    protected Map<UUID, Ban> UUIDBan = new HashMap<UUID, Ban>();

    /**
     * Used to lookup bans by name
     */
    @Deprecated
    protected Map<String, Ban> nameBan = null;

    /**
     * Used to lookup bans by ip address
     */
    protected Map<String, Ban> ipBan = new HashMap<String, Ban>();

    /**
     * A set of all bans. No ban in the lookup maps is not in here.
     */
    protected final Set<Ban> bans = new HashSet<Ban>();

    private static final SimpleDateFormat dateFormat =
            new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

    public CSVBanDatabase(File banStorageDir) {
        storageFile = new File(banStorageDir, "bans.csv");

         // Set up an audit trail
        try {
            FileHandler handler = new FileHandler(
                    (new File(banStorageDir, "bans.%g.%u.log")).getAbsolutePath()
                    .replace("\\", "/"), true);

            handler.setFormatter(new Formatter() {

                @Override
                public String format(LogRecord record) {
                    return "[" + dateFormat.format(new Date())
                            + "] " + record.getMessage() + "\r\n";
                }
            });

            auditLogger.addHandler(handler);
        } catch (SecurityException e) {
            logger().warning("Failed to setup audit log for the "
                    + "CSV ban database: " + e.getMessage());
        } catch (IOException e) {
            logger().warning("Failed to setup audit log for the "
                    + "CSV ban database: " + e.getMessage());
        }
    }

    public synchronized boolean load() {
        FileInputStream input = null;
        boolean successful = true;
        boolean needsSaved = false;

        try {
            input = new FileInputStream(storageFile);
            InputStreamReader streamReader = new InputStreamReader(input, "utf-8");
            CSVReader reader = new CSVReader(new BufferedReader(streamReader));
            String[] line;

            while ((line = reader.readNext()) != null) {
                int lineLen = line.length;
                if (lineLen < 5) {
                    logger().warning("A ban entry with < 5 fields was found!");
                    continue;
                }
                try {
                    int i = 0;
                    UUID ID = null;
                    if (lineLen > 5) {
                        String rawLine = line[i++];
                        if (!rawLine.isEmpty() && !rawLine.equals("null")) {
                            ID = UUID.fromString(rawLine);
                        }
                    }
                    String name = line[i++].toLowerCase();
                    String address = line[i++];
                    String reason = line[i++];
                    long startDate = Long.parseLong(line[i++]);
                    long endDate = Long.parseLong(line[i++]);
                    if ("".equals(name) || "null".equals(name)) name = null;
                    if ("".equals(address) || "null".equals(address)) address = null;
                    Ban ban = new Ban(ID, name, address, reason, startDate, endDate);
                    if (ID != null) {
                        UUIDBan.put(ID, ban);
                    } else if (name != null) {
                        logger().finest("Converting " + name + "'s ban record to UUID...");
                        ID = UUIDUtil.convert(name);
                        if (ID != null) {
                            // Update the record
                            ban = new Ban(ID, name, address, reason, startDate, endDate);
                            UUIDBan.put(ID, ban);

                            // Log & request save
                            needsSaved = true;
                            logger().finest("Success!");
                        } else {
                            if (nameBan == null) {
                                nameBan = new HashMap<String, Ban>();
                            }
                            nameBan.put(name, ban);
                            logger().warning(ban.toString() + " could not be converted!");
                        }
                    }
                    if (address != null) ipBan.put(address, ban);
                    bans.add(ban);
                } catch (IllegalArgumentException i) {
                    if (i instanceof NumberFormatException) {
                        logger().warning("Non-long long field found in ban!");
                    } else {
                        logger().warning("Invalid UUID field found in ban!");
                    }
                }
            }
            logger().info(bans.size() + " banned name(s) loaded.");
        } catch (FileNotFoundException ignored) {
        } catch (IOException e) {
            nameBan = new HashMap<String, Ban>();
            ipBan = new HashMap<String, Ban>();
            logger().warning("Failed to load " + storageFile.getAbsolutePath()
                    + ": " + e.getMessage());
            successful = false;
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException ignored) {
                }
            }
        }
        if (needsSaved) save();
        return successful;
    }

    public synchronized boolean save() {
        FileOutputStream output = null;
        boolean successful = true;

        try {
            output = new FileOutputStream(storageFile);
            CSVWriter writer = new CSVWriter(new BufferedWriter(new OutputStreamWriter(output, "utf-8")));
            String[] line;

            for (Ban ban : bans) {
                line = new String[] {
                        String.valueOf(ban.getID()),
                        ban.getLastKnownAlias(),
                        ban.getAddress(),
                        ban.getReason(),
                        String.valueOf(ban.getStart()),
                        String.valueOf(ban.getEnd())
                };
                writer.writeNext(line);
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            logger().warning("Failed to save " + storageFile.getAbsolutePath()
                    + ": " + e.getMessage());
            successful = false;
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException ignored) {
                }
            }
        }
        return successful;
    }

    public boolean unload() {
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

    @Override
    public boolean isBanned(UUID ID) {
        Ban ban = UUIDBan.get(ID);
        if (ban != null) {
            if (ban.getEnd() != 0L && ban.getEnd() - System.currentTimeMillis() <= 0) {
                unban(ID, null, null, "Tempban expired");
                save();
                return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean isBanned(InetAddress address) {
        Ban ban = ipBan.get(address.getHostAddress());
        if (ban != null) {
            if (ban.getEnd() != 0L && ban.getEnd() - System.currentTimeMillis() <= 0) {
                unban(null, address.getHostAddress(), null, "Tempban expired");
                save();
                return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public String getBannedMessage(UUID ID) {
        Ban ban = UUIDBan.get(ID);
        if (ban == null || ban.getReason() == null) return "You are banned.";
        return ban.getReason();
    }

    @Override
    public String getBannedMessage(String address) {
        Ban ban = ipBan.get(address);
        if (ban == null || ban.getReason() == null) return "You are banned by IP.";
        return ban.getReason();
    }

    public void ban(Player player, CommandSender source, String reason, long end) {
        ban(player.getUniqueId(), player.getName(), player.getAddress().getAddress().getHostAddress(), source, reason, end);
    }

    @Override
    public void ban(UUID ID, String name, String address, CommandSender source, String reason, long end) {
        Validate.isTrue(ID != null || address != null, "You must specify either an ID, or address");
        Ban ban = new Ban(ID, name, address, reason, System.currentTimeMillis(), end);
        if (ID != null) {
            UUIDBan.put(ID, ban);
        }
        if (address != null) {
            ipBan.put(address, ban);
        }
        bans.add(ban);
        auditLogger.info(String.format("BAN: %s (%s) added %s: %s",
            source == null ? "Plugin" : ChatUtil.toUniqueName(source),
            source == null ? "local" : ServerUtil.toInetAddressString(source),
            ban.toString(),
            reason));
    }

    @Override
    public boolean unbanName(String name, CommandSender source, String reason) {
        if (nameBan == null || name == null || name.isEmpty()) return false;
        Ban ban = nameBan.remove(name.toLowerCase());
        if (ban != null) {
            bans.remove(ban);
            auditLogger.info(String.format("UNBAN: %s (%s) removed %s: %s",
                    source == null ? "Plugin" : ChatUtil.toUniqueName(source),
                    source == null ? "local" : ServerUtil.toInetAddressString(source),
                    ban.toString(),
                    reason));
            return true;
        }
        return false;
    }

    @Override
    public boolean unban(Player player, CommandSender source, String reason) {
        return unban(player.getUniqueId(), null, source, reason);
    }

    @Override
    public boolean unban(UUID ID, String address, CommandSender source, String reason) {
        Ban ban = null;
        if (ID != null) {
            ban = UUIDBan.remove(ID);
        }
        if (ban == null && address != null) {
            ban = ipBan.remove(address);
        }
        if (ban != null) {
            bans.remove(ban);
            auditLogger.info(String.format("UNBAN: %s (%s) removed %s: %s",
                    source == null ? "Plugin" : ChatUtil.toUniqueName(source),
                    source == null ? "local" : ServerUtil.toInetAddressString(source),
                    ban.toString(),
                    reason));
            return true;
        }
        return false;
    }

    public void logKick(Player player, CommandSender source, String reason) {
        auditLogger.info(String.format("KICKED: %s (%s) kicked player '%s': %s",
                ChatUtil.toUniqueName(source),
                ServerUtil.toInetAddressString(source),
                player.getName(),
                reason));
    }

    public void importFrom(BanDatabase bans) {
        for (Ban ban : bans) {
            boolean set = false;
            if (ban.getID() != null) {
                set = true;
                UUIDBan.put(ban.getID(), ban);
            }
            if (ban.getAddress() != null && !ban.getAddress().isEmpty()) {
                set = true;
                ipBan.put(ban.getAddress(), ban);
            }
            if (set) {
                this.bans.add(ban);
            } else {
                logger().warning(ban.toString() + " could not be imported!");
            }
        }
    }

    @Override
    public Ban getBanned(UUID ID) {
        return UUIDBan.get(ID);
    }

    @Override
    public Ban getBanned(String address) {
        return ipBan.get(address);
    }

    public Iterator<Ban> iterator() {
        return new Iterator<Ban>() {
            private final Iterator<Ban> setIter = bans.iterator();
            private Ban next;
            public boolean hasNext() {
                return setIter.hasNext();
            }

            public Ban next() {
                return next = setIter.next();
            }

            public void remove() {
                unban(next.getID(), next.getAddress(), null, "Removed by iterator");
            }
        };
    }
}
