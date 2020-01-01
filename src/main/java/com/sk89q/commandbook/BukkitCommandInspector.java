package com.sk89q.commandbook;

import com.sk89q.bukkit.util.CommandInspector;
import com.sk89q.worldedit.WorldEdit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.enginehub.piston.CommandManager;
import org.enginehub.piston.inject.InjectedValueStore;
import org.enginehub.piston.inject.Key;
import org.enginehub.piston.inject.MapBackedValueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.sk89q.worldedit.util.formatting.WorldEditText.reduceToText;

class BukkitCommandInspector implements CommandInspector {
    private static final Logger logger = LoggerFactory.getLogger(com.sk89q.commandbook.BukkitCommandInspector.class);
    private final CommandBook plugin;
    private final CommandManager dispatcher;

    BukkitCommandInspector(CommandBook plugin, CommandManager dispatcher) {
        checkNotNull(plugin);
        checkNotNull(dispatcher);
        this.plugin = plugin;
        this.dispatcher = dispatcher;
    }

    @Override
    public String getShortText(Command command) {
        Optional<org.enginehub.piston.Command> mapping = dispatcher.getCommand(command.getName());
        if (mapping.isPresent()) {
            return reduceToText(mapping.get().getDescription(), WorldEdit.getInstance().getConfiguration().defaultLocale);
        } else {
            logger.warn("BukkitCommandInspector doesn't know how about the command '" + command + "'");
            return "Help text not available";
        }
    }

    @Override
    public String getFullText(Command command) {
        Optional<org.enginehub.piston.Command> mapping = dispatcher.getCommand(command.getName());
        if (mapping.isPresent()) {
            return reduceToText(mapping.get().getFullHelp(), WorldEdit.getInstance().getConfiguration().defaultLocale);
        } else {
            logger.warn("BukkitCommandInspector doesn't know how about the command '" + command + "'");
            return "Help text not available";
        }
    }

    @Override
    public boolean testPermission(CommandSender sender, Command command) {
        Optional<org.enginehub.piston.Command> mapping = dispatcher.getCommand(command.getName());
        if (mapping.isPresent()) {
            InjectedValueStore store = MapBackedValueStore.create();
            store.injectValue(Key.of(CommandSender.class), context -> Optional.of(sender));
            return mapping.get().getCondition().satisfied(store);
        } else {
            logger.warn("BukkitCommandInspector doesn't know how about the command '" + command + "'");
            return false;
        }
    }
}
