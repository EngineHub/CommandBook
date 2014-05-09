package com.sk89q.commandbook.item;

import java.util.regex.Pattern;

/**
 * This class is provided as a utility for programmers who are not comfortable with creating
 * regex patterns to match their item name.
 *
 * The regex match created by the provided item name is a case insensitive exact match.
 */
public abstract class SimpleItemProvider implements ItemProvider {

    private final Pattern pattern;

    public SimpleItemProvider(String itemName) {
        pattern = Pattern.compile("^(?iu)" + itemName + "$");
    }

    @Override
    public Pattern getPattern() {
        return pattern;
    }
}
