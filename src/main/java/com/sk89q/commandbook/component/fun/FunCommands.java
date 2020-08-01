/*
 * CommandBook
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) CommandBook team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.commandbook.component.fun;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.command.argument.MultiPlayerTarget;
import com.sk89q.commandbook.util.ChatUtil;
import com.sk89q.commandbook.util.entity.EntityUtil;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.worldedit.command.util.CommandPermissions;
import com.sk89q.worldedit.command.util.CommandPermissionsConditionGenerator;
import com.zachsthings.libcomponents.bukkit.BasePlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.annotation.param.Arg;
import org.enginehub.piston.annotation.param.Switch;

import java.util.concurrent.ThreadLocalRandom;

@CommandContainer(superTypes = CommandPermissionsConditionGenerator.Registration.class)
public class FunCommands {
    private static final ThreadLocalRandom RANDOM = ThreadLocalRandom.current();

    private FunComponent component;

    public FunCommands(FunComponent component) {
        this.component = component;
    }

    @Command(
        name = "ping",
        desc = "A dummy command"
    )
    public void pingCmd(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "Pong!");
    }

    @Command(
        name = "pong",
        desc = "A dummy command"
    )
    @CommandPermissions({"commandbook.pong"})
    public void pongCmd(CommandSender sender) {
        FunComponentConfiguration config = component.getConfig();
        String message = ChatColor.YELLOW + String.format(
            config.pongMessage, ChatUtil.toColoredName(sender, ChatColor.YELLOW)
        );

        if (config.pongBroadcast) {
            Bukkit.broadcastMessage(message);
        } else {
            sender.sendMessage(message);
        }
    }

    @Command(
        name = "slap",
        desc = "Slap a player"
    )
    @CommandPermissions({"commandbook.slap", "commandbook.slap.other"})
    public void slapCmd(CommandSender sender,
                        @Switch(name = 'v', desc = "very hard")
                            boolean veryHard,
                        @Switch(name = 'h', desc = "hard")
                            boolean hard,
                        @Switch(name = 'd', desc = "damage")
                            boolean damage,
                        @Switch(name = 'q', desc = "quiet")
                            boolean quiet,
                        @Switch(name = 's', desc = "silent")
                            boolean silent,
                        @Arg(desc = "player(s) to target")
                            MultiPlayerTarget targetPlayers) throws CommandException {
        // Check permissions!
        for (Player player : targetPlayers) {
            if (player == sender) {
                CommandBook.inst().checkPermission(sender, "commandbook.slap");
            } else {
                CommandBook.inst().checkPermission(sender, "commandbook.slap.other");
            }
        }

        boolean included = false;
        int count = 0;
        for (Player player : targetPlayers) {

            if (veryHard) {
                player.setVelocity(new Vector(
                    RANDOM.nextDouble() * 10.0 - 5,
                    RANDOM.nextDouble() * 10,
                    RANDOM.nextDouble() * 10.0 - 5));
            } else if (hard) {
                player.setVelocity(new Vector(
                    RANDOM.nextDouble() * 5.0 - 2.5,
                    RANDOM.nextDouble() * 5,
                    RANDOM.nextDouble() * 5.0 - 2.5));
            } else {
                player.setVelocity(new Vector(
                    RANDOM.nextDouble() * 2.0 - 1,
                    RANDOM.nextDouble() * 1,
                    RANDOM.nextDouble() * 2.0 - 1));
            }

            if (damage) {
                player.setHealth(Math.max(0, player.getHealth() - 1));
            }

            // Tell the players
            if (quiet || silent) {
                if (player.equals(sender)) {
                    player.sendMessage(ChatColor.YELLOW + "Slapped!");

                    included = true;
                } else if (!silent) {
                    player.sendMessage(ChatColor.YELLOW + "You've been slapped by "
                        + ChatUtil.toColoredName(sender, ChatColor.YELLOW) + ".");
                }
            } else {
                if (count < 3) {
                    BasePlugin.server().broadcastMessage(
                        ChatColor.YELLOW + ChatUtil.toColoredName(sender, ChatColor.YELLOW)
                            + " slapped " + ChatUtil.toColoredName(player, ChatColor.YELLOW));
                } else if (count == 3) {
                    BasePlugin.server().broadcastMessage(
                        ChatColor.YELLOW + ChatUtil.toColoredName(sender, ChatColor.YELLOW)
                            + " slapped more people...");
                }
                count++;
            }
        }

        // The player didn't receive any items, then we need to send the
        // user a message so s/he know that something is indeed working
        if (!included && (quiet || silent)) {
            sender.sendMessage(ChatColor.YELLOW.toString() + "Players slapped.");
        }
    }

    @Command(
        name = "rocket",
        desc = "Rocket a player"
    )
    @CommandPermissions({"commandbook.rocket", "commandbook.rocket.other"})
    public void rocketCmd(CommandSender sender,
                          @Switch(name = 'h', desc = "hard")
                              boolean hard,
                          @Switch(name = 'q', desc = "quiet")
                              boolean quiet,
                          @Switch(name = 's', desc = "silent")
                              boolean silent,
                          @Arg(desc = "player(s) to target")
                              MultiPlayerTarget targetPlayers) throws CommandException {
        // Check permissions!
        for (Player player : targetPlayers) {
            if (player == sender) {
                CommandBook.inst().checkPermission(sender, "commandbook.rocket");
            } else {
                CommandBook.inst().checkPermission(sender, "commandbook.rocket.other");
            }
        }

        boolean included = false;
        int count = 0;
        for (Player player : targetPlayers) {
            if (hard) {
                player.setVelocity(new Vector(0, 4, 0));
            } else {
                player.setVelocity(new Vector(0, 2, 0));
            }

            if (quiet || silent) {
                // Tell the user
                if (player.equals(sender)) {
                    player.sendMessage(ChatColor.YELLOW + "Rocketed!");

                    // Keep track of this
                    included = true;
                } else if (!silent) {
                    player.sendMessage(ChatColor.YELLOW + "You've been rocketed by "
                        + ChatUtil.toColoredName(sender, ChatColor.YELLOW) + ".");

                }
            } else {
                if (count < 3) {
                    BasePlugin.server().broadcastMessage(
                        ChatColor.YELLOW + ChatUtil.toColoredName(sender, ChatColor.YELLOW)
                            + " rocketed " + ChatUtil.toColoredName(player, ChatColor.YELLOW));
                } else if (count == 3) {
                    BasePlugin.server().broadcastMessage(
                        ChatColor.YELLOW + ChatUtil.toColoredName(sender, ChatColor.YELLOW)
                            + " rocketed more people...");
                }
                count++;
            }
        }

        // The player didn't receive any items, then we need to send the
        // user a message so s/he know that something is indeed working
        if (!included && (quiet || silent)) {
            sender.sendMessage(ChatColor.YELLOW.toString() + "Players rocketed.");
        }
    }

    @Command(
        name = "barrage",
        desc = "Send a barrage of arrows"
    )
    @CommandPermissions({"commandbook.barrage", "commandbook.barrage.other"})
    public void barrageCmd(CommandSender sender,
                           @Switch(name = 'q', desc = "quiet")
                               boolean quiet,
                           @Switch(name = 's', desc = "silent")
                               boolean silent,
                           @Arg(desc = "player(s) to target")
                               MultiPlayerTarget targetPlayers) throws CommandException {
        // Check permissions!
        for (Player player : targetPlayers) {
            if (player == sender) {
                CommandBook.inst().checkPermission(sender, "commandbook.barrage");
            } else {
                CommandBook.inst().checkPermission(sender, "commandbook.barrage.other");
            }
        }

        boolean included = false;
        int count = 0;
        for (Player player : targetPlayers) {
            EntityUtil.sendProjectilesFromEntity(player, 24, 2, Arrow.class);

            if (quiet || silent) {
                // Tell the user
                if (player.equals(sender)) {
                    player.sendMessage(ChatColor.YELLOW + "Barrage attack!");

                    // Keep track of this
                    included = true;
                } else if (!silent) {
                    player.sendMessage(ChatColor.YELLOW + "BARRAGE attack from "
                        + ChatUtil.toColoredName(sender, ChatColor.YELLOW) + ".");

                }
            } else {
                if (count < 3) {
                    BasePlugin.server().broadcastMessage(
                        ChatColor.YELLOW + ChatUtil.toColoredName(sender, ChatColor.YELLOW)
                            + " used BARRAGE on " + ChatUtil.toColoredName(player, ChatColor.YELLOW));
                } else if (count == 3) {
                    BasePlugin.server().broadcastMessage(
                        ChatColor.YELLOW + ChatUtil.toColoredName(sender, ChatColor.YELLOW)
                            + " used it on more people...");
                }
                count++;
            }
        }

        // The player didn't receive any items, then we need to send the
        // user a message so s/he know that something is indeed working
        if (!included && (quiet || silent)) {
            sender.sendMessage(ChatColor.YELLOW.toString() + "Barrage attack sent.");
        }
    }

    @Command(
        name = "firebarrage",
        desc = "Send an attack of fireballs"
    )
    @CommandPermissions({"commandbook.firebarrage", "commandbook.firebarrage.other"})
    public void firebarrageCmd(CommandSender sender,
                               @Switch(name = 'q', desc = "quiet")
                                   boolean quiet,
                               @Switch(name = 's', desc = "silent")
                                   boolean silent,
                               @Arg(desc = "player(s) to target")
                                   MultiPlayerTarget targetPlayers) throws CommandException {
        // Check permissions!
        for (Player player : targetPlayers) {
            if (player == sender) {
                CommandBook.inst().checkPermission(sender, "commandbook.firebarrage");
            } else {
                CommandBook.inst().checkPermission(sender, "commandbook.firebarrage.other");
            }
        }

        boolean included = false;
        int count = 0;
        for (Player player : targetPlayers) {
            // moved math to util because I felt like it
            EntityUtil.sendProjectilesFromEntity(player, 8, 10, Fireball.class);

            if (quiet || silent) {
                // Tell the user
                if (player.equals(sender)) {
                    player.sendMessage(ChatColor.YELLOW + "Fireball attack!");

                    // Keep track of this
                    included = true;
                } else if (!silent) {
                    player.sendMessage(ChatColor.YELLOW + "Fireball attack from "
                        + ChatUtil.toColoredName(sender, ChatColor.YELLOW) + ".");

                }
            } else {
                if (count < 3) {
                    BasePlugin.server().broadcastMessage(
                        ChatColor.YELLOW + ChatUtil.toColoredName(sender, ChatColor.YELLOW)
                            + " used Fireball attack on " + ChatUtil.toColoredName(player, ChatColor.YELLOW));
                } else if (count == 3) {
                    BasePlugin.server().broadcastMessage(
                        ChatColor.YELLOW + ChatUtil.toColoredName(sender, ChatColor.YELLOW)
                            + " used it on more people...");
                }
                count++;
            }
        }

        // The player didn't receive any items, then we need to send the
        // user a message so s/he know that something is indeed working
        if (!included && (quiet || silent)) {
            sender.sendMessage(ChatColor.YELLOW.toString() + "Fireball attack sent.");
        }
    }

    @Command(
        name = "cannon",
        desc = "Send a ball of fire to a face"
    )
    @CommandPermissions({"commandbook.cannon", "commandbook.cannon.other"})
    public void cannonCmd(CommandSender sender,
                          @Switch(name = 'q', desc = "quiet")
                              boolean quiet,
                          @Switch(name = 's', desc = "silent")
                              boolean silent,
                          @Arg(desc = "player(s) to target")
                              MultiPlayerTarget targetPlayers) throws CommandException {
        // Check permissions!
        for (Player player : targetPlayers) {
            if (player == sender) {
                CommandBook.inst().checkPermission(sender, "commandbook.cannon");
            } else {
                CommandBook.inst().checkPermission(sender, "commandbook.cannon.other");
            }
        }

        boolean included = false;
        int count = 0;
        for (Player player : targetPlayers) {
            double diff = (2 * Math.PI) / 24.0;
            for (double a = 0; a < 2 * Math.PI; a += diff) {
                player.getWorld().spawnEntity(player.getEyeLocation(), EntityType.FIREBALL);
            }

            if (quiet || silent) {
                // Tell the user
                if (player.equals(sender)) {
                    player.sendMessage(ChatColor.YELLOW + "Fireball attack!");

                    // Keep track of this
                    included = true;
                } else if (!silent) {
                    player.sendMessage(ChatColor.YELLOW + "Fireball attack from "
                        + ChatUtil.toColoredName(sender, ChatColor.YELLOW) + ".");

                }
            } else {
                if (count < 3) {
                    BasePlugin.server().broadcastMessage(
                        ChatColor.YELLOW + ChatUtil.toColoredName(sender, ChatColor.YELLOW)
                            + " used Fireball attack on " + ChatUtil.toColoredName(player, ChatColor.YELLOW));
                } else if (count == 3) {
                    BasePlugin.server().broadcastMessage(
                        ChatColor.YELLOW + ChatUtil.toColoredName(sender, ChatColor.YELLOW)
                            + " used it more people...");
                }
                count++;
            }
        }

        // The player didn't receive any items, then we need to send the
        // user a message so s/he know that something is indeed working
        if (!included && (quiet || silent)) {
            sender.sendMessage(ChatColor.YELLOW.toString() + "Fireball attack sent.");
        }
    }
}
