package com.sk89q.commandbook.component.locations;

import com.sk89q.commandbook.ComponentCommandRegistrar;
import com.sk89q.commandbook.util.InputUtil;
import com.sk89q.commandbook.util.LocationUtil;
import com.sk89q.commandbook.util.suggestion.SuggestionHelper;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
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

public class LocationTargetConverter implements ArgumentConverter<LocationTarget> {
    public static void register(ComponentCommandRegistrar registrar) {
        registrar.registerConverter(Key.of(LocationTarget.class), new LocationTargetConverter());
    }

    public static void register(CommandManager commandManager) {
        commandManager.registerConverter(Key.of(LocationTarget.class), new LocationTargetConverter());
    }

    @Override
    public Component describeAcceptableArguments() {
        return TextComponent.of("any player, named destination, or coordinate");
    }

    private ConversionResult<LocationTarget> getInvalidFormatResult() {
        return FailedConversion.from(new IllegalArgumentException("Invalid location format."));
    }

    private String[] normalizedSplit(String input) {
        input = input.replaceAll("(\\s|,)+", " ");
        return input.split(" ");
    }

    @Override
    public ConversionResult<LocationTarget> convert(String argument, InjectedValueAccess context) {
        Optional<CommandSender> optSender = context.injectedValue(Key.of(CommandSender.class));
        if (!optSender.isPresent()){
            return FailedConversion.from(new IllegalStateException("No command sender present"));
        }

        CommandSender sender = optSender.get();


        Location loc;
        boolean[] relative = new boolean[] { false, false, false };
        boolean precise = false;

        String[] parts = normalizedSplit(argument);
        if (parts.length == 1) {
            try {
                loc = InputUtil.LocationParser.matchLocation(sender, parts[0]); // matches both #7 and #8
            } catch (CommandException e) {
                return FailedConversion.from(new IllegalArgumentException(e.getMessage()));
            }
        } else if (parts.length == 3 && sender.hasPermission("commandbook.locations.coords")) {
            String xArg = parts[0];
            String yArg = parts[1];
            String zArg = parts[2];

            if (xArg.startsWith("~")) relative[0] = true;
            if (yArg.startsWith("~")) relative[1] = true;
            if (zArg.startsWith("~")) relative[2] = true;

            if (relative[0] || relative[1] || relative[2]) {
                if (!sender.hasPermission("commandbook.locations.coords.relative")) {
                    return getInvalidFormatResult();
                }
            }

            double x = InputUtil.LocationParser.parseCoordinateValue(xArg);
            double y = InputUtil.LocationParser.parseCoordinateValue(yArg);
            double z = InputUtil.LocationParser.parseCoordinateValue(zArg);

            if (x != (int) x || y != (int) y || z != (int) z) {
                precise = true;
            }

            World world = LocationUtil.extractWorld(sender);

            loc = new Location(world, x, y, z);
        } else {
            return getInvalidFormatResult();
        }

        if (!precise) {
            if (loc.getX() == loc.getBlockX()) loc.add(0.5, 0, 0);
            if (loc.getZ() == loc.getBlockZ()) loc.add(0, 0, 0.5);
        }

        return SuccessfulConversion.fromSingle(new LocationTarget(loc, relative));
    }

    @Override
    public List<String> getSuggestions(String input) {
        List<String> suggestions = new ArrayList<>();

        if (normalizedSplit(input).length == 1) {
            SuggestionHelper.addPlayerNameSuggestions(suggestions, input);
        }

        return suggestions;
    }
}
