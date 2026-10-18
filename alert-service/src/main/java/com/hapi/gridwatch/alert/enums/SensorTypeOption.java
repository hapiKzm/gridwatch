package com.hapi.gridwatch.alert.enums;

public enum SensorTypeOption {
    TEMPERATURE( 90),
    LOAD( 100);

    public final int limit;
    SensorTypeOption(int limit) {
        this.limit = limit;
    }
}
