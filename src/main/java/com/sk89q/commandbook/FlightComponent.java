package com.sk89q.commandbook;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import com.sk89q.commandbook.session.PersistentSession;
import com.sk89q.commandbook.session.SessionComponent;
import com.sk89q.commandbook.util.PlayerUtil;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.minecraft.util.commands.NestedCommand;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;

@ComponentInformation(friendlyName = "Flight", desc = "Allows players to fly and control flight speed")
public class FlightComponent extends BukkitComponent implements Listener {
    private static final float DEFAULT_FLIGHT_SPEED = 0.1f, DEFAULT_WALK_SPEED = 0.2f;
    @InjectComponent private SessionComponent sessions;
    private LocalConfiguration config;

    @Override
    public void enable() {
        CommandBook.registerEvents(this);
        registerCommands(Commands.class);
        config = configure(new LocalConfiguration());
    }

    private static class LocalConfiguration extends ConfigurationBase {
        @Setting("auto-enable") public boolean autoEnable = false;
    }

    public static class FlightSession extends PersistentSession {
        private FlightSession() {
            super(-1);
        }

        @Setting("can-fly") private boolean canFly;
    }

    public class Commands {
        @Command(aliases = "fly", usage = "[player]", desc = "Toggle flight for a player", min = 0, max = 1)
        @CommandPermissions("commandbook.flight.toggle")
        public void fly(CommandContext args, CommandSender sender) throws CommandException {
            Player player;
            if (args.argsLength() == 1) {
                player = PlayerUtil.matchSinglePlayer(sender, args.getString(0));
            } else {
                player = PlayerUtil.checkPlayer(sender);
            }
            FlightSession session = sessions.getSession(FlightSession.class, player);

            session.canFly = !session.canFly;
            player.setAllowFlight(session.canFly);


            player.sendMessage(ChatColor.YELLOW + "You can " + (session.canFly ? "now" : "no longer") + " fly!");

        }

        @Command(aliases = "speed", desc = "Set the speed of player movement")
        @NestedCommand(SpeedCommands.class)
        public void speed() {
        }

        @Command(aliases = "reverse", desc = "Go in reverse!", usage = "[player]", min = 0, max = 1)
        @CommandPermissions("commandbook.flight.reverse")
        public void reverse(CommandContext args, CommandSender sender) throws CommandException {
            Player player;
            if (args.argsLength() > 0) {
                player = PlayerUtil.matchSinglePlayer(sender, args.getString(0));
            } else {
                player = PlayerUtil.checkPlayer(sender);
            }

            player.setFlySpeed(-player.getFlySpeed());
            // player.setWalkSpeed(-player.getWalkSpeed()); // Gives weird video artifacts TODO: Bug DB about disabling viewport distortions when we change walk speed
            player.sendMessage(ChatColor.YELLOW + "And now in reverse!");
            if (sender != player) {
                sender.sendMessage(ChatColor.YELLOW + player.getName() + " has been put in reverse!");
            }
        }
    }

    public class SpeedCommands {
        @Command(aliases = {"flight", "fly", "f"}, usage = "[player] [speed-multiplier]", desc = "Set the flying speed multiplier")
        @CommandPermissions({"commandbook.flight.speed", "commandbook.speed.flight"})
        public void flightSpeed(CommandContext args, CommandSender sender) throws CommandException {
            Player player;
            float flightMultiplier = 1f;
            if (args.argsLength() == 2) {
                player = PlayerUtil.matchSinglePlayer(sender, args.getString(0));
                flightMultiplier = (float) args.getDouble(1);
            } else if (args.argsLength() == 1) {
                try {
                    flightMultiplier = (float) args.getDouble(0);
                    player = PlayerUtil.checkPlayer(sender);
                } catch (NumberFormatException e) {
                    player = PlayerUtil.matchSinglePlayer(sender, args.getString(0));
                }
            } else {
                player = PlayerUtil.checkPlayer(sender);
            }
            final float flightSpeed = DEFAULT_FLIGHT_SPEED * flightMultiplier; // Apply multiplier

            try {
                player.setFlySpeed(flightSpeed);
            } catch (IllegalArgumentException e) { // We gave an invalid value, tell the user nicely
                if (flightSpeed < DEFAULT_WALK_SPEED) {
                    throw new CommandException("Speed multiplier too low: " + flightMultiplier);
                } else if (flightSpeed > DEFAULT_WALK_SPEED) {
                    throw new CommandException("Speed multiplier too high: " + flightMultiplier);
                }
            }

            player.sendMessage(ChatColor.YELLOW + "Your flight speed has been set to " + flightMultiplier + "x");
            if (sender != player) {
                sender.sendMessage(ChatColor.YELLOW + player.getName() + "'s flight speed has been set to " + flightMultiplier + "x");
            }
        }

        @Command(aliases = {"walk", "walking", "w"}, usage = "[player] [speed-multiplier]", desc = "Set the walking speed multiplier")
        @CommandPermissions({"commandbook.speed.walk"})
        public void walkSpeed(CommandContext args, CommandSender sender) throws CommandException {
            Player player;
            float walkMultiplier = 1f;
            if (args.argsLength() == 2) {
                player = PlayerUtil.matchSinglePlayer(sender, args.getString(0));
                walkMultiplier = (float) args.getDouble(1);
            } else if (args.argsLength() == 1) {
                try {
                    walkMultiplier = (float) args.getDouble(0);
                    player = PlayerUtil.checkPlayer(sender);
                } catch (NumberFormatException e) {
                    player = PlayerUtil.matchSinglePlayer(sender, args.getString(0));
                }
            } else {
                player = PlayerUtil.checkPlayer(sender);
            }
            final float walkSpeed = DEFAULT_WALK_SPEED * walkMultiplier; // Apply multiplier

            try {
                player.setWalkSpeed(walkSpeed);
            } catch (IllegalArgumentException e) { // We gave an invalid value, tell the user nicely
                if (walkSpeed < DEFAULT_WALK_SPEED) {
                    throw new CommandException("Speed multiplier too low: " + walkMultiplier);
                } else if (walkSpeed > DEFAULT_WALK_SPEED) {
                    throw new CommandException("Speed multiplier too high: " + walkMultiplier);
                }
            }

            player.sendMessage(ChatColor.YELLOW + "Your walking speed has been set to " + walkMultiplier + "x");
            if (sender != player) {
                sender.sendMessage(ChatColor.YELLOW + player.getName() + "'s walking speed has been set to " + walkMultiplier + "x");
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        FlightSession session = sessions.getSession(FlightSession.class, event.getPlayer());
        if (config.autoEnable && CommandBook.inst().hasPermission(event.getPlayer(), "commandbook.flight.onjoin")) {
            event.getPlayer().setAllowFlight(session.canFly = true);
        }
    }

    @EventHandler
    public void onPlayerChangeGameMode(PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();
        FlightSession session = sessions.getSession(FlightSession.class, player);
        if (event.getNewGameMode() != GameMode.CREATIVE) {
            player.setAllowFlight(session.canFly);
        }
    }
}
