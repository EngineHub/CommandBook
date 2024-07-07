package com.sk89q.commandbook;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.sk89q.bukkit.util.CommandInfo;
import com.sk89q.bukkit.util.CommandRegistration;
import com.sk89q.commandbook.command.CommandExceptionConverter;
import com.sk89q.commandbook.command.argument.MultiPlayerTargetConverter;
import com.sk89q.commandbook.command.argument.OfflineSinglePlayerTargetConverter;
import com.sk89q.commandbook.command.argument.SinglePlayerTargetConverter;
import com.sk89q.commandbook.util.WorldEditAdapter;
import com.sk89q.worldedit.command.argument.Arguments;
import com.sk89q.worldedit.command.util.PermissionCondition;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.internal.command.CommandArgParser;
import com.sk89q.worldedit.internal.command.CommandRegistrationHandler;
import com.sk89q.worldedit.internal.util.Substring;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.util.formatting.text.TranslatableComponent;
import com.sk89q.worldedit.util.formatting.text.format.TextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.enginehub.piston.Command;
import org.enginehub.piston.CommandManager;
import org.enginehub.piston.exception.CommandException;
import org.enginehub.piston.exception.ConditionFailedException;
import org.enginehub.piston.exception.UsageException;
import org.enginehub.piston.impl.CommandManagerServiceImpl;
import org.enginehub.piston.inject.*;
import org.enginehub.piston.suggestion.Suggestion;
import org.enginehub.piston.util.HelpGenerator;
import org.enginehub.piston.util.TextHelper;
import org.enginehub.piston.util.ValueProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PlatformCommandManager {
    public static final Pattern COMMAND_CLEAN_PATTERN = Pattern.compile("^[/]+");
    private static final Logger log = LoggerFactory.getLogger(PlatformCommandManager.class);
    private static final java.util.logging.Logger COMMAND_LOG =
            java.util.logging.Logger.getLogger("com.sk89q.commandbook.CommandLog");

    private final CommandManagerServiceImpl commandManagerService;
    private final CommandManager commandManager;
    private final InjectedValueStore globalInjectedValues;
    private final CommandRegistrationHandler registration;
    private final ComponentCommandRegistrar componentRegistrar;
    private final CommandExceptionConverter exceptionConverter = new CommandExceptionConverter();

    protected PlatformCommandManager() {
        this.commandManagerService = new CommandManagerServiceImpl();
        this.commandManager = commandManagerService.newCommandManager();
        this.globalInjectedValues = MapBackedValueStore.create();
        this.registration = new CommandRegistrationHandler(ImmutableList.of());
        this.componentRegistrar = new ComponentCommandRegistrar(this, commandManagerService, registration);

        // setup separate from main constructor
        // ensures that everything is definitely assigned
        initialize();
    }

    private void initialize() {
        // Set up the commands manager
        registerAlwaysInjectedValues();
        registerArgumentConverters();
    }

    private void registerAlwaysInjectedValues() {

    }

    private void registerArgumentConverters() {
        SinglePlayerTargetConverter.register(commandManager);
        MultiPlayerTargetConverter.register(commandManager);
        OfflineSinglePlayerTargetConverter.register(commandManager);
    }

    protected void registerCoreCommands(CommandBook commandBook) {
        CommandBookCommands.register(commandManagerService, commandManager, registration);

        // We want to register with bukkit only, as we're already registered with the global command
        // manager.
        registerCommandsWithBukkit(commandManager, commandBook);
    }

    private void registerCommandsWithBukkit(CommandManager commandManager, CommandBook commandBook) {
        BukkitCommandInspector inspector = new BukkitCommandInspector(commandBook, commandManager);

        CommandRegistration registration = new CommandRegistration(commandBook);
        registration.register(commandManager.getAllCommands()
                .map(command -> {
                    String[] permissionsArray = command.getCondition()
                            .as(PermissionCondition.class)
                            .map(PermissionCondition::getPermissions)
                            .map(s -> s.toArray(new String[0]))
                            .orElseGet(() -> new String[0]);

                    String[] aliases = Stream.concat(
                            Stream.of(command.getName()),
                            command.getAliases().stream()
                    ).toArray(String[]::new);
                    // TODO Handle localisation correctly
                    return new CommandInfo(
                        TextHelper.reduceToText(command.getUsage()),
                        TextHelper.reduceToText(command.getDescription()),
                        aliases,
                        inspector,
                        permissionsArray
                    );
                }).collect(Collectors.toList()));
    }

    public void registerCommandsWith(CommandManager componentCommandManager, CommandBook commandBook) {
        // Register the component command manager with piston
        commandManager.registerManager(componentCommandManager);

        // Register the component command manager with bukkit
        registerCommandsWithBukkit(componentCommandManager, commandBook);
    }

    public ComponentCommandRegistrar getComponentRegistrar() {
        return componentRegistrar;
    }

    private Stream<Substring> parseArgs(String input) {
        return CommandArgParser.forArgString(input.substring(1)).parseArgs();
    }

    private MemoizingValueAccess initializeInjectedValues(Arguments arguments, CommandSender actor) {
        InjectedValueStore store = MapBackedValueStore.create();

        store.injectValue(Key.of(Actor.class), ValueProvider.constant(WorldEditAdapter.adapt(actor)));
        store.injectValue(Key.of(CommandSender.class), ValueProvider.constant(actor));
        if (actor instanceof Player) {
            store.injectValue(Key.of(Player.class), ValueProvider.constant((Player) actor));
        } else {
            store.injectValue(Key.of(Player.class), context -> {
                throw new CommandException(
                        TextComponent.of("This command must be used with a player."),
                        ImmutableList.of()
                );
            });
        }

        store.injectValue(Key.of(Arguments.class), ValueProvider.constant(arguments));

        return MemoizingValueAccess.wrap(MergedValueAccess.of(store, globalInjectedValues));
    }

    private void handleUnknownException(Actor actor, Throwable t) {
        actor.printError(TranslatableComponent.of("worldedit.command.error.report"));
        actor.print(TextComponent.of(t.getClass().getName() + ": " + t.getMessage()));
        log.error("An unexpected error while handling a CommandBook command", t);
    }

    public void handleCommand(CommandSender sender, String arguments) {
        Actor actor = WorldEditAdapter.adapt(sender);
        String[] split = parseArgs(arguments).map(Substring::getSubstring).toArray(String[]::new);

        // No command found!
        if (!commandManager.containsCommand(split[0])) {
            return;
        }

        MemoizingValueAccess context = initializeInjectedValues(() -> arguments, sender);

        try {
            // This is a bit of a hack, since the call method can only throw CommandExceptions
            // everything needs to be wrapped at least once. Which means to handle all WorldEdit
            // exceptions without writing a hook into every dispatcher, we need to unwrap these
            // exceptions and rethrow their converted form, if their is one.
            try {
                commandManager.execute(context, ImmutableList.copyOf(split));
            } catch (Throwable t) {
                // Use the exception converter to convert the exception if any of its causes
                // can be converted, otherwise throw the original exception

                Throwable newT = exceptionConverter.convert(t);
                if (newT != null) {
                    throw newT;
                }

                throw t;
            }
        } catch (ConditionFailedException e) {
            if (e.getCondition() instanceof PermissionCondition) {
                actor.printError(TranslatableComponent.of("worldedit.command.permissions"));
            } else {
                actor.print(e.getRichMessage());
            }
        } catch (UsageException e) {
            actor.print(TextComponent.builder("")
                    .color(TextColor.RED)
                    .append(e.getRichMessage())
                    .build());
            ImmutableList<Command> cmd = e.getCommands();
            if (!cmd.isEmpty()) {
                actor.print(TextComponent.builder("Usage: ")
                        .color(TextColor.RED)
                        .append(HelpGenerator.create(e.getCommandParseResult()).getUsage())
                        .build());
            }
        } catch (CommandException e) {
            actor.print(TextComponent.builder("")
                    .color(TextColor.RED)
                    .append(e.getRichMessage())
                    .build());
        } catch (Throwable t) {
            handleUnknownException(actor, t);
        }
    }

    public List<Substring> handleCommandSuggestion(CommandSender sender, String arguments) {
        try {
            List<Substring> split = parseArgs(arguments).collect(Collectors.toList());
            List<String> argStrings = split.stream()
                    .map(Substring::getSubstring)
                    .collect(Collectors.toList());
            MemoizingValueAccess access = initializeInjectedValues(() -> arguments, sender);
            ImmutableSet<Suggestion> suggestions;
            try {
                suggestions = commandManager.getSuggestions(access, argStrings);
            } catch (Throwable t) { // catch errors which are *not* command exceptions generated by parsers/suggesters
                if (!(t instanceof CommandException)) {
                    log.debug("Unexpected error occurred while generating suggestions for input: " + arguments, t);
                    return Collections.emptyList();
                }
                throw t;
            }

            return suggestions.stream()
                    .map(suggestion -> {
                        int noSlashLength = arguments.length() - 1;
                        Substring original = suggestion.getReplacedArgument() == split.size()
                                ? Substring.from(arguments, noSlashLength, noSlashLength)
                                : split.get(suggestion.getReplacedArgument());
                        // increase original points by 1, for removed `/` in `parseArgs`
                        return Substring.wrap(
                                suggestion.getSuggestion(),
                                original.getStart() + 1,
                                original.getEnd() + 1
                        );
                    }).collect(Collectors.toList());
        } catch (ConditionFailedException ignored) { }

        return Collections.emptyList();
    }
}
