package com.uddernetworks.holysheet.socket.jshell;

public class SerializedVariable {
    private String name;
    private final String type;
    private final Object object;

    public SerializedVariable(String name, Object object) {
        this.name = name;
        this.object = object;
        this.type = object == null ? null : object.getClass().getCanonicalName();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public Object getObject() {
        return object;
    }
}