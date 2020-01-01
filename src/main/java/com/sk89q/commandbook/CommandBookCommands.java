// $Id$
/*
 * Copyright (C) 2010, 2011 sk89q <http://www.sk89q.com>
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

import com.sk89q.commandbook.util.PaginatedResult;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.worldedit.command.util.CommandPermissions;
import com.sk89q.worldedit.command.util.CommandPermissionsConditionGenerator;
import com.sk89q.worldedit.internal.command.CommandRegistrationHandler;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.util.formatting.text.TranslatableComponent;
import com.sk89q.worldedit.util.formatting.text.adapter.bukkit.TextAdapter;
import com.sk89q.worldedit.util.formatting.text.format.TextColor;
import com.zachsthings.libcomponents.AbstractComponent;
import com.zachsthings.libcomponents.ComponentInformation;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.enginehub.piston.CommandManager;
import org.enginehub.piston.CommandManagerService;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.annotation.param.Arg;
import org.enginehub.piston.annotation.param.ArgFlag;
import org.enginehub.piston.part.SubCommandPart;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

@CommandContainer(superTypes = CommandPermissionsConditionGenerator.Registration.class)
public class CommandBookCommands {
    public static void register(CommandManagerService service, CommandManager commandManager, CommandRegistrationHandler registration) {
        commandManager.register("cmdbook", builder -> {
            builder.description(TextComponent.of("Commandbook Commands"));

            CommandManager manager = service.newCommandManager();
            registration.register(
                    manager,
                    CommandBookCommandsRegistration.builder(),
                    new CommandBookCommands()
            );

            builder.addPart(SubCommandPart.builder(TranslatableComponent.of("worldedit.argument.action"), TextComponent.of("Sub-command to run."))
                    .withCommands(manager.getAllCommands().collect(Collectors.toList()))
                    .required()
                    .build());
        });
    }

    @Command(name = "version", desc = "CommandBook version information")
    public void versionCmd(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "CommandBook " + CommandBook.inst().getDescription().getVersion());
        sender.sendMessage(ChatColor.YELLOW + "http://www.sk89q.com");
    }

    @Command(name = "reload", desc = "Reload CommandBook's settings")
    @CommandPermissions({"commandbook.reload"})
    public void reloadCmd(CommandSender sender) {
        try {
            CommandBook.inst().getGlobalConfiguration().load();
        } catch (IOException e) {
            sender.sendMessage(ChatColor.RED + "Error reloading configuration: " + e.getMessage());
            sender.sendMessage(ChatColor.RED + "See console for details!");
            CommandBook.logger().log(Level.WARNING, "Error reloading configuration: " + e, e);
        }

        CommandBook.inst().loadConfiguration();
        CommandBook.inst().getComponentManager().reloadComponents();

        sender.sendMessage(ChatColor.YELLOW + "CommandBook's configuration has been reloaded.");
    }

    @Command(name = "save", desc = "Save CommandBook's settings")
    @CommandPermissions({"commandbook.save"})
    public void saveCmd(CommandSender sender) {
        CommandBook.inst().getGlobalConfiguration().save();

        sender.sendMessage(ChatColor.YELLOW + "CommandBook's configuration has been reloaded.");
    }
    @Command(name = "help", aliases = {"doc"}, desc = "Get documentation for a component")
    @CommandPermissions("commandbook.component.help")
    public void helpCmd(CommandSender sender,
                            @ArgFlag(name = 'p', desc = "Page of results to return", def = "1") int page,
                            @Arg(desc = "Component to disable", def = "") String componentName) {
        if (componentName == null) {
            try {
                new PaginatedResult<AbstractComponent>("Name - Description") {
                    @Override
                    public String format(AbstractComponent entry) {
                        return entry.getInformation().friendlyName() + " - " + entry.getInformation().desc();
                    }
                }.display(sender, CommandBook.inst().getComponentManager().getComponents(), page);
            } catch (CommandException ignored) { }
        } else {
            AbstractComponent component = CommandBook.inst().getComponentManager().getComponent(componentName);
            if (component == null) {
                TextAdapter.sendComponent(
                        sender,
                        TextComponent.of("No such component: " + componentName).color(TextColor.RED)
                );
                return;
            }

            final ComponentInformation info = component.getInformation();
            sender.sendMessage(ChatColor.YELLOW + info.friendlyName() + " - " + info.desc());
            if (info.authors().length > 0 && info.authors()[0].length() > 0) {
                sender.sendMessage(ChatColor.YELLOW + "Authors: " +
                        Arrays.toString(info.authors()).replaceAll("[(.*)]", "$1"));
            }
            Map<String, String> commands = component.getCommands();
            if (commands.size() > 0) {
                try {
                    new PaginatedResult<Map.Entry<String, String>>("    Command - Description") {
                        @Override
                        public String format(Map.Entry<String, String> entry) {
                            return "    /" + entry.getKey() + " " + entry.getValue();
                        }
                    }.display(sender, commands.entrySet(), page);
                } catch (CommandException ignored) { }
            } else {
                sender.sendMessage(ChatColor.YELLOW + "No commands");
            }
        }
    }
}
