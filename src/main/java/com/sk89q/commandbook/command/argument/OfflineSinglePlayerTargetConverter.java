package com.sk89q.commandbook.command.argument;

import com.sk89q.commandbook.util.InputUtil;
import com.sk89q.commandbook.util.suggestion.SuggestionHelper;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
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
import java.util.UUID;

public class OfflineSinglePlayerTargetConverter implements ArgumentConverter<OfflineSinglePlayerTarget> {
    public static void register(CommandManager commandManager) {
        commandManager.registerConverter(Key.of(OfflineSinglePlayerTarget.class), new OfflineSinglePlayerTargetConverter());
    }

    @Override
    public Component describeAcceptableArguments() {
        return TextComponent.of("any single online or offline player");
    }

    @Override
    public ConversionResult<OfflineSinglePlayerTarget> convert(String argument, InjectedValueAccess context) {
        Optional<CommandSender> optSender = context.injectedValue(Key.of(CommandSender.class));
        if (!optSender.isPresent()){
            return FailedConversion.from(new IllegalStateException("No command sender present"));
        }

        try {
            Player onlinePlayer = InputUtil.PlayerParser.matchSinglePlayer(optSender.get(), argument);
            return SuccessfulConversion.fromSingle(new OfflineSinglePlayerTarget(onlinePlayer));
        } catch (CommandException ignored) { }

        try {
            return SuccessfulConversion.fromSingle(new OfflineSinglePlayerTarget(
                    Bukkit.getOfflinePlayer(UUID.fromString(argument))
            ));
        } catch (IllegalArgumentException ignored) {
            for (OfflinePlayer offlinePlayer : Bukkit.getServer().getOfflinePlayers()) {
                String offlinePlayerName = offlinePlayer.getName();
                if (offlinePlayerName == null) {
                    continue;
                }

                if (offlinePlayerName.equalsIgnoreCase(argument)) {
                    return SuccessfulConversion.fromSingle(new OfflineSinglePlayerTarget(offlinePlayer));
                }
            }
            return FailedConversion.from(new IllegalArgumentException("No player previously on this server with that name. Use UUID."));
        }
    }

    @Override
    public List<String> getSuggestions(String argument, InjectedValueAccess context) {
        List<String> suggestions = new ArrayList<>();

        SuggestionHelper.addPlayerNameSuggestions(suggestions, argument);

        for (OfflinePlayer offlinePlayer : Bukkit.getServer().getOfflinePlayers()) {
            if (offlinePlayer.isOnline()) {
                continue;
            }

            String offlinePlayerName = offlinePlayer.getName();
            if (offlinePlayerName == null) {
                continue;
            }

            String upperCaseName = offlinePlayerName.toUpperCase();
            if (upperCaseName.contains(argument.toUpperCase())) {
                suggestions.add(offlinePlayerName);
            }
        }

        return suggestions;
    }
}
