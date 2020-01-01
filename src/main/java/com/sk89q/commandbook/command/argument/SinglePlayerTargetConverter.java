package com.sk89q.commandbook.command.argument;

import com.sk89q.commandbook.util.InputUtil;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.enginehub.piston.CommandManager;
import org.enginehub.piston.converter.ConversionResult;
import org.enginehub.piston.converter.FailedConversion;
import org.enginehub.piston.converter.SuccessfulConversion;
import org.enginehub.piston.inject.InjectedValueAccess;
import org.enginehub.piston.inject.Key;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SinglePlayerTargetConverter extends PlayerTargetConverter<SinglePlayerTarget>  {
    public static void register(CommandManager commandManager) {
        commandManager.registerConverter(Key.of(SinglePlayerTarget.class), new SinglePlayerTargetConverter());
    }

    @Override
    public Component describeAcceptableArguments() {
        return TextComponent.of("any single online player");
    }

    @Override
    public ConversionResult<SinglePlayerTarget> convert(String argument, InjectedValueAccess context) {
        Optional<CommandSender> optSender = context.injectedValue(Key.of(CommandSender.class));
        if (!optSender.isPresent()){
            return FailedConversion.from(new IllegalStateException("No command sender present"));
        }

        CommandSender sender = optSender.get();
        try {
            Player result = InputUtil.PlayerParser.matchSinglePlayer(sender, argument);
            return SuccessfulConversion.fromSingle(new SinglePlayerTarget(result));
        } catch (CommandException e) {
            return FailedConversion.from(new IllegalArgumentException(e.getMessage()));
        }
    }

    @Override
    public List<String> getSuggestions(String input) {
        List<String> suggestions = new ArrayList<>();

        addPlayerNameSuggestions(suggestions);

        return suggestions;
    }
}
