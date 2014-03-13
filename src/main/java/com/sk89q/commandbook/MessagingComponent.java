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

import com.sk89q.commandbook.events.CommandSenderMessageEvent;
import com.sk89q.commandbook.events.SharedMessageEvent;
import com.sk89q.commandbook.session.AdministrativeSession;
import com.sk89q.commandbook.session.SessionComponent;
import com.sk89q.commandbook.session.UserSession;
import com.sk89q.commandbook.util.ChatUtil;
import com.sk89q.commandbook.util.InputUtil;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BasePlugin;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.ConfigurationNode;
import com.zachsthings.libcomponents.config.Setting;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Arrays;
import java.util.HashSet;

import static com.sk89q.commandbook.util.ChatUtil.replaceColorMacros;

@ComponentInformation(friendlyName = "Messaging", desc = "Commands that involve direct player <-> player or player <-> admin" +
        "communication are handled through this component.")
@Depend(components = SessionComponent.class)
public class MessagingComponent extends BukkitComponent implements Listener {

    @InjectComponent private SessionComponent sessions;

    private LocalConfiguration config;

    @Override
    public void enable() {
        config = configure(new LocalConfiguration());
        registerCommands(Commands.class);
        CommandBook.registerEvents(this);
    }

    @Override
    public void reload() {
        super.reload();
        configure(config);
    }

    private static class LocalConfiguration extends ConfigurationBase {
        @Setting("console-say-format") public String consoleSayFormat = "<`r*Console`w> %s";
        @Setting("broadcast-format") public String broadcastFormat = "`r[Broadcast] %s";
        @Setting("pm-color") private String pmColorString = "GRAY";
        @Setting("pm-text-color") private String pmTextColorString = "RESET";
        public ChatColor pmColor = ChatColor.GRAY; // Color for PM label
        public ChatColor pmTextColor = ChatColor.RESET; // Color for text of PM
        @Setting("twitter-style") public boolean twitterStyle = true;

        @Override
        public void load(ConfigurationNode node) {
            super.load(node);
            boolean error = false;
            try {
                pmColor = ChatColor.valueOf(pmColorString);
            } catch (IllegalArgumentException e) {
                CommandBook.logger().warning("Unknown PM Color  '" + pmColorString + "'! Resetting to GRAY");
                pmColor = ChatColor.GRAY;
                pmColorString = "GRAY";
                error = true;
            }
            try {
                pmTextColor = ChatColor.valueOf(pmTextColorString);
            } catch (IllegalArgumentException e) {
                CommandBook.logger().warning("Unknown PM Color  '" + pmTextColorString + "'! Resetting to GRAY");
                pmTextColor = ChatColor.GRAY;
                pmTextColorString = "GRAY";
                error = true;
            }
            if (error) {
                save(node);
            }
        }
    }

    public void messagePlayer(CommandSender sender, String target, String message) throws CommandException {
        CommandSender receiver =
                InputUtil.PlayerParser.matchPlayerOrConsole(sender, target);
        UserSession receiverSession = sessions.getSession(UserSession.class, receiver);
        AFKComponent.AFKSession afkSession = sessions.getSession(AFKComponent.AFKSession.class, receiver);
        if (afkSession.isAFK()) {
            String status = afkSession.getIdleStatus();
            sender.sendMessage(config.pmColor + ChatUtil.toColoredName(receiver, config.pmColor) + " is afk. "
                    + "They might not see your message."
                    + (status.trim().length() == 0 ? "" : " (" + status + ")"));
        }

        receiver.sendMessage(config.pmColor + "(From "
                + ChatUtil.toColoredName(sender, config.pmColor) + "): "
                + config.pmTextColor + message);

        sender.sendMessage(config.pmColor + "(To "
                + ChatUtil.toColoredName(receiver, config.pmColor) + "): "
                + config.pmTextColor + message);

        CommandBook.logger().info("(PM) " + ChatUtil.toColoredName(sender, ChatColor.RESET) + " -> "
                + ChatUtil.toColoredName(receiver, ChatColor.RESET) + ": " + message);

        sessions.getSession(UserSession.class, sender).setLastRecipient(receiver);

        // If the receiver hasn't had any player talk to them yet or hasn't
        // send a message, then we add it to the receiver's last message target
        // so s/he can /reply easily
        receiverSession.setNewLastRecipient(sender);
    }

