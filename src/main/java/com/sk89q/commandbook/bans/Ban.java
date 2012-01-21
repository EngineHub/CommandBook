package com.sk89q.commandbook.bans;

/**
 * @author zml2008
 */
public class Ban {
    private String name, address, reason;
    private long start, end;

    public Ban(String name, String address, String reason, long start, long end) {
        this.name = name;
        this.address = address;
        this.reason = reason;
        this.start = start;
        this.end = end;
    }

    public String getName() {
        return name;
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
        if (a == null && b == null) {
            return true;
        } else if (a == null || b == null) {
            return false;
        } else {
            return a.equals(b);
        }
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (address != null ? address.hashCode() : 0);
        return result;
    }
}
