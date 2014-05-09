package com.sk89q.commandbook.item;

public class ItemIdentifier {
    private String name, detail;

    public ItemIdentifier(String name, String detail) {
        this.name = name;
        this.detail = detail;
    }

    public String getName() {
        return name;
    }

    public String getDetail() {
        return detail;
    }
}
