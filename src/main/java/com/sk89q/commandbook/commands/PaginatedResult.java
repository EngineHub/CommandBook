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

package com.sk89q.commandbook.commands;

import com.sk89q.minecraft.util.commands.CommandException;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Commands that wish to display a paginated list of results can use this class to do
 * the actual pagination, giving a list of items, a page number, and basic formatting information.
 */
public abstract class PaginatedResult<T> {
    private final String header;

    protected static final int PER_PAGE = 9;
    
    public PaginatedResult(String header) {
        this.header = header;
    }
    
    public void display(CommandSender sender, Collection<? extends T> results, int page) throws CommandException {
        display(sender, new ArrayList<T>(results), page);
    }
    
    public void display(CommandSender sender, List<? extends T> results, int page) throws CommandException {
        if (results.size() == 0) throw new CommandException("No results match!");
        --page;

        int maxPages = results.size() / PER_PAGE;

        // If the content divides perfectly, eg (18 entries, and 9 per page)
        // we end up with a blank page this handles this case
        if (results.size() % PER_PAGE == 0) {
            maxPages--;
        }

        page = Math.max(0, Math.min(page, maxPages));

        sender.sendMessage(ChatColor.YELLOW + header + " - Page (" + (page + 1) + "/" + (maxPages + 1) + ")");
        for (int i = PER_PAGE * page; i < PER_PAGE * page + PER_PAGE  && i < results.size(); i++) {
            sender.sendMessage(ChatColor.YELLOW.toString() + format(results.get(i)));
        }
    }
    
    public abstract String format(T entry);

}
