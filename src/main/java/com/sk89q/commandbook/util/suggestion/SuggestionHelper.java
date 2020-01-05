package com.sk89q.commandbook.util.suggestion;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.util.InputUtil;
import com.sk89q.minecraft.util.commands.CommandException;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;

public class SuggestionHelper {
    private SuggestionHelper() { }

    public static void trialAddPlayerSuggestion(List<String> suggestions, String trailText, int minMatch) {
        try {
            CommandSender sender = CommandBook.server().getConsoleSender();
            int numMatched = InputUtil.PlayerParser.matchPlayers(sender, trailText).size();
            if (numMatched >= minMatch) {
                suggestions.add(trailText);
            }
        } catch (CommandException ignored) { }
    }

    public static void trialAddPlayerSuggestion(List<String> suggestions, String trialText) {
        trialAddPlayerSuggestion(suggestions, trialText, 2);
    }

    public static void addPlayerNameSuggestions(List<String> suggestions, String input) {
        input = input.toLowerCase();

        Collection<? extends Player> players = CommandBook.server().getOnlinePlayers();
        boolean useDisplayNames = CommandBook.inst().lookupWithDisplayNames;

        for (Player player : players) {
            String playerName = player.getName();

            String lowerPlayerName = playerName.toLowerCase();
            if (input.isEmpty() || lowerPlayerName.contains(input)) {
                suggestions.add(playerName);
            }

            String displayName = ChatColor.stripColor(player.getDisplayName());
            String lowerDisplayName = displayName.toLowerCase();
            if (useDisplayNames && !lowerPlayerName.equals(lowerDisplayName)) {
                if (input.isEmpty() || lowerDisplayName.contains(input)) {
                    suggestions.add(displayName);
                }
            }
        }
    }
}
