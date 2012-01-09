/*
 * CommandBook
 * Copyright (C) 2011 sk89q <http://www.sk89q.com>
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

package com.sk89q.commandbook;

import com.sk89q.commandbook.components.AbstractComponent;
import com.sk89q.commandbook.components.ComponentInformation;
import com.sk89q.commandbook.util.ItemUtil;
import com.sk89q.commandbook.util.LocationUtil;
import com.sk89q.commandbook.util.PlayerUtil;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import static com.sk89q.commandbook.util.EntityUtil.matchCreatureType;
import java.util.Random;

@ComponentInformation(friendlyName = "Fun", desc = "Provides some fun commands to toy with users. (/rocket and /pong are two fun ones)")
public class FunComponent extends AbstractComponent {
    private static final Random random = new Random();

    @Override
    public void initialize() {
        registerCommands(Commands.class);
    }

    public LivingEntity spawn(Location loc, CreatureType type, String specialType,
                               CommandContext args, CommandSender sender) throws CommandException {
        LivingEntity creature = loc.getWorld().spawnCreature(loc, type);
        if (args.hasFlag('d')) {
            creature.setHealth(1);
        }
        if (args.hasFlag('i')) {
            creature.setFireTicks(20 * 25);
        }
        if (args.hasFlag('r')) {
            creature.setVelocity(new Vector(0, 2, 0));
        }
        if (args.hasFlag('b') && creature instanceof Animals) {
            ((Animals) creature).setAge(-24000);
        }
        if (!specialType.equals("")) {
            if (creature instanceof Wolf) {
                if (specialType.matches("angry")) {
                    ((Wolf) creature).setAngry(true);
                    return creature;
                } else if (specialType.matches("sit(ting)?")) {
                    ((Wolf) creature).setSitting(true);
                    return creature;
                } else if (specialType.matches("tame(d)?") && sender instanceof Player) {
                    ((Wolf) creature).setOwner(((Player) sender));
                    return creature;
                }
            } else if (creature instanceof Creeper
                    && specialType.matches("(power(ed)?|electric|lightning|shock(ed)?)")) {
                ((Creeper) creature).setPowered(true);
                return creature;
            } else if (creature instanceof Sheep) {
                if (specialType.matches("shear(ed)?")) {
                    ((Sheep) creature).setSheared(true);
                    return creature;
                }
                ((Sheep) creature).setColor(ItemUtil.matchDyeColor(specialType));
                return creature;
            } else if (creature instanceof Pig
                    && specialType.matches("saddle(d)?")) {
                ((Pig) creature).setSaddle(true);
                return creature;
            } else if (creature instanceof Slime) {
                ((Slime) creature).setSize(Integer.parseInt(specialType));
                return creature;
            } else if (creature instanceof PigZombie) {
                if (specialType.matches("angry")) {
                    ((PigZombie) creature).setAngry(true);
                    return creature;
                }
                ((PigZombie) creature).setAnger(Integer.parseInt(specialType));
                return creature;
            } else if (creature instanceof Enderman) {
                ItemStack item = CommandBook.inst().getItem(specialType);
                if (item == null) return creature;
               ((Enderman) creature).setCarriedMaterial(item.getData());
                return creature;
            }
        }
        return creature;
    }

    public class Commands {
        @Command(aliases = {"ping"},
                usage = "", desc = "A dummy command",
                flags = "", min = 0, max = 0)
        public void ping(CommandContext args, CommandSender sender) throws CommandException {
            sender.sendMessage(ChatColor.YELLOW + "Pong!");
        }

        @Command(aliases = {"pong"},
                usage = "", desc = "A dummy command",
                flags = "", min = 0, max = 0)
        public void pong(CommandContext args, CommandSender sender) throws CommandException {

            sender.sendMessage(ChatColor.YELLOW +
                    "I hear " + PlayerUtil.toName(sender) + " likes cute Asian boys.");
        }

        @Command(aliases = {"spawnmob"}, usage = "<mob>[|rider] [count] [location]", desc = "Spawn a mob",
                flags = "dirb", min = 1, max = 4)
        @CommandPermissions({"commandbook.spawnmob"})
        public void spawnMob(CommandContext args, CommandSender sender) throws CommandException {
            Location loc;

            if (args.argsLength() >= 3) {
                loc = LocationUtil.matchLocation(sender, args.getString(2));
            } else {
                loc = PlayerUtil.checkPlayer(sender).getLocation();
            }

            String[] creatureInput = args.getString(0).split("\\|");
            boolean hasRider = creatureInput.length == 2;
            String creatureName = "";
            String riderName = "";
            String specialType = "";
            String riderSpecialType = "";
            if (creatureInput[0].contains(":")) {
                String[] nameParts = creatureInput[0].split(":", 2);
                creatureName = nameParts[0];
                specialType = nameParts[1].toLowerCase();
            } else {
                creatureName = creatureInput[0];
            }

            if (hasRider) {
                if (creatureInput[1].contains(":")) {
                    String[] nameParts = creatureInput[1].split(":", 2);
                    riderName = nameParts[0];
                    riderSpecialType = nameParts[1].toLowerCase();
                } else {
                    riderName = creatureInput[1];
                }
            }

            int count = Math.max(1, args.getInteger(1, 1));
            CreatureType type = matchCreatureType(sender, creatureName);
            CreatureType riderType = null;
            if (hasRider) {
                riderType = matchCreatureType(sender, riderName);
                CommandBook.inst().checkPermission(sender, "commandbook.spawnmob." + riderType.getName());
            }
            CommandBook.inst().checkPermission(sender, "commandbook.spawnmob." + type.getName());

            if ((hasRider ? count * 2 : count) > 10) {
                CommandBook.inst().checkPermission(sender, "commandbook.spawnmob.many");
            }

            for (int i = 0; i < count; i++) {
                LivingEntity ridee = spawn(loc, type, specialType, args, sender);
                if (hasRider) {
                    LivingEntity rider = spawn(loc, riderType, riderSpecialType, args, sender);
                    ridee.setPassenger(rider);
                }
            }

            sender.sendMessage(ChatColor.YELLOW + "" + count + " mob(s) spawned!");
        }

        @Command(aliases = {"slap"}, usage = "[target]", desc = "Slap a player", flags = "hdvs", min = 0, max = 1)
        @CommandPermissions({"commandbook.slap"})
        public void slap(CommandContext args, CommandSender sender) throws CommandException {
            Iterable<Player> targets = null;
            boolean included = false;
            int count = 0;

            // Detect arguments based on the number of arguments provided
            if (args.argsLength() == 0) {
                targets = PlayerUtil.matchPlayers(PlayerUtil.checkPlayer(sender));

                // Check permissions!
                CommandBook.inst().checkPermission(sender, "commandbook.slap");
            } else if (args.argsLength() == 1) {
                targets = PlayerUtil.matchPlayers(sender, args.getString(0));

                // Check permissions!
                CommandBook.inst().checkPermission(sender, "commandbook.slap.other");
            }

            for (Player player : targets) {
                count++;

                if (args.hasFlag('v')) {
                    player.setVelocity(new Vector(
                            random.nextDouble() * 10.0 - 5,
                            random.nextDouble() * 10,
                            random.nextDouble() * 10.0 - 5));
                } else if (args.hasFlag('h')) {
                    player.setVelocity(new Vector(
                            random.nextDouble() * 5.0 - 2.5,
                            random.nextDouble() * 5,
                            random.nextDouble() * 5.0 - 2.5));
                } else {
                    player.setVelocity(new Vector(
                            random.nextDouble() * 2.0 - 1,
                            random.nextDouble() * 1,
                            random.nextDouble() * 2.0 - 1));
                }

                if (args.hasFlag('d')) {
                    player.setHealth(player.getHealth() - 1);
                }

                if (args.hasFlag('s')) {
                    // Tell the user
                    if (player.equals(sender)) {
                        player.sendMessage(ChatColor.YELLOW + "Slapped!");

                        // Keep track of this
                        included = true;
                    } else {
                        player.sendMessage(ChatColor.YELLOW + "You've been slapped by "
                                + PlayerUtil.toName(sender) + ".");

                    }
                } else {
                    if (count < 6) {
                        CommandBook.server().broadcastMessage(
                                ChatColor.YELLOW + PlayerUtil.toName(sender)
                                        + " slapped " + PlayerUtil.toName(player));
                    } else if (count == 6) {
                        CommandBook.server().broadcastMessage(
                                ChatColor.YELLOW + PlayerUtil.toName(sender)
                                        + " slapped more people...");
                    }
                }
            }

            // The player didn't receive any items, then we need to send the
            // user a message so s/he know that something is indeed working
            if (!included && args.hasFlag('s')) {
                sender.sendMessage(ChatColor.YELLOW.toString() + "Players slapped.");
            }
        }

        @Command(aliases = {"rocket"}, usage = "[target]", desc = "Rocket a player", flags = "hs", min = 0, max = 1)
        @CommandPermissions({"commandbook.rocket"})
        public void rocket(CommandContext args, CommandSender sender) throws CommandException {
            Iterable<Player> targets = null;
            boolean included = false;
            int count = 0;

            // Detect arguments based on the number of arguments provided
            if (args.argsLength() == 0) {
                targets = PlayerUtil.matchPlayers(PlayerUtil.checkPlayer(sender));
            } else if (args.argsLength() == 1) {
                targets = PlayerUtil.matchPlayers(sender, args.getString(0));

                // Check permissions!
                CommandBook.inst().checkPermission(sender, "commandbook.rocket.other");
            }

            for (Player player : targets) {
                if (args.hasFlag('h')) {
                    player.setVelocity(new Vector(0, 50, 0));
                } else {
                    player.setVelocity(new Vector(0, 20, 0));
                }

                if (args.hasFlag('s')) {
                    // Tell the user
                    if (player.equals(sender)) {
                        player.sendMessage(ChatColor.YELLOW + "Rocketed!");

                        // Keep track of this
                        included = true;
                    } else {
                        player.sendMessage(ChatColor.YELLOW + "You've been rocketed by "
                                + PlayerUtil.toName(sender) + ".");

                    }
                } else {
                    if (count < 6) {
                        CommandBook.server().broadcastMessage(
                                ChatColor.YELLOW + PlayerUtil.toName(sender)
                                        + " rocketed " + PlayerUtil.toName(player));
                    } else if (count == 6) {
                        CommandBook.server().broadcastMessage(
                                ChatColor.YELLOW + PlayerUtil.toName(sender)
                                        + " rocketed more people...");
                    }
                }
            }

            // The player didn't receive any items, then we need to send the
            // user a message so s/he know that something is indeed working
            if (!included && args.hasFlag('s')) {
                sender.sendMessage(ChatColor.YELLOW.toString() + "Players rocketed.");
            }
        }

        @Command(aliases = {"barrage"}, usage = "[target]", desc = "Send a barrage of arrows", flags = "s", min = 0, max = 1)
        @CommandPermissions({"commandbook.barrage"})
        public void barrage(CommandContext args, CommandSender sender) throws CommandException {
            Iterable<Player> targets = null;
            boolean included = false;
            int count = 0;

            // Detect arguments based on the number of arguments provided
            if (args.argsLength() == 0) {
                targets = PlayerUtil.matchPlayers(PlayerUtil.checkPlayer(sender));
            } else if (args.argsLength() == 1) {
                targets = PlayerUtil.matchPlayers(sender, args.getString(0));

                // Check permissions!
                CommandBook.inst().checkPermission(sender, "commandbook.barrage.other");
            }

            for (Player player : targets) {
                double diff = (2 * Math.PI) / 24.0;
                for (double a = 0; a < 2 * Math.PI; a += diff) {
                    Vector vel = new Vector(Math.cos(a), 0, Math.sin(a));
                    CommandBookUtil.sendArrowFromPlayer(player, vel, 2);
                }

                if (args.hasFlag('s')) {
                    // Tell the user
                    if (player.equals(sender)) {
                        player.sendMessage(ChatColor.YELLOW + "Barrage attack!");

                        // Keep track of this
                        included = true;
                    } else {
                        player.sendMessage(ChatColor.YELLOW + "BARRAGE attack from "
                                + PlayerUtil.toName(sender) + ".");

                    }
                } else {
                    if (count < 6) {
                        CommandBook.server().broadcastMessage(
                                ChatColor.YELLOW + PlayerUtil.toName(sender)
                                        + " used BARRAGE on " + PlayerUtil.toName(player));
                    } else if (count == 6) {
                        CommandBook.server().broadcastMessage(
                                ChatColor.YELLOW + PlayerUtil.toName(sender)
                                        + " used it more people...");
                    }
                }
            }

            // The player didn't receive any items, then we need to send the
            // user a message so s/he know that something is indeed working
            if (!included && args.hasFlag('s')) {
                sender.sendMessage(ChatColor.YELLOW.toString() + "Barrage attack sent.");
            }
        }

        @Command(aliases = {"firebarrage"}, usage = "[target]", desc = "Send a attack of fireballs", flags = "s",
                min = 0, max = 1)
        @CommandPermissions({"commandbook.firebarrage"})
        public void barragefire(CommandContext args, CommandSender sender) throws CommandException {

            Iterable<Player> targets = null;
            boolean included = false;
            int count = 0;

            // Detect arguments based on the number of arguments provided
            if (args.argsLength() == 0) {
                targets = PlayerUtil.matchPlayers(PlayerUtil.checkPlayer(sender));
            } else if (args.argsLength() == 1) {
                targets = PlayerUtil.matchPlayers(sender, args.getString(0));

                // Check permissions!
                CommandBook.inst().checkPermission(sender, "commandbook.firebarrage.other");
            }

            for (Player player : targets) {
                // moved math to util because I felt like it
                CommandBookUtil.sendFireballsFromPlayer(player, 8);

                if (args.hasFlag('s')) {
                    // Tell the user
                    if (player.equals(sender)) {
                        player.sendMessage(ChatColor.YELLOW + "Fireball attack!");

                        // Keep track of this
                        included = true;
                    } else {
                        player.sendMessage(ChatColor.YELLOW + "Fireball attack from "
                                + PlayerUtil.toName(sender) + ".");

                    }
                } else {
                    if (count < 6) {
                        CommandBook.server().broadcastMessage(
                                ChatColor.YELLOW + PlayerUtil.toName(sender)
                                        + " used Fireball attack on " + PlayerUtil.toName(player));
                    } else if (count == 6) {
                        CommandBook.server().broadcastMessage(
                                ChatColor.YELLOW + PlayerUtil.toName(sender)
                                        + " used it more people...");
                    }
                }
            }

            // The player didn't receive any items, then we need to send the
            // user a message so s/he know that something is indeed working
            if (!included && args.hasFlag('s')) {
                sender.sendMessage(ChatColor.YELLOW.toString() + "Fireball attack sent.");
            }
        }
        @Command(aliases = {"cannon"},
                usage = "[target]", desc = "Send a ball of fire to a face", flags = "s",
                min = 0, max = 1)
        @CommandPermissions({"commandbook.cannon"})
       public void cannon(CommandContext args, CommandSender sender) throws CommandException {

            Iterable<Player> targets = null;
            boolean included = false;
            int count = 0;
            
            // Detect arguments based on the number of arguments provided
            if (args.argsLength() == 0) {
                targets = PlayerUtil.matchPlayers(PlayerUtil.checkPlayer(sender));
            } else if (args.argsLength() == 1) {            
                targets = PlayerUtil.matchPlayers(sender, args.getString(0));
                
                // Check permissions!
               CommandBook.inst().checkPermission(sender, "commandbook.barragefire.other");
            }

            for (Player player : targets) {
                double diff = (2 * Math.PI) / 24.0;
                for (double a = 0; a < 2 * Math.PI; a += diff) {
                    CommandBookUtil.sendCannonToPlayer(player);
                }

                if (args.hasFlag('s')) {
                    // Tell the user
                    if (player.equals(sender)) {
                        player.sendMessage(ChatColor.YELLOW + "Fireball attack!");
                        
                        // Keep track of this
                        included = true;
                    } else {
                        player.sendMessage(ChatColor.YELLOW + "Fireball attack from "
                                + PlayerUtil.toName(sender) + ".");
                        
                    }
                } else {
                    if (count < 6) {
                    	CommandBook.server().broadcastMessage(
                                ChatColor.YELLOW + PlayerUtil.toName(sender)
                                + " used Fireball attack on " + PlayerUtil.toName(player));
                    } else if (count == 6) {
                    	CommandBook.server().broadcastMessage(
                                ChatColor.YELLOW + PlayerUtil.toName(sender)
                                + " used it more people...");
                    }
                }
            }
            
            // The player didn't receive any items, then we need to send the
            // user a message so s/he know that something is indeed working
            if (!included && args.hasFlag('s')) {
                sender.sendMessage(ChatColor.YELLOW.toString() + "Fireball attack sent.");
            }
        }

    }
}
