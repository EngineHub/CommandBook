package com.sk89q.commandbook.profiles;

import com.sk89q.commandbook.profiles.binary.BinaryProfileManager;
import com.sk89q.commandbook.profiles.editions.Profile_E1;
import com.sk89q.commandbook.util.entity.player.PlayerUtil;
import com.sk89q.minecraft.util.commands.*;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ProfilesComponent extends BukkitComponent {

    private ProfileManager manager;

    @Override
    public void enable() {
        manager = new BinaryProfileManager();
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
                flags = "oive", min = 1, max = 1)
        @CommandPermissions({"commandbook.profiles.save"})
        public void profileSaveCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = PlayerUtil.checkPlayer(sender);
            final String profileName = args.getString(0);

            Profile_E1 profile = manager.getProfile(profileName);

            if (profile != null && !args.hasFlag('o')) {
                throw new CommandException("A profile by that name already exist!");
            }

            ProfileSettings settings = new ProfileSettings();
            settings.storeInventory = args.hasFlag('i');
            settings.storeVitals = args.hasFlag('v');
            settings.storeExpirence = args.hasFlag('e');

            if (!manager.saveProfile(profileName, manager.createProfile(player, settings))) {
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

            Profile_E1 profile = manager.getProfile(profileName);

            if (profile == null) {
                throw new CommandException("A profile by that name doesn't exist!");
            }

            // Restore the contents
            ProfileSettings settings = profile.getSettings();
            if (settings.storeInventory) {
                player.getInventory().setArmorContents(profile.getArmourContents());
                player.getInventory().setContents(profile.getInventoryContents());
            }
            if (settings.storeVitals) {
                player.setHealth(Math.min(player.getMaxHealth(), profile.getHealth()));
                player.setFoodLevel(profile.getHunger());
                player.setSaturation(profile.getSaturation());
                player.setExhaustion(profile.getExhaustion());
            }
            if (args.hasFlag('e') && settings.storeExpirence) {
                player.setLevel(profile.getLevel());
                player.setExp(profile.getExperience());
            }

            sender.sendMessage("Profile loaded, and successfully applied!");
        }

        @Command(aliases = {"delete"},
                usage = "<profile name>", desc = "Delete a saved inventory profile",
                flags = "", min = 1, max = 1)
        @CommandPermissions({"commandbook.profiles.delete"})
        public void profileDeleteCmd(CommandContext args, CommandSender sender) throws CommandException {

            if (!manager.deleteProfile(args.getString(0))) {
                throw new CommandException("That profile couldn't be deleted!");
            }
            sender.sendMessage("Profile deleted!");
        }
    }
}
