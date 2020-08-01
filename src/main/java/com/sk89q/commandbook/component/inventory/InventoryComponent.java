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

package com.sk89q.commandbook.component.inventory;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.worldedit.blocks.BaseItem;
import com.sk89q.worldedit.world.item.ItemType;
import com.sk89q.worldedit.world.item.ItemTypes;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import org.bukkit.command.CommandSender;

@ComponentInformation(friendlyName = "Inventory",
        desc = "Inventory-related commands, such as /give and /clear, are handled in this component.")
public class InventoryComponent extends BukkitComponent {
    protected InventoryComponentConfiguration config;

    @Override
    public void enable() {
        config = configure(new InventoryComponentConfiguration());

        CommandBook.getComponentRegistrar().registerTopLevelCommands((commandManager, registration) -> {
            registration.register(commandManager, InventoryCommandsRegistration.builder(), new InventoryCommands(this));
        });
    }

    @Override
    public void reload() {
        super.reload();
        configure(config);
    }

    protected InventoryComponentConfiguration getConfig() {
        return config;
    }

    // -- Helper methods

    /**
     * Checks to see if a user can use an item.
     *
     * @param sender
     * @param itemStack
     * @throws CommandException
     */
    public void checkAllowedItem(CommandSender sender, BaseItem itemStack)
            throws CommandException {

        ItemType itemType = itemStack.getType();
        if (itemType == null || itemType == ItemTypes.AIR) {
            throw new CommandException("Non-existent item specified.");
        }

        // Check if the user has an override
        if (CommandBook.inst().hasPermission(sender, "commandbook.override.any-item")) {
            return;
        }

        String itemId = itemType.getId();
        String namespacedPermission = itemId.replace(":", ".");
        boolean hasPermissions = CommandBook.inst().hasPermission(sender, "commandbook.items." + namespacedPermission);

        // Also check the permissions system
        if (hasPermissions) {
            return;
        }

        if (config.useItemPermissionsOnly) {
            throw new CommandException("That item is not allowed.");
        }

        if (config.allowedItems.size() > 0) {
            if (!config.allowedItems.contains(itemId)) {
                throw new CommandException("That item is not allowed.");
            }
        }

        if (config.disallowedItems.contains(itemId)) {
            throw new CommandException("That item is disallowed.");
        }
    }
}
