package com.sk89q.commandbook.config;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.util.yaml.YAMLProcessor;

import java.io.InputStream;

/**
 * A simple YAMLProcessor that loads YAML files from the jar's defaults/ folder
 */
public class DefaultsFileYAMLProcessor extends YAMLProcessor {
    protected final String file;
    
    public DefaultsFileYAMLProcessor(String file, boolean writeDefaults) {
        super(null, writeDefaults);
        this.file = file;
    }

    @Override
    public InputStream getInputStream() {
        return CommandBook.inst().getClass().getResourceAsStream("/defaults/" + file);
    }
}
