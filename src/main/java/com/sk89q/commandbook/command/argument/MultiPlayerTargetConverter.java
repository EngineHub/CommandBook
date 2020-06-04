package com.sk89q.commandbook.command.argument;

import com.sk89q.commandbook.util.InputUtil;
import com.sk89q.commandbook.util.suggestion.SuggestionHelper;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.enginehub.piston.CommandManager;
import org.enginehub.piston.converter.ArgumentConverter;
import org.enginehub.piston.converter.ConversionResult;
import org.enginehub.piston.converter.FailedConversion;
import org.enginehub.piston.converter.SuccessfulConversion;
import org.enginehub.piston.inject.InjectedValueAccess;
import org.enginehub.piston.inject.Key;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MultiPlayerTargetConverter implements ArgumentConverter<MultiPlayerTarget> {
    public static void register(CommandManager commandManager) {
        commandManager.registerConverter(Key.of(MultiPlayerTarget.class), new MultiPlayerTargetConverter());
    }

    @Override
    public Component describeAcceptableArguments() {
        return TextComponent.of("any number of online players, or a player matching pattern");
    }

    @Override
    public ConversionResult<MultiPlayerTarget> convert(String argument, InjectedValueAccess context) {
        Optional<CommandSender> optSender = context.injectedValue(Key.of(CommandSender.class));
        if (!optSender.isPresent()) {
            return FailedConversion.from(new IllegalStateException("No command sender present"));
        }

        CommandSender sender = optSender.get();
        try {
            List<Player> results = InputUtil.PlayerParser.matchPlayers(sender, argument);
            return SuccessfulConversion.fromSingle(new MultiPlayerTarget(results));
        } catch (CommandException e) {
            return FailedConversion.from(new IllegalArgumentException(e.getMessage()));
        }
    }

    @Override
    public List<String> getSuggestions(String argument, InjectedValueAccess context) {
        List<String> suggestions = new ArrayList<>();

        Optional<CommandSender> optSender = context.injectedValue(Key.of(CommandSender.class));
        if (!optSender.isPresent()) {
            return suggestions;
        }

        CommandSender sender = optSender.get();
        if (argument.isEmpty() || argument.equals("*")) {
            SuggestionHelper.trialAddPlayerSuggestion(sender, suggestions, "*");
        }

        SuggestionHelper.trialAddPlayerSuggestion(sender, suggestions, argument + "*");
        SuggestionHelper.trialAddPlayerSuggestion(sender, suggestions, "*" + argument);

        if (argument.isEmpty() || argument.startsWith("#")) {
            SuggestionHelper.trialAddPlayerSuggestion(sender, suggestions, "#world");
            SuggestionHelper.trialAddPlayerSuggestion(sender, suggestions, "#near");
        }

        SuggestionHelper.addPlayerNameSuggestions(suggestions, argument);

        return suggestions;
    }
}