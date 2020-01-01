package com.sk89q.commandbook.component.bans;

import java.util.UUID;

/**
 * @author zml2008
 */
public class Ban {
    private final UUID ID;
    private String name;
    private final String address;
    private final String reason;
    private final long start;
    private final long end;

    public Ban(UUID ID, String name, String address, String reason, long start, long end) {
        this.ID = ID;
        this.name = name;
        this.address = address;
        this.reason = reason;
        this.start = start;
        this.end = end;
    }

    public UUID getID() {
        return ID;
    }

    public String getLastKnownAlias() {
        return name;
    }

    public void setLastAlias(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public String getReason() {
        return reason;
    }

    public long getStart() {
        return start;
    }

    public long getEnd() {
        return end;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Ban)) {
            return false;
        }
        Ban ban = (Ban) other;
        return potentialNullEquals(name, ban.name)
                && potentialNullEquals(address, ban.address);
    }

    public static boolean potentialNullEquals(Object a, Object b) {
        return (a == null && b == null)
                || a != null && b != null
                && a.equals(b);
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (address != null ? address.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("BAN[");
        if (ID != null) {
            builder.append("(UUID: ").append(ID).append(")");
        }
        if (name != null) {
            builder.append("(LAST NAME: ").append(name).append(")");
        }
        if (address != null) {
            builder.append("(ADDRESS: ").append(address).append(")");
        }
        if (reason != null) {
            builder.append("(REASON: ").append(reason).append(")");
        }
        if (start != 0) {
            builder.append("(START: ").append(start).append(")");
        }
        builder.append("(END: ");
        if (end != 0) {
            builder.append(end);
        } else {
            builder.append("NEVER");
        }
        builder.append(")");
        builder.append("]");
        return builder.toString();
    }
}
