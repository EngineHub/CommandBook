// $Id$
/*
 * Copyright (C) 2010, 2011 sk89q <http://www.sk89q.com>
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

package com.sk89q.commandbook.component.session;

import com.sk89q.commandbook.util.entity.player.UUIDUtil;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import org.bukkit.command.CommandSender;

import java.util.concurrent.TimeUnit;

public abstract class PersistentSession extends ConfigurationBase {
    public static final long THIRTY_MINUTES = TimeUnit.MINUTES.toMillis(30);
    public static final long ONE_HOUR = TimeUnit.HOURS.toMillis(1);

    private final long maxTime;
    @Setting("last-update") private long lastUpdate;
    private CommandSender sender;
    @Deprecated private String senderName;
    private String uniqueName;

    protected PersistentSession(long maxTime) {
        this.maxTime = maxTime;
    }

    public long getGoneTime() {
        return (System.currentTimeMillis() - lastUpdate);
    }

    public boolean isRecent() {
        return maxTime == -1 || getGoneTime() < maxTime;
    }

    public CommandSender getOwner() {
        return sender;
    }

    @Deprecated
    public String getSenderName() {
        return senderName;
    }

    public String getUniqueName() {
        return uniqueName;
    }

    public void handleDisconnect() {
        lastUpdate = System.currentTimeMillis();
        sender = null;
    }

    public void handleReconnect(CommandSender sender) {
        if (sender == null) {
            throw new IllegalArgumentException("sender must not be null!");
        }
        this.sender = sender;
        this.senderName = sender.getName();
        this.uniqueName = UUIDUtil.toUniqueString(sender);
    }

}
