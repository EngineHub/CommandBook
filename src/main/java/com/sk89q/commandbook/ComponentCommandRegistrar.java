package com.sk89q.commandbook;

import com.sk89q.worldedit.internal.command.CommandRegistrationHandler;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.util.formatting.text.TranslatableComponent;
import org.enginehub.piston.CommandManager;
import org.enginehub.piston.CommandManagerService;
import org.enginehub.piston.part.SubCommandPart;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class ComponentCommandRegistrar {
    private final PlatformCommandManager platform;
    private final CommandManagerService service;
    private final CommandRegistrationHandler registration;

    public ComponentCommandRegistrar(PlatformCommandManager platform, CommandManagerService service,
                                     CommandRegistrationHandler registration) {
        this.platform = platform;
        this.service = service;
        this.registration = registration;
    }

    public void registerAsSubCommand(String command, Collection<String> aliases, String description,
                                     CommandManager parentManager, BiConsumer<CommandManager, CommandRegistrationHandler> op) {
        parentManager.register(command, builder -> {
            builder.description(TextComponent.of(description));
            builder.aliases(aliases);

            CommandManager manager = service.newCommandManager();
            op.accept(manager, registration);

            builder.addPart(SubCommandPart.builder(TranslatableComponent.of("worldedit.argument.action"), TextComponent.of("Sub-command to run."))
                    .withCommands(manager.getAllCommands().collect(Collectors.toList()))
                    .required()
                    .build());
        });
    }

    public void registerAsSubCommand(String command, String description, CommandManager parentManager,
                                     BiConsumer<CommandManager, CommandRegistrationHandler> op) {
        registerAsSubCommand(command, new ArrayList<>(), description, parentManager, op);
    }

    public void registerTopLevelCommands(BiConsumer<CommandManager, CommandRegistrationHandler> op) {
        CommandManager componentManager = service.newCommandManager();
        op.accept(componentManager, registration);

        platform.registerCommandsWith(componentManager, CommandBook.inst());
    }
}
