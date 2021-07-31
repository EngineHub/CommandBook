package com.sk89q.commandbook;

import com.sk89q.worldedit.internal.command.CommandRegistrationHandler;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.util.formatting.text.TranslatableComponent;
import org.enginehub.piston.CommandManager;
import org.enginehub.piston.CommandManagerService;
import org.enginehub.piston.converter.ArgumentConverter;
import org.enginehub.piston.gen.CommandRegistration;
import org.enginehub.piston.inject.Key;
import org.enginehub.piston.part.SubCommandPart;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;
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

    public class Registrar {
        private final CommandManager commandManager;

        protected Registrar(CommandManager commandManager) {
            this.commandManager = commandManager;
        }

        public <CI> void register(CommandRegistration<CI> registrationContent, CI instance) {
            registration.register(commandManager, registrationContent, instance);
        }

        public <T> void registerConverter(Key<T> argumentKey, ArgumentConverter<T> converter) {
            commandManager.registerConverter(argumentKey, converter);
        }

        public void registerAsSubCommand(String command, Collection<String> aliases, String description,
                                         Consumer<Registrar> op) {
            commandManager.register(command, builder -> {
                builder.description(TextComponent.of(description));
                builder.aliases(aliases);

                CommandManager manager = service.newCommandManager();
                op.accept(new Registrar(manager));

                builder.addPart(SubCommandPart.builder(TranslatableComponent.of("worldedit.argument.action"), TextComponent.of("Sub-command to run."))
                    .withCommands(manager.getAllCommands().collect(Collectors.toList()))
                    .required()
                    .build());
            });
        }

        public void registerAsSubCommand(String command, String description, Consumer<Registrar> op) {
            registerAsSubCommand(command, new ArrayList<>(), description, op);
        }
    }

    public void registerTopLevelCommands(Consumer<Registrar> op) {
        CommandManager componentManager = service.newCommandManager();
        op.accept(new Registrar(componentManager));

        platform.registerCommandsWith(componentManager, CommandBook.inst());
    }
}
