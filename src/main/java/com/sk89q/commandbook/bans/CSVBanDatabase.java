package com.sk89q.commandbook.bans;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.util.ChatUtil;
import com.sk89q.commandbook.util.ServerUtil;
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
     * Used to lookup bans by name
     */
    protected Map<String, Ban> nameBan = new HashMap<String, Ban>();

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

        try {
            input = new FileInputStream(storageFile);
            InputStreamReader streamReader = new InputStreamReader(input, "utf-8");
            CSVReader reader = new CSVReader(new BufferedReader(streamReader));
            String[] line;

            while ((line = reader.readNext()) != null) {
                if (line.length < 5) {
                    logger().warning("A ban entry with < 5 fields was found!");
                    continue;
                }
                try {
                    String name = line[0].toLowerCase();
                    String address = line[1];
                    String reason = line[2];
                    long startDate = Long.parseLong(line[3]);
                    long endDate = Long.parseLong(line[4]);
                    if ("".equals(name) || "null".equals(name)) name = null;
                    if ("".equals(address) || "null".equals(address)) address = null;
                    Ban ban = new Ban(name, address, reason, startDate, endDate);
                    if (name != null) nameBan.put(name, ban);
                    if (address != null) ipBan.put(address, ban);
                    bans.add(ban);
                } catch (NumberFormatException e) {
                    logger().warning("Non-long long field found in ban!");
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
                        ban.getName(),
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

    public boolean isBannedName(String name) {
        name = name.toLowerCase();
        Ban ban = nameBan.get(name);
        if (ban != null) {
            if (ban.getEnd() != 0L && ban.getEnd() - System.currentTimeMillis() <= 0) {
                unban(name, null, null, "Tempban expired");
                save();
                return false;
            }
            return true;
        }
        return false;
    }

    public boolean isBannedAddress(InetAddress address) {
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

    public String getBannedNameMesage(String name) {
        return getBannedNameMessage(name);
    }

    public void ban(Player player, CommandSender source, String reason, long end) {
        ban(player.getName(), player.getAddress().getAddress().getHostAddress(), source, reason, end);
    }

    public void ban(String name, String address, CommandSender source, String reason, long end) {
        Ban ban = new Ban(name, address, reason, System.currentTimeMillis(), end);
        String banned = null;
        String bannedName = null;
        if (name != null) {
            name = name.toLowerCase();
            nameBan.put(name, ban);
            banned = "name";
            bannedName = name;
        }
        if (address != null) {
            ipBan.put(address, ban);
            banned = banned == null ? "address" : banned + " and address";
            bannedName = bannedName == null ? address : banned + "/" + address;
        }
        if (name != null || address != null) {
            bans.add(ban);
            auditLogger.info(String.format("BAN: %s (%s) banned %s '%s': %s",
                    source == null ? "Plugin" : ChatUtil.toUniqueName(source),
                    source == null ? "local" : ServerUtil.toInetAddressString(source),
                    banned,
                    bannedName,
                    reason));
        }
    }

    public void banName(String name, CommandSender source, String reason) {
        ban(name, null, source, reason, 0L);
    }

    public void banAddress(String address, CommandSender source, String reason) {
        ban(null, address, source, reason, 0L);
    }

    public boolean unban(String name, String address, CommandSender source, String reason) {
        Ban ban = null;
        String banned = null;
        String bannedName = null;
        if (name != null) {
            name = name.toLowerCase();
            ban = nameBan.remove(name);
            if (ban != null) {
                banned = "name";
                bannedName = name;
                if (ipBan.remove(ban.getAddress()) != null) {
                    banned += " and address";
                    bannedName += "/" + address;
                }
            }
        }
        if (ban == null && address != null) {
            ban = ipBan.remove(address);
            if (ban != null) {
                banned = "address";
                bannedName = address;
                if (nameBan.remove(ban.getName()) != null) {
                    banned = "name and " + banned;
                    bannedName = name + "/" + bannedName;
                }
            }
        }
        if (ban != null) {
            bans.remove(ban);
            auditLogger.info(String.format("UNBAN: %s (%s) unbanned %s '%s': %s",
                    source == null ? "Plugin" : ChatUtil.toUniqueName(source),
                    source == null ? "local" : ServerUtil.toInetAddressString(source),
                    banned,
                    bannedName,
                    reason));
            return true;
        }
        return false;
    }

    public boolean unbanName(String name, CommandSender source, String reason) {
        return unban(name, null, source, reason);
    }

    public boolean unbanAddress(String address, CommandSender source, String reason) {
        return unban(null, address, source, reason);
    }

    public String getBannedNameMessage(String name) {
        Ban ban = nameBan.get(name.toLowerCase());
        if (ban == null || ban.getReason() == null) return "You are banned.";
        return ban.getReason();
    }

    public String getBannedAddressMessage(String address) {
        Ban ban = ipBan.get(address);
        if (ban == null || ban.getReason() == null) return "You are banned by IP.";
        return ban.getReason();
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
            if (ban.getName() != null && ban.getName().length() > 0) {
                nameBan.put(ban.getName(), ban);
            }
            if (ban.getAddress() != null && ban.getAddress().length() > 0) {
                ipBan.put(ban.getAddress(), ban);
            }
            this.bans.add(ban);
        }
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
                unban(next.getName(), next.getAddress(), null, "Removed by iterator");
            }
        };
    }

    public Ban getBannedName(String name) {
        return nameBan.get(name);
    }

    public Ban getBannedAddress(String address) {
        return ipBan.get(address);
    }
}
