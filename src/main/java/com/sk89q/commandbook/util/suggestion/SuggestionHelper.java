package com.sk89q.commandbook.util.suggestion;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.util.InputUtil;
import com.sk89q.minecraft.util.commands.CommandException;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;

public class SuggestionHelper {
    private SuggestionHelper() { }

    public static void trialAddPlayerSuggestion(List<String> suggestions, String trailText) {
        try {
            InputUtil.PlayerParser.matchPlayers(CommandBook.server().getConsoleSender(), trailText);
            suggestions.add(trailText);
        } catch (CommandException ignored) { }
    }

    public static void addPlayerNameSuggestions(List<String> suggestions) {
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
