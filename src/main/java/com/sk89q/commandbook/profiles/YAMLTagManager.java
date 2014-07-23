package com.sk89q.commandbook.profiles;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class YAMLTagManager implements TagManager {
    @Override
    public ProfileTag getTag(String tagName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean loadTag(UUID player, ProfileTag tag) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ProfileTag> getActiveTags(UUID player) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<ProfileTag> clearTags(UUID player) {
        throw new UnsupportedOperationException();
    }
}
