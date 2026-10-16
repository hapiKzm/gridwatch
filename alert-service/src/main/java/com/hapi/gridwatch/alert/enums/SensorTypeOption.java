package com.hapi.gridwatch.alert.enums;

import jakarta.validation.constraints.NotNull;

public enum SensorTypeOption {
    TEMPERATURE("TEMPERATURE", 90),
    LOAD("LOAD", 100);

    public final int limit;
    public final String name;
    SensorTypeOption(String name, int limit) {
        this.name = name;
        this.limit = limit;
    }
}
