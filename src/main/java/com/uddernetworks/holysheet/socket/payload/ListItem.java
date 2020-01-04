package com.uddernetworks.holysheet.socket.payload;

public class ListItem {
    private String name;
    private long size;
    private int sheets;
    private long date;
    private String id;
    private boolean selfOwned;
    private String owner;

    public ListItem(String name, long size, int sheets, long date, String id, boolean selfOwned, String owner) {
        this.name = name;
        this.size = size;
        this.sheets = sheets;
        this.date = date;
        this.id = id;
        this.selfOwned = selfOwned;
        this.owner = owner;
    }

    @Override
    public String toString() {
        return "ListItem{" +
                "name='" + name + '\'' +
                ", size=" + size +
                ", sheets=" + sheets +
                ", date=" + date +
                ", id='" + id + '\'' +
                ", selfOwned=" + selfOwned +
                ", owner='" + owner + '\'' +
                '}';
    }
}