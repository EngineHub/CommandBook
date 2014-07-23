package com.sk89q.commandbook.profiles;

import com.sk89q.commandbook.profiles.profile.Inventory;
import com.sk89q.commandbook.profiles.profile.Profile;
import com.sk89q.commandbook.profiles.profile.Vitals;
import com.sk89q.commandbook.profiles.scope.ProfileScope;
import com.sk89q.commandbook.util.entity.player.PlayerUtil;
import com.sk89q.minecraft.util.commands.*;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.List;

public class ProfilesComponent extends BukkitComponent {

    private ProfileManager manager;
    private ProfileFactory factory;

    @Override
    public void enable() {
        manager = new YAMLProfileManager();
        factory = new ProfileFactory();
    }

    public class Commands {
        @Command(aliases = {"profiles", "p"}, desc = "Profile Commands")
        @NestedCommand({NestedProfileCommands.class})
        public void profileCommands(CommandContext args, CommandSender sender) throws CommandException {

        }
    }

    public class NestedProfileCommands {
        @Command(aliases = {"save"},
                usage = "<profile name>", desc = "Save an inventory as a profile",
                flags = "viel", min = 1, max = 1)
        @CommandPermissions({"commandbook.profiles.save"})
        public void profileSaveCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = PlayerUtil.checkPlayer(sender);
            final String profileName = args.getString(0);

            ProfileScope scope = null;
            Profile profile = manager.getProfile(scope, profileName);

            if (profile != null && !args.hasFlag('o')) {
                throw new CommandException("A profile by that name already exist!");
            }

            ProfileSettings settings = new ProfileSettings(
                    profileName,
                    args.hasFlag('v'),
                    args.hasFlag('i'),
                    args.hasFlag('e'),
                    args.hasFlag('l')
            );

            if (!manager.saveProfile(scope, factory.create(player, settings))) {
                throw new CommandException("The profile: " + profileName + ", failed to save!");
            }
            sender.sendMessage("Profile: " + profileName + ", saved!");
        }

        @Command(aliases = {"load"},
                usage = "<profile name>", desc = "Load a saved inventory profile",
                flags = "ef", min = 1, max = 1)
        @CommandPermissions({"commandbook.profiles.load"})
        public void profileLoadCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = PlayerUtil.checkPlayer(sender);

            final String profileName = args.getString(0);

            Profile profile = manager.getProfile(null, profileName);

            if (profile == null) {
                throw new CommandException("A profile by that name doesn't exist!");
            }

            // Restore the contents
            Vitals proVit = profile.getVitals();
            Inventory proInv = profile.getInventory();
            if (proVit != null) {
                player.setHealth(Math.min(player.getMaxHealth(), proVit.getHealth()));
                player.setFoodLevel((int) proVit.getHunger());
                player.setSaturation((float) proVit.getSaturation());
                player.setExhaustion((float) proVit.getExhaustion());
            }
            if (proInv != null) {
                List<ItemStack> items = proInv.getItems();
                PlayerInventory pInv = player.getInventory();
                for (int i = 0; i < Math.min(items.size(), pInv.getSize()); ++i) {
                    pInv.setItem(i, items.get(i));
                }
            }

            /*
            if (args.hasFlag('e') && settings.storeExperience) {
                player.setLevel(profile.getLevel());
                player.setExp(profile.getExperience());
            }
            */

            sender.sendMessage("Profile loaded, and successfully applied!");
        }

        @Command(aliases = {"delete"},
                usage = "<profile name>", desc = "Delete a saved inventory profile",
                flags = "", min = 1, max = 1)
        @CommandPermissions({"commandbook.profiles.delete"})
        public void profileDeleteCmd(CommandContext args, CommandSender sender) throws CommandException {

            /*
            if (!manager.deleteProfile(args.getString(0))) {
                throw new CommandException("That profile couldn't be deleted!");
            }
            */
            sender.sendMessage("Profile deleted!");
        }
    }
}
