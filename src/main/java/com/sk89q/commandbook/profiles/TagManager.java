package com.sk89q.commandbook.profiles;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface TagManager {
    public ProfileTag getTag(String tagName);
    public boolean loadTag(UUID player, ProfileTag tag);
    public List<ProfileTag> getActiveTags(UUID player);
    public Set<ProfileTag> clearTags(UUID player);
}
