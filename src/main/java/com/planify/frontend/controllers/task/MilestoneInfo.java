package com.planify.frontend.controllers.task;

public class MilestoneInfo {
    private final String uuid;
    private final String name;

    public MilestoneInfo(String uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public String getUuid() { return uuid; }
    public String getName() { return name; }

    @Override
    public String toString() {
        return name; // ensures ComboBox shows the name
    }
}
