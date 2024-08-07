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

package com.sk89q.commandbook.component.entityspawn;

import com.google.common.collect.Lists;
import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.util.InputUtil;
import com.sk89q.commandbook.util.entity.player.PlayerUtil;
import com.sk89q.commandbook.util.item.ItemUtil;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.worldedit.blocks.BaseItem;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.world.block.BlockType;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.List;

import static com.sk89q.commandbook.util.entity.EntityUtil.matchCreatureType;

@ComponentInformation(friendlyName = "Entity Spawning", desc = "Provides commands to spawn entities.")
public class EntitySpawnComponent extends BukkitComponent {
    @Override
    public void enable() {
        registerCommands(Commands.class);
    }

    private static <T extends Enum<T>> String valueList(Class<T> clazz) {
        StringBuilder res = new StringBuilder();
        boolean first = true;
        for (Object e : clazz.getEnumConstants()) {
            if (!first) {
                res.append(", ");
            }
            res.append(e.toString());
            first = false;
        }
        return res.toString();
    }

    public LivingEntity spawn(Location loc, EntityType type, String specialTypes,
                              CommandContext args, CommandSender sender) throws CommandException {
        Entity spawned = loc.getWorld().spawn(loc, type.getEntityClass());
        if (!(spawned instanceof LivingEntity)) {
            spawned.remove();
            throw new CommandException("Not a creature!");
        }
        LivingEntity creature = (LivingEntity) spawned;

        if (args.hasFlag('d')) {
            creature.setHealth(1);
        }
        if (args.hasFlag('i')) {
            creature.setFireTicks(20 * 25);
        }
        if (args.hasFlag('r')) {
            creature.setVelocity(new Vector(0, 2, 0));
        }
        if (args.hasFlag('b')) {
            if (creature instanceof Ageable) {
                ((Ageable) creature).setBaby();
            } else if (creature instanceof Zombie) {
                ((Zombie) creature).setBaby(true);
            }
        }
        if (args.hasFlag('p')) {
            creature.setCanPickupItems(true);
        }
        /*
        if (args.hasFlag('i')) {
            EntityLiving.bJ() // @TODO see about having this exposed to api?
        }
         */
        if (args.hasFlag('t') && creature instanceof Tameable) {
            if (sender instanceof AnimalTamer) {
                ((Tameable) creature).setOwner((AnimalTamer) sender);
            } else {
                ((Tameable) creature).setTamed(true);
            }
        }

        if (creature instanceof Skeleton) {
            creature.getEquipment().setItemInHand(new ItemStack(Material.BOW));
        } else if (creature instanceof PigZombie) {
            creature.getEquipment().setItemInHand(new ItemStack(Material.GOLDEN_SWORD));
        }

        String[] types = specialTypes.split(",");

        if (!specialTypes.isEmpty() && types.length > 0) {
            outerloop:
            for (String specialType : types) {
                switch (creature.getType()) {
                    case WOLF:
                        if (specialType.matches("(?i)angry")) {
                            ((Wolf) creature).setAngry(true);
                        } else if (specialType.matches("(?i)sit(ting)?")) {
                            ((Wolf) creature).setSitting(true);
                        }
                        continue;
                    case OCELOT:
                        Ocelot.Type catType;
                        try {
                            catType = Ocelot.Type.valueOf(specialType.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            throw new CommandException("Unknown cat type '" + specialType + "'. Allowed values are: "
                                + valueList(Ocelot.Type.class));
                        }
                        if (catType != null) {
                            ((Ocelot) creature).setCatType(catType);
                        }
                        break outerloop; // only one color
                    case CREEPER:
                        if (specialType.matches("(?i)(power(ed)?|electric|lightning|shock(ed)?)")) {
                            ((Creeper) creature).setPowered(true);
                        }
                        break outerloop;
                    case SHEEP:
                        if (specialType.matches("(?i)shear(ed)?")) {
                            ((Sheep) creature).setSheared(true);
                        } else {
                            ((Sheep) creature).setColor(ItemUtil.matchDyeColor(specialType));
                        }
                        continue;
                    case PIG:
                        if (specialType.matches("(?i)saddle(d)?")) {
                            ((Pig) creature).setSaddle(true);
                        }
                        break outerloop;
                    case SLIME:
                        ((Slime) creature).setSize(Integer.parseInt(specialType));
                        break outerloop;
                    case SKELETON:
                        if (specialType.matches("(?i)wither")) {
                            ((Skeleton) creature).setSkeletonType(Skeleton.SkeletonType.WITHER);
                        }
                        break outerloop;
                    case ZOMBIFIED_PIGLIN:
                        if (specialType.matches("(?i)angry")) {
                            ((PigZombie) creature).setAngry(true);
                            return creature;
                        } else {
                            ((PigZombie) creature).setAnger(Integer.parseInt(specialType));
                        }
                        break outerloop; // having both would be redundant
                    case ZOMBIE:
                        if (specialType.matches("(?i)villager")) {
                            ((Zombie) creature).setVillager(true);
                        }
                        break;
                    case ENDERMAN:
                        BaseItem item = ItemUtil.getItem(specialType);
                        if (item == null) return creature;
                        BlockType blockType = item.getType().getBlockType();
                        if (blockType == null) return creature;
                        ((Enderman) creature).setCarriedBlock(BukkitAdapter.adapt(blockType.getDefaultState()));
                        break outerloop; // only one set of hands
                    case IRON_GOLEM:
                        if (specialType.matches("(?i)(friendly|player(-created)?)")) {
                            ((IronGolem) creature).setPlayerCreated(true);
                        }
                        break outerloop;
                    case VILLAGER:
                        Villager.Profession profession;
                        try {
                            profession = Villager.Profession.valueOf(specialType.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            throw new CommandException("Unknown profession '" + specialType + "'. Allowed values are: "
                                + valueList(Villager.Profession.class));
                        }
                        if (profession != null) {
                            ((Villager) creature).setProfession(profession);
                        }
                        break outerloop; // only one profession
                    case HORSE:
                        Horse.Color color = null;
                        Horse.Style style = null;
                        Horse.Variant variant = null;
                        try {
                            color = Horse.Color.valueOf(specialType.toUpperCase());
                        } catch (IllegalArgumentException e) {}
                        if (color != null) {
                            ((Horse) creature).setColor(color);
                            continue;
                        }
                        try {
                            style = Horse.Style.valueOf(specialType.toUpperCase());
                        } catch (IllegalArgumentException e) {}
                        if (style != null) {
                            ((Horse) creature).setStyle(style);
                            continue;
                        }
                        try {
                            variant = Horse.Variant.valueOf(specialType.toUpperCase());
                        } catch (IllegalArgumentException e) {}
                        if (variant != null) {
                            ((Horse) creature).setVariant(variant);
                            continue;
                        }
                        throw new CommandException("Unknown color, style, or variant '" + specialType + "'.");
                    default:
                        break outerloop; // can't do anything with this animal, regardless of all the types given
                }
            }
        }
        return creature;
    }

    public class Commands {
        @Command(aliases = {"spawnmob"}, usage = "<mob>[|rider] [count] [location]", desc = "Spawn a mob",
            flags = "dirbt", min = 1, max = 4)
        @CommandPermissions({"commandbook.spawnmob"})
        public void spawnMobCmd(CommandContext args, CommandSender sender) throws CommandException {
            List<Location> locations;

            if (args.argsLength() >= 3) {
                locations = InputUtil.LocationParser.matchLocations(sender, args.getString(2));
            } else {
                locations = Lists.newArrayList(PlayerUtil.checkPlayer(sender).getLocation());
            }

            String[] creatureInput = args.getString(0).split("\\|");
            boolean hasRider = creatureInput.length == 2;
            String creatureName;
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
            int total = count * locations.size();
            EntityType type = matchCreatureType(sender, creatureName, true);
            EntityType riderType = null;
            if (hasRider) {
                riderType = matchCreatureType(sender, riderName, true);
                CommandBook.inst().checkPermission(sender, "commandbook.spawnmob." + riderType.getName());
            }
            CommandBook.inst().checkPermission(sender, "commandbook.spawnmob." + type.getName());

            if ((hasRider ? total * 2 : total) > 10) {
                CommandBook.inst().checkPermission(sender, "commandbook.spawnmob.many");
            }

            for (Location loc : locations) {
                for (int i = 0; i < count; i++) {
                    LivingEntity ridee = spawn(loc, type, specialType, args, sender);
                    if (hasRider) {
                        LivingEntity rider = spawn(loc, riderType, riderSpecialType, args, sender);
                        ridee.setPassenger(rider);
                    }
                }
            }

            sender.sendMessage(ChatColor.YELLOW + "" + total + " mob(s) spawned!");
        }
    }
}
