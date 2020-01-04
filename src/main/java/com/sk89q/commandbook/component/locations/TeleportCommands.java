package com.sk89q.commandbook.component.locations;

import com.google.common.collect.Lists;
import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.command.argument.MultiPlayerTarget;
import com.sk89q.commandbook.command.argument.SinglePlayerTarget;
import com.sk89q.commandbook.util.ChatUtil;
import com.sk89q.commandbook.util.InputUtil;
import com.sk89q.commandbook.util.entity.player.PlayerUtil;
import com.sk89q.commandbook.util.entity.player.iterators.TeleportPlayerIterator;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.worldedit.command.util.CommandPermissions;
import com.sk89q.worldedit.command.util.CommandPermissionsConditionGenerator;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.annotation.param.Arg;
import org.enginehub.piston.annotation.param.Switch;

@CommandContainer(superTypes = CommandPermissionsConditionGenerator.Registration.class)
public class TeleportCommands {
    private TeleportComponent component;

    public TeleportCommands(TeleportComponent component) {
        this.component = component;
    }

    @Command(name = "teleport", aliases = {"tp"}, desc = "Teleport to a location")
    @CommandPermissions({"commandbook.teleport"})
    public void teleport(CommandSender sender,
                         @Switch(name = 's', desc = "silent") boolean silent,
                         @Arg(desc = "players to teleport", def = "") MultiPlayerTarget targets,
                         @Arg(desc = "destination") LocationTarget destination) throws CommandException {
        if (targets == null) {
            targets = new MultiPlayerTarget(PlayerUtil.checkPlayer(sender));
        }

        Location loc = destination.get();

        boolean hasTeleOtherCurrent = CommandBook.inst().hasPermission(sender, "commandbook.teleport.other");
        boolean hasTeleOtherTo = CommandBook.inst().hasPermission(sender, loc.getWorld(), "commandbook.teleport.other");

        for (Player target : targets) {
            if (target != sender) {
                // If any of the targets is not the sender, we need to check .other
                // we must check for the from (target's world), current (sender's world),
                // and to (target location's world).
                if (!loc.getWorld().equals(target.getWorld())) {
                    CommandBook.inst().checkPermission(sender, target.getWorld(), "commandbook.teleport.other");
                }

                // If either check has failed, check both to get the proper exception response
                if (!hasTeleOtherCurrent || !hasTeleOtherTo) {
                    CommandBook.inst().checkPermission(sender, "commandbook.teleport.other");
                    CommandBook.inst().checkPermission(sender, loc.getWorld(), "commandbook.teleport.other");
                }
            } else {
                // If the target is the sender, just check the to (target location's world)
                // the current has already been checked
                CommandBook.inst().checkPermission(sender, loc.getWorld(), "commandbook.teleport");
            }
        }

        (new TeleportPlayerIterator(sender, destination, silent)).iterate(targets);
    }

    @Command(name = "call", aliases = {"tpa"}, desc = "Request a teleport")
    @CommandPermissions({"commandbook.call"})
    public void requestTeleport(Player sendingPlayer,
                                @Arg(desc = "player to teleport to") SinglePlayerTarget target) throws CommandException {
        CommandBook.inst().checkPermission(sendingPlayer, target.get().getWorld(), "commandbook.call");

        component.getSession(sendingPlayer).checkLastTeleportRequest(target.get());
        component.getSession(target.get()).addBringable(sendingPlayer);

        String senderMessage = ChatUtil.replaceColorMacros(
                ChatUtil.replaceMacros(sendingPlayer, component.getConfig().callMessageSender))
                .replaceAll("%ctarget%", ChatUtil.toColoredName(target.get(), null))
                .replaceAll("%target%", ChatUtil.toName(target.get()));
        String targetMessage = ChatUtil.replaceColorMacros(
                ChatUtil.replaceMacros(sendingPlayer, component.getConfig().callMessageTarget))
                .replaceAll("%ctarget%", ChatUtil.toColoredName(target.get(), null))
                .replaceAll("%target%", ChatUtil.toName(target.get()));
        sendingPlayer.sendMessage(senderMessage);
        target.get().sendMessage(targetMessage);
    }

