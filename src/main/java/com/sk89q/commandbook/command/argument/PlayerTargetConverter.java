package com.sk89q.commandbook.command.argument;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.util.InputUtil;
import com.sk89q.minecraft.util.commands.CommandException;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.enginehub.piston.converter.ArgumentConverter;

import java.util.Collection;
import java.util.List;

public abstract class PlayerTargetConverter<T> implements ArgumentConverter<T> {
    protected void trialAddSuggestion(List<String> suggestions, String trailText) {
        try {
            InputUtil.PlayerParser.matchPlayers(CommandBook.server().getConsoleSender(), trailText);
            suggestions.add(trailText);
        } catch (CommandException ignored) { }
    }

    protected void addPlayerNameSuggestions(List<String> suggestions) {
        Collection<? extends Player> players = CommandBook.server().getOnlinePlayers();
        boolean useDisplayNames = CommandBook.inst().lookupWithDisplayNames;

        for (Player player : players) {
            String playerName = player.getName();
            String displayName = ChatColor.stripColor(player.getDisplayName());

            suggestions.add(playerName);
            if (useDisplayNames && !playerName.equals(displayName)) {
                suggestions.add(displayName);
            }
        }
    }
}
