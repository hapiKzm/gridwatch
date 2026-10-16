package com.hapi.gridwatch.alert.enums;

public enum SensorTypeOption {
    TEMPERATURE("TEMPERATURE", 90),
    LOAD("LOAD", 100);

    public final int limit;
    public final String name;
    SensorTypeOption(String name, int limit) {
        this.name = name;
        this.limit = limit;
    }

    public static SensorTypeOption findByName( String s) {
        for (SensorTypeOption t : SensorTypeOption.values())
            if (t.name.equals(s)) return t;

        return null;
    }
}
