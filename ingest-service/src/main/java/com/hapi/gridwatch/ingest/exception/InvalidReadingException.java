package com.hapi.gridwatch.ingest.exception;

public class InvalidReadingException extends RuntimeException{
    public InvalidReadingException(String message){
        super(message);
    }
}
