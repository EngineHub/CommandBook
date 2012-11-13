package com.sk89q.commandbook.util;

import org.bukkit.World;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.zachsthings.libcomponents.bukkit.BasePlugin;

/**
 * Backwards compatibility.
 */
public class LegacyBukkitCompat {

    private LegacyBukkitCompat() {
    }

    /**
     * Try to extract the world of a command sender.
     *
     * @param sender command sender
     * @return world or null
     */
    public static World extractWorld(CommandSender sender) {
        if (sender instanceof Player) {
            return ((Player) sender).getWorld();
        } else if (sender instanceof BlockCommandSender) {
            return ((BlockCommandSender) sender).getBlock().getWorld();
        } else {
            return BasePlugin.server().getWorlds().get(0);
        }
    }

}
