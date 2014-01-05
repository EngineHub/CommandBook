package com.sk89q.commandbook;

import com.sk89q.commandbook.session.PersistentSession;
import com.sk89q.commandbook.session.SessionComponent;
import com.sk89q.commandbook.util.LocationUtil;
import com.sk89q.commandbook.util.PlayerUtil;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@ComponentInformation(friendlyName = "AFK Checker", desc = "AFK Checking and management.")
@Depend(components = {GodComponent.class, SessionComponent.class})
public class AFKComponent extends BukkitComponent implements Runnable, Listener {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = CommandBook.logger();
    private final Server server = CommandBook.server();

    @InjectComponent private GodComponent godComponent;
    @InjectComponent private SessionComponent sessions;

    private LocalConfiguration config;

    @Override
    public void enable() {

        config = configure(new LocalConfiguration());

        CommandBook.registerEvents(this);
        if (config.useMovementEvent) {
            CommandBook.registerEvents(new MovementListener());
        }

        registerCommands(Commands.class);
        server.getScheduler().runTaskTimer(inst, this, 20, 20);
    }

    @Override
    public void reload() {

        super.reload();
        configure(config);
    }

    private static class LocalConfiguration extends ConfigurationBase {

        @Setting("use-movement-event")
        public boolean useMovementEvent = true;
        @Setting("movement-threshold")
        public double movementThreshold = .04;
        @Setting("sneak-movement-threshold")
        public double sneakMovementThreshold = .004;
        @Setting("afk-minutes")
        public int afkMinutes = 3;
        @Setting("afk-kick-minutes")
        public int afkKickMinutes = 60;
        @Setting("afk-general-sleep-ignored")
        public boolean afkGeneralSleepIgnored = true;
        @Setting("afk-command-sleep-ignored")
        public boolean afkCommandSleepIgnored = false;
        @Setting("afk-general-protection")
        public boolean afkGeneralProtection = true;
        @Setting("afk-command-protection")
        public boolean afkCommandProtection = false;
    }

    /**
     * Determines if a player is marked as AFK.
     *
     * @param player the player to check
     * @return true if the player should be kicked
     */
    public boolean isAfk(Player player) {

        return sessions.getSession(AFKSession.class, player).isAFK();
    }

    /**
     * Determines if a time is sufficient to be AFK.
     *
     * @param time the last update time
     * @return true if the player should be kicked
     */
    public boolean isAfk(long time) {

        return time != 0 && System.currentTimeMillis() - time >= TimeUnit.MINUTES.toMillis(config.afkMinutes);
    }

    /**
     * Determines if a player should be kicked for being AFK.
     *
     * @param player the player to check
     * @return true if the player should be kicked
     */
    public boolean shouldKick(Player player) {

        return shouldKick(sessions.getSession(AFKSession.class, player).getLastUpdate());
    }

    /**
     * Determines if a time is sufficient to be kicked for being AFK.
     *
     * @param time the last update time
     * @return true if the player should be kicked
     */
    public boolean shouldKick(long time) {

        if (config.afkKickMinutes < 1) return false;

        double maxP = server.getMaxPlayers();
        double curP = server.getOnlinePlayers().length;

        double fraction = ((maxP - curP) + maxP * .2) / maxP;
        int duration = (int) Math.max(config.afkMinutes + 2, Math.min(config.afkKickMinutes, config.afkKickMinutes * fraction));
        return time != 0 && System.currentTimeMillis() - time >= TimeUnit.MINUTES.toMillis(duration);
    }

    /**
     * Determines if a player can be set to ignored for the sleep check.
     *
     * @param player the player to check
     * @return true if the player can be set to ignored for the sleep check
     */
    public boolean canIgnoreSleep(Player player) {

        AFKSession session = sessions.getSession(AFKSession.class, player);
        return (session.isRequested() && canIgnoreSleep(true)) || (isAfk(session.getLastUpdate()) && canIgnoreSleep(false));
    }

    /**
     * Determines if a player can be set to ignored for the sleep check.
     *
     * @param requested whether or not the player requested to be afk
     * @return true if the player can be set to ignored for the sleep check
     */
    public boolean canIgnoreSleep(boolean requested) {

        return !requested && config.afkGeneralSleepIgnored || requested && config.afkCommandSleepIgnored;
    }

    /**
     * Determines if a player can be protected.
     *
     * @param player the player to check
     * @return true if the player can be protected
     */
    public boolean canProtect(Player player) {

        AFKSession session = sessions.getSession(AFKSession.class, player);
        return (session.isRequested() && canProtect(true)) || (isAfk(session.getLastUpdate()) && canProtect(false));
    }

    /**
     * Determines if a player can be protected.
     *
     * @param requested whether or not the player requested to be afk
     * @return true if the player can be protected
     */
    public boolean canProtect(boolean requested) {

        return !requested && config.afkGeneralProtection || requested && config.afkCommandProtection;
    }

    /**
     * Updates a player's last active time.
     *
     * @param player player to update
     */
    public void update(Player player) {

        AFKSession session = sessions.getSession(AFKSession.class, player);

        // Update the time and remove the idle message
        session.setLastUpdate(System.currentTimeMillis());
        session.setIdleStatus(null);

        // Restore god mode setting
        if (godComponent.hasGodMode(player) && session.isProtected()) {
            godComponent.disableGodMode(player);
            session.setProtected(false);
        }
    }

