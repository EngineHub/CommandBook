package com.sk89q.commandbook.util;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.extension.platform.Actor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import static com.google.common.base.Preconditions.checkNotNull;

public class WorldEditAdapter {
    @Deprecated
    public static Actor adapt(CommandSender sender) {
        WorldEditPlugin worldEdit = (WorldEditPlugin) Bukkit.getPluginManager().getPlugin("WorldEdit");
        checkNotNull(worldEdit);

        return worldEdit.wrapCommandSender(sender);
    }
}
