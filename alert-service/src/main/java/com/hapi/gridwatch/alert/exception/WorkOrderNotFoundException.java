package com.hapi.gridwatch.alert.exception;

public class WorkOrderNotFoundException extends RuntimeException{
    public WorkOrderNotFoundException(String message){
        super(message);
    }
}
