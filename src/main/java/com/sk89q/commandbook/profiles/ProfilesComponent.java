package com.sk89q.commandbook.profiles;

import com.sk89q.commandbook.profiles.profile.Inventory;
import com.sk89q.commandbook.profiles.profile.Profile;
import com.sk89q.commandbook.profiles.profile.Vitals;
import com.sk89q.commandbook.profiles.scope.*;
import com.sk89q.commandbook.util.entity.player.PlayerUtil;
import com.sk89q.minecraft.util.commands.*;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.List;

public class ProfilesComponent extends BukkitComponent {

    private static final String defaultSave = "DEFAULT";

    private TagManager tags = null;
    private ProfileManager manager;
    private ProfileFactory factory;

    @Override
    public void enable() {
        manager = new YAMLProfileManager();
        factory = new ProfileFactory();
    }

    public static boolean restore(Player player, Profile profile) {
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
        return true;
    }

    public class Commands {
        @Command(aliases = {"profiles", "p"}, desc = "Profile Commands")
        @NestedCommand({NestedProfileCommands.class})
        public void profileCommands(CommandContext args, CommandSender sender) throws CommandException {

        }
    }

    public class NestedProfileCommands {

        private class ProfileTypeName {
            private final ProfileType type;
            private final String name;

            private ProfileTypeName(ProfileType type, String name) {
                this.type = type;
                this.name = name;
            }

            public ProfileType getType() {
                return type;
            }

            public String getName() {
                return name;
            }
        }

        private ProfileTypeName parseType(String name) {
            if (name.startsWith("#")) {
                return new ProfileTypeName(ProfileType.TAG, name.substring(1, name.length()));
            } else if (name.startsWith("@")) {
                return new ProfileTypeName(ProfileType.GLOBAL, name.substring(1, name.length()));
            } else {
                return new ProfileTypeName(ProfileType.PERSONAL, name);
            }
        }

        private ProfileScope resolveScope(Player player, ProfileType type) {
            switch (type) {
                case GLOBAL:
                    return new GlobalScope();
                case PERSONAL:
                    return new PersonalScope(player.getUniqueId());
                case TAG:
                    return new TagScope();
            }
            return null;
        }

        @Command(aliases = {"save"},
                usage = "<profile name>", desc = "Save an inventory as a profile",
                flags = "viel", min = 1, max = 1)
        @CommandPermissions({"commandbook.profiles.save"})
        public void profileSaveCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = PlayerUtil.checkPlayer(sender);
            String profileName = args.getString(0);

            ProfileTypeName typeName = parseType(profileName);
            profileName = typeName.getName();
            ProfileScope scope = resolveScope(player, typeName.getType());

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

        @Command(aliases = {"clear"},
                usage = "", desc = "Clear the current tag profile",
                flags = "", min = 0, max = 0)
        @CommandPermissions({"commandbook.profiles.load"})
        public void profileClearCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = PlayerUtil.checkPlayer(sender);

            ProfileScope scope = new PersonalTagScope(player.getUniqueId());
            for (ProfileTag tag : tags.getActiveTags(player.getUniqueId())) {
                Profile profile = manager.getProfile(scope, tag.getName());
                restore(player, profile);
                manager.remProfile(scope, profile);
            }
            tags.clearTags(player.getUniqueId());
            player.sendMessage(ChatColor.YELLOW + "Current tag profile(s) cleared!");
        }

        private void loadTag(Player player, String tagName) throws CommandException {
            List<ProfileTag> activeTags = tags.getActiveTags(player.getUniqueId());
            if (!activeTags.isEmpty()) {
                throw new CommandException("You already have tags activated.");
            }

            ProfileTag tag = tags.getTag(tagName);
            if (tag == null) {
                throw new CommandException("No such tag exist!");
            }
            if (!tags.loadTag(player.getUniqueId(), tag)) {
                throw new CommandException("That tag has already been loaded!");
            }

            ProfileSettings settings = new ProfileSettings(
                    tag.getName(),
                    true,
                    true,
                    true,
                    false
            );

            manager.saveProfile(new PersonalTagScope(player.getUniqueId()), factory.create(player, settings));
        }

        @Command(aliases = {"load"},
                usage = "<profile name>", desc = "Load a saved inventory profile",
                flags = "ef", min = 1, max = 1)
        @CommandPermissions({"commandbook.profiles.load"})
        public void profileLoadCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = PlayerUtil.checkPlayer(sender);

            String profileName = args.getString(0);

            ProfileTypeName typeName = parseType(profileName);
            profileName = typeName.getName();
            ProfileScope scope = resolveScope(player, typeName.getType());

            Profile profile = manager.getProfile(scope, profileName);

            if (profile == null) {
                throw new CommandException("A profile by that name doesn't exist.");
            }

            if (scope instanceof TagScope) {
                loadTag(player, profileName);
            }

            // Restore the contents
            if (!restore(player, profile)) {
                throw new CommandException("Failed to apply the profile.");
            }
            sender.sendMessage(ChatColor.YELLOW + "Profile loaded, and successfully applied!");
        }

        @Command(aliases = {"delete"},
                usage = "<profile name>", desc = "Delete a saved inventory profile",
                flags = "", min = 1, max = 1)
        @CommandPermissions({"commandbook.profiles.delete"})
        public void profileDeleteCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = PlayerUtil.checkPlayer(sender);
            String profileName = args.getString(0);

            ProfileTypeName typeName = parseType(profileName);
            profileName = typeName.getName();
            ProfileScope scope = resolveScope(player, typeName.getType());

            Profile profile = manager.getProfile(scope, profileName);
            if (profile == null) {
                throw new CommandException("A profile by that name doesn't exist.");
            }
            if (!manager.remProfile(scope, profile)) {
                throw new CommandException("That profile couldn't be deleted.");
            }
            sender.sendMessage(ChatColor.YELLOW + "Profile deleted!");
        }
    }
}
