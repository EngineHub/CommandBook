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

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * A simple migrator for configurations that moves values from one key to another.
 * Values do not have their types converted.
 */
public abstract class ConfigurationMigrator {
    protected final YAMLProcessor config;
    protected final File oldFile;

    protected ConfigurationMigrator(File configFile) {
        this(configFile, new YAMLProcessor(configFile, false));
        try {
            config.load();
        } catch (IOException ignore) {}
    }
    
    protected ConfigurationMigrator(File configFile, YAMLProcessor processor) {
        this.oldFile = configFile;
        this.config = processor;
    }
    
    protected abstract Map<String, String> getMigrationKeys();

    protected abstract boolean shouldMigrate();
    
    public String migrate() {
        if (!shouldMigrate()) {
            return null;
        }

        if (!oldFile.renameTo(new File(oldFile.getAbsolutePath() + ".old"))) {
            return "Unable to rename backup old configuration file!";
        }
        for (Map.Entry<String, String> entry : getMigrationKeys().entrySet()) {
            Object existing = config.getProperty(entry.getKey());
            config.removeProperty(entry.getKey());
            if (existing == null || entry.getValue() == null) {
                continue;
            }
            config.setProperty(entry.getValue().replaceAll("%", entry.getKey()), existing);
        }
        if (!config.save()) {
            return "Failed to save migrated configuration!";
        }
        return null;
    }

}
