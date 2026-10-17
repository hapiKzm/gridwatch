package com.hapi.gridwatch.ingest.enums;

public enum SensorTypeOption {
    TEMPERATURE("TEMPERATURE"),
    LOAD("LOAD");

    public final String name;
    SensorTypeOption(String name) {
        this.name = name;
    }

    public static SensorTypeOption findByName( String s) {
        for (SensorTypeOption t : SensorTypeOption.values())
            if (t.name.equals(s)) return t;
        return null;
    }
}
