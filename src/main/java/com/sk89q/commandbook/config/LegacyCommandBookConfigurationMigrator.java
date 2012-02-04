/*
 * CommandBook
 * Copyright (C) 2012 sk89q <http://www.sk89q.com>
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

package com.sk89q.commandbook.config;

import com.sk89q.util.yaml.YAMLProcessor;
import com.zachsthings.libcomponents.config.ConfigurationMigrator;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Converts pre-2.0 configurations to the 2.0 format
 */
public class LegacyCommandBookConfigurationMigrator extends ConfigurationMigrator {
    public LegacyCommandBookConfigurationMigrator(File configFile, YAMLProcessor processor) {
        super(configFile, processor);
    }
    
    private static final Map<String, String> migrationKeys = new LinkedHashMap<String, String>();
    static {
        migrationKeys.put("online-list", "component.%");
        migrationKeys.put("online-on-join", "component.online-list.list-on-join");
        migrationKeys.put("verify-name-format", null);
        migrationKeys.put("motd", "component.stored-messages.%");
        migrationKeys.put("rules", "component.stored-messages.%");
        migrationKeys.put("item-permissions-only", "component.inventory.%");
        migrationKeys.put("disallowed-items", "component.inventory.%");
        migrationKeys.put("allowed-items", "component.inventory.%");
        migrationKeys.put("default-item-stack-size", "component.inventory.%");
        migrationKeys.put("thor-hammer-items", "component.thor.hammer-items");
        migrationKeys.put("broadcast-bans", "component.bans.%");
        migrationKeys.put("broadcast-kicks", "component.bans.&");
        migrationKeys.put("console-say-format", "component.messaging.%");
        migrationKeys.put("broadcast-format", "component.messaging.%");
        migrationKeys.put("exact-spawn", "component.spawn-locations.%");
        migrationKeys.put("time-lock", "component.time-control.%");
        migrationKeys.put("time-lock-delay", "component.time-control.%");
        migrationKeys.put("per-world-warps", "component.warps.per-world");
        migrationKeys.put("per-world-homes", "component.homes.per-world");
    }

    @Override
    public Map<String, String> getMigrationKeys() {
        return migrationKeys;
    }

    @Override
    public boolean shouldMigrate() {
        return config.getProperty("online-on-join") != null;
    }
}
