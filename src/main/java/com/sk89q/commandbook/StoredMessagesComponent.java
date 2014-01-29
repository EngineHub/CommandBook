/*
 * CommandBook
 * Copyright (C) 2011 sk89q <http://www.sk89q.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.commandbook;

import com.sk89q.commandbook.events.MOTDSendEvent;
import com.sk89q.commandbook.util.ChatUtil;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.HashMap;
import java.util.Map;

import static com.sk89q.commandbook.util.ChatUtil.replaceColorMacros;
import static com.sk89q.commandbook.util.ChatUtil.sendMessage;

@ComponentInformation(friendlyName = "Stored Messages", desc = "Handles stored messages, such as the MOTD and rules pages.")
public class StoredMessagesComponent extends BukkitComponent implements Listener {

    protected final Map<String, String> messages = new HashMap<String, String>();
    
    private LocalConfiguration config;
    
    @Override
    public void enable() {
        config = new LocalConfiguration();
        loadMessages();
        CommandBook.registerEvents(this);
        registerCommands(Commands.class);
    }
    
    @Override
    public void reload() {
        super.reload();
        loadMessages();
    }

    public void loadMessages() {
        configure(config);
        messages.put("motd", config.motd);
        messages.put("rules", config.rules);
    }

    /**
     * Get preprogrammed messages.
     *
     * @param id
     * @return may return null
     */
    public String getMessage(String id) {
        return messages.get(id);
    }

    private static class LocalConfiguration extends ConfigurationBase {
        @Setting("motd") public String motd = "Hello, `Y%name%`w.\n" +
                "The time now is `Y%time%`w and you're in world '%world%'.\n" +
                "`2This is the default CommandBook MOTD! Adjust it in config.yml.\n" +
                "`2See `bhttp://wiki.sk89q.com/wiki/CommandBook`2 for more configuration information and help!";
        @Setting("rules") public String rules = "- Be courteous and respect others.\n" +
                "- Don't use any tools to give you an unfair advantage.\n" +
                "`2This is the default CommandBook rules text! You can adjust it in config.yml.";
        
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Show the MOTD.
        String motd = getMessage("motd");

        if (motd != null && motd.length() > 0) {
            CommandBook.callEvent(new MOTDSendEvent(player));

            sendMessage(player,
                    replaceColorMacros(
                            ChatUtil.replaceMacros(
                                    player, motd)));
        }

    }

    public class Commands {
        @Command(aliases = {"motd"},
                usage = "", desc = "Show the message of the day",
                min = 0, max = 0)
        @CommandPermissions({"commandbook.motd"})
        public void motd(CommandContext args, CommandSender sender) throws CommandException {

            String motd = getMessage("motd");

            if (motd == null || motd.length() < 1) {
                sender.sendMessage(ChatColor.RED + "MOTD not configured in CommandBook yet!");
            } else {
                CommandBook.callEvent(new MOTDSendEvent(sender));

                sendMessage(sender,
                        replaceColorMacros(
                                ChatUtil.replaceMacros(
                                        sender, motd)));
            }
        }

        @Command(aliases = {"rules"},
                usage = "", desc = "Show the rules",
                min = 0, max = 0)
        @CommandPermissions({"commandbook.rules"})
        public void rules(CommandContext args, CommandSender sender) throws CommandException {

            String motd = getMessage("rules");

            if (motd == null || motd.length() < 1) {
                sender.sendMessage(ChatColor.RED + "Rules not configured in CommandBook yet!");
            } else {
                sendMessage(sender,
                        replaceColorMacros(
                                ChatUtil.replaceMacros(
                                        sender, motd)));
            }
        }
    }
}
