package com.hapi.gridwatch.alert.exception;

public class UnknownSensorTypeException extends RuntimeException{
    public UnknownSensorTypeException(String message){
        super(message);
    }
}