    @Command(name = "bring", aliases = {"tphere", "grab", "g"}, desc = "Bring a player to you")
    public void bring(CommandSender sender,
                      @Arg(desc = "players to teleport") MultiPlayerTarget targets) throws CommandException {
        Player player = PlayerUtil.checkPlayer(sender);

        // If we have a single player match, check that for bringable. If we do not,
        // then check to see if the player can bring multiple players in his current
        // world. If that is the case then allow this to continue.
        if (targets.size() == 1) {
            Player target = targets.iterator().next();
            if (component.getSession(player).isBringable(target)) {
                final String senderMessage = ChatUtil.replaceColorMacros(
                        ChatUtil.replaceMacros(sender, component.getConfig().bringMessageSender))
                        .replaceAll("%ctarget%", ChatUtil.toColoredName(target, null))
                        .replaceAll("%target%", target.getName());
                final String targetMessage = ChatUtil.replaceColorMacros(
                        ChatUtil.replaceMacros(sender, component.getConfig().bringMessageTarget))
                        .replaceAll("%ctarget%", ChatUtil.toColoredName(target, null))
                        .replaceAll("%target%", target.getName());

                (new TeleportPlayerIterator(sender, player.getLocation()) {
                    @Override
                    public void onVictim(CommandSender sender, Player player) {
                        player.sendMessage(targetMessage);
                    }

                    @Override
                    public void onInformMany(CommandSender sender, int affected) {
                        sender.sendMessage(senderMessage);
                    }
                }).iterate(Lists.newArrayList(target));
                return;
            } else if (!CommandBook.inst().hasPermission(sender, "commandbook.teleport.other")) {
                // There was a single player match, but, the target was not bringable, and the player
                // does not have permission to teleport players in his/her current world.
                throw new CommandException(component.getConfig().bringMessageNoPerm);
            }
        }

        Location loc = player.getLocation();

        // There was not a single player match, or that single player match did not request
        // to be brought. The player does have permission to teleport players in his/her
        // current world. However, we're now teleporting in targets from potentially different worlds,
        // and we should ensure that the sender has permission to teleport players in those worlds.
        for (Player aTarget : targets) {
            // We have already checked the from and current locations, we must now check the to if the world does not match
            if (!loc.getWorld().equals(aTarget.getWorld())) {
                CommandBook.inst().checkPermission(sender, aTarget.getWorld(), "commandbook.teleport.other");
            }
        }

        (new TeleportPlayerIterator(sender, loc)).iterate(targets);
    }

    @Command(name = "put", aliases = {"place"}, desc = "Put a player at where you are looking")
    @CommandPermissions({"commandbook.teleport.other"})
    public void put(CommandSender sender,
                    @Arg(desc = "players to teleport") MultiPlayerTarget targets) throws CommandException {
        Location loc = InputUtil.LocationParser.matchLocation(sender, "#target");

        for (Player target : targets) {
            // We have already checked the from and current locations, we must now check the to if the world does not match
            if (!loc.getWorld().equals(target.getWorld())) {
                CommandBook.inst().checkPermission(sender, target.getWorld(), "commandbook.teleport.other");
            }
        }

        (new TeleportPlayerIterator(sender, loc)).iterate(targets);
    }

    @Command(name = "return", aliases = {"ret"}, desc = "Teleport back to your last location")
    @CommandPermissions({"commandbook.return"})
    public void ret(Player sendingPlayer,
                    @Arg(desc = "players to teleport", def = "") SinglePlayerTarget player) throws CommandException {
        if (player == null) {
            player = new SinglePlayerTarget(sendingPlayer);
        }

        if (player.get() != sendingPlayer) {
            CommandBook.inst().checkPermission(sendingPlayer, "commandbook.return.other");
        }

        Location lastLoc = component.getSession(player.get()).popLastLocation();

        if (lastLoc != null) {
            component.getSession(player.get()).setIgnoreLocation(lastLoc);
            lastLoc.getChunk().load(true);
            (new TeleportPlayerIterator(sendingPlayer, lastLoc) {
                @Override
                public void onCaller(Player player) {
                    sender.sendMessage(ChatColor.YELLOW + "You've been returned.");
                }

                @Override
                public void onVictim(CommandSender sender, Player player) {
                    player.sendMessage(ChatColor.YELLOW + "You've been returned by "
                            + ChatUtil.toColoredName(sender, ChatColor.YELLOW) + ".");
                }

                @Override
                public void onInformMany(CommandSender sender, int affected) {
                    sender.sendMessage(ChatColor.YELLOW.toString() + affected + " returned.");
                }
            }).iterate(player);
        } else {
            sendingPlayer.sendMessage(ChatColor.RED + "There's no past location in your history.");
        }
    }
}