    @Override
    public void run() {

        for (final AFKSession session : sessions.getSessions(AFKSession.class).values()) {

            if (session == null) continue;
            final Player target = session.getPlayer();
            if (target == null || !session.getPlayer().isValid()) continue;

            boolean passedTime = isAfk(session.getLastUpdate());
            if (session.isRequested() || passedTime) {
                if (shouldKick(session.getLastUpdate())) {
                    // Restore sleep ignored setting
                    if (session.isSleepIgnored()) {
                        target.setSleepingIgnored(false);
                        session.setSleepIgnored(false);
                    }

                    server.getScheduler().runTaskLater(inst, new Runnable() {

                        @Override
                        public void run() {

                            target.kickPlayer("Inactivity - " + (System.currentTimeMillis() - session.getLastUpdate()) / 60000 + " Minutes");
                        }
                    }, 1);
                } else if (!session.isAFK()) {

                    // Grey out list name
                    String name = target.getName();
                    target.setPlayerListName(ChatColor.GRAY + name.substring(0, Math.min(14, name.length())));

                    // Mark the player as AFK
                    session.setAFK(true);
                    target.sendMessage(ChatColor.YELLOW + "You are now marked as AFK.");
                }

                // Check and set sleep ignored
                if (canIgnoreSleep(session.isRequested()) || (passedTime && canIgnoreSleep(false))) {
                    if (!target.isSleepingIgnored()) {
                        target.setSleepingIgnored(true);
                        session.setSleepIgnored(true);
                    }
                }

                // Check and set god mode
                if (canProtect(session.isRequested()) || (passedTime && canProtect(false))) {
                    if (!godComponent.hasGodMode(target)) {
                        godComponent.enableGodMode(target);
                        session.setProtected(true);
                    }
                }
            } else if (session.isAFK()) {

                // Fix list name
                target.setPlayerListName(target.getName());

                // Restore sleep ignored setting
                if (session.isSleepIgnored()) {
                    target.setSleepingIgnored(false);
                    session.setSleepIgnored(false);
                }

                // Mark the player as not AFK
                session.setAFK(false);
                target.sendMessage(ChatColor.YELLOW + "You are no longer marked as AFK.");
            }
        }
    }

    public class Commands {
        @Command(aliases = {"afk", "away"},
                usage = "", desc = "Set yourself as away",
                flags = "", min = 0, max = -1)
        @CommandPermissions({"commandbook.away"})
        public void afk(CommandContext args, CommandSender sender) throws CommandException {

            Player player = PlayerUtil.checkPlayer(sender);

            String status = "";
            if (args.argsLength() > 0) {
                status = args.getJoinedStrings(0);

                // Make sure someone isn't playing around with this
                if (status.equals("null")) {
                    status = "";
                }
            }
            sessions.getSession(AFKSession.class, player).setIdleStatus(status);

            player.sendMessage(ChatColor.YELLOW
                    + (status.isEmpty() ? "Set as away" : "Set away status to \"" + status + "\"")
                    + ".");
        }
    }

    @EventHandler
    public void onEntityTargetPlayer(EntityTargetEvent event) {

        if (event.getTarget() instanceof Player) {

            if (canProtect((Player) event.getTarget())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {

        update(event.getPlayer());
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {

        update(event.getPlayer());
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {

        update(event.getPlayer());
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {

        update(event.getPlayer());
    }

    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {

        update(event.getPlayer());
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {

        if (event.getPlayer() instanceof Player) update((Player) event.getPlayer());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {

        if (event.getWhoClicked() instanceof Player) update((Player) event.getWhoClicked());
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {

        if (event.getPlayer() instanceof Player) update((Player) event.getPlayer());
    }

    @EventHandler
    public void onEntityDamageEntityEvent(EntityDamageByEntityEvent event) {

        if (event.getDamager() instanceof Player) update((Player) event.getDamager());
    }

    public class MovementListener implements Listener {
        @EventHandler
        public void onMoveChange(PlayerMoveEvent event) {

            Player player = event.getPlayer();
            double distanceSQ = LocationUtil.distanceSquared2D(event.getFrom(), event.getTo());

            if (distanceSQ > config.movementThreshold
                    || (player.isSneaking() && distanceSQ > config.sneakMovementThreshold)) {
                update(player);
            }
        }
    }

    // AFK Session
    public static class AFKSession extends PersistentSession {

        @Setting("idle-status") private String idleStatus = "null";
        private long lastUpdate = 0;
        private boolean protect = false;
        private boolean sleepIgnored = false;
        private boolean awayFromKeyboard = false;

        protected AFKSession() {
            super(THIRTY_MINUTES);
        }

        public Player getPlayer() {
            CommandSender sender = super.getOwner();
            return sender instanceof Player ? (Player) sender : null;
        }

        public String getIdleStatus() {
            return idleStatus.equals("null") ? "" : idleStatus;
        }

        public void setIdleStatus(String status) {
            this.idleStatus = status == null ? "null" : status;
        }

        public boolean isRequested() {
            return !idleStatus.equals("null");
        }

        public long getLastUpdate() {
            return lastUpdate;
        }

        public void setLastUpdate(long lastUpdate) {
            this.lastUpdate = lastUpdate;
        }

        public boolean isProtected() {
            return protect;
        }

        public void setProtected(boolean protect) {
            this.protect = protect;
        }

        public boolean isSleepIgnored() {
            return sleepIgnored;
        }

        public void setSleepIgnored(boolean sleepIgnored) {
            this.sleepIgnored = sleepIgnored;
        }

        public boolean isAFK() {
            return awayFromKeyboard;
        }

        public void setAFK(boolean awayFromKeyboard) {
            this.awayFromKeyboard = awayFromKeyboard;
        }
    }
}

