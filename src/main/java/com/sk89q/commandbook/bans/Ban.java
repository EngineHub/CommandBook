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
}