    /**
     * Called on player chat.
     *
     * @param event Relevant event details
     */
    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        if (sessions.getSession(AdministrativeSession.class, event.getPlayer()).isMute()) {
            event.getPlayer().sendMessage(ChatColor.RED + "You are muted.");
            event.setCancelled(true);
        } else if (event.getMessage().startsWith("@") && config.twitterStyle) {
            final String message = event.getMessage();
            int spaceIndex = message.indexOf(" ");
            if (spaceIndex > -1) {
                try {
                    String name = message.substring(1, spaceIndex);
                    if (name.length() <= 0) {
                        return;
                    }

                    messagePlayer(event.getPlayer(), name, message.substring(spaceIndex + 1));
                } catch (CommandException e) {
                    event.getPlayer().sendMessage(ChatColor.RED + e.getMessage());
                }
                event.setCancelled(true);
            }
        }
    }

    public class Commands {
        @Command(aliases = {"me"}, usage = "<message...>", desc = "Send an action message", min = 1, max = -1)
        @CommandPermissions({"commandbook.say.me"})
        public void me(CommandContext args, CommandSender sender) throws CommandException {
            if (sender instanceof Player && sessions.getSession(AdministrativeSession.class, sender).isMute()) {
                sender.sendMessage(ChatColor.RED + "You are muted.");
                return;
            }

            String name = ChatUtil.toColoredName(sender, ChatColor.YELLOW);
            String msg = args.getJoinedStrings(0);

            BasePlugin.callEvent(
                    new SharedMessageEvent(name + " " + msg));

            BasePlugin.server().broadcastMessage("* " + name + " " + msg);
        }

        @Command(aliases = {"say"}, usage = "<message...>", desc = "Send a message", min = 1, max = -1)
        @CommandPermissions({"commandbook.say"})
        public void say(CommandContext args, CommandSender sender) throws CommandException {
            if (sender instanceof Player && sessions.getSession(AdministrativeSession.class, (Player) sender).isMute()) {
                sender.sendMessage(ChatColor.RED + "You are muted.");
                return;
            }

            String msg = args.getJoinedStrings(0);

            if (sender instanceof Player) {
                if (BasePlugin.callEvent(
                        new AsyncPlayerChatEvent(false, (Player) sender, msg,
                                new HashSet<Player>(Arrays.asList(BasePlugin.server().getOnlinePlayers())))).isCancelled()) {
                    return;
                }
            }

            BasePlugin.callEvent(
                    new CommandSenderMessageEvent(sender, msg));

            if (sender instanceof Player) {
                BasePlugin.server().broadcastMessage(
                        "<" + ChatUtil.toColoredName(sender, ChatColor.RESET)
                                + "> " + args.getJoinedStrings(0));
            } else {
                BasePlugin.server().broadcastMessage(
                        replaceColorMacros(config.consoleSayFormat).replace(
                                "%s", args.getJoinedStrings(0)));
            }
        }

        @Command(aliases = {"msg", "message", "whisper", "pm", "tell", "w"}, usage = "<target> <message...>", desc = "Private message a user", min = 2, max = -1)
        @CommandPermissions({"commandbook.msg"})
        public void msg(CommandContext args, CommandSender sender) throws CommandException {
            // This will throw errors as needed
            messagePlayer(sender, args.getString(0), args.getJoinedStrings(1));
        }

        @Command(aliases = {"reply", "r"}, usage = "<message...>", desc = "Reply to last user", min = 1, max = -1)
        @CommandPermissions({"commandbook.msg"})
        public void reply(CommandContext args, CommandSender sender) throws CommandException {
            String lastRecipient = sessions.getSession(UserSession.class, sender).getLastRecipient();
            if (lastRecipient == null) {
                throw new CommandException("You haven't messaged anyone.");
            }

            messagePlayer(sender, lastRecipient, args.getJoinedStrings(0));
        }

        @Command(aliases = {"mute"}, usage = "<target>", desc = "Mute a player", flags = "o", min = 1, max = 1)
        @CommandPermissions({"commandbook.mute"})
        public void mute(CommandContext args, CommandSender sender) throws CommandException {

            Player player = InputUtil.PlayerParser.matchSinglePlayer(sender, args.getString(0));

            if (CommandBook.inst().hasPermission(player, "commandbook.mute.exempt")
                    && !(args.hasFlag('o')
                    && CommandBook.inst().hasPermission(sender, "commandbook.mute.exempt.override"))) {
                throw new CommandException("Player " + ChatUtil.toName(sender) + " is exempt from being muted!");
            }

            if (!sessions.getSession(AdministrativeSession.class, player).setMute(true)) {

                if (player != sender) {
                    player.sendMessage(ChatColor.YELLOW + "You've been muted by "
                            + ChatUtil.toColoredName(sender, ChatColor.YELLOW));
                }
                sender.sendMessage(ChatColor.YELLOW + "You've muted "
                        + ChatUtil.toColoredName(player, ChatColor.YELLOW));
            } else {
                throw new CommandException("Player " + ChatUtil.toName(player) + " is already muted!");
            }
    }

        @Command(aliases = {"unmute"}, usage = "<target>", desc = "Unmute a player", min = 1, max = 1)
        @CommandPermissions({"commandbook.mute"})
        public void unmute(CommandContext args, CommandSender sender) throws CommandException {
            Player player = InputUtil.PlayerParser.matchSinglePlayer(sender, args.getString(0));

            if (sessions.getSession(AdministrativeSession.class, player).setMute(false)) {
                if (player != sender) {
                    player.sendMessage(ChatColor.YELLOW + "You've been unmuted by "
                        + ChatUtil.toColoredName(sender, ChatColor.YELLOW));
                }
                sender.sendMessage(ChatColor.YELLOW + "You've unmuted "
                        + ChatUtil.toColoredName(player, ChatColor.YELLOW));
            } else {
                throw new CommandException("Player " + ChatUtil.toName(player) + " was not muted!");
            }
        }

        @Command(aliases = {"broadcast"}, usage = "<message...>", desc = "Broadcast a message", min = 1, max = -1)
        @CommandPermissions({"commandbook.broadcast"})
        public void broadcast(CommandContext args, CommandSender sender) throws CommandException {
            BasePlugin.server().broadcastMessage(
                    replaceColorMacros(config.broadcastFormat).replace(
                            "%s", args.getJoinedStrings(0)));
        }
    }
}
