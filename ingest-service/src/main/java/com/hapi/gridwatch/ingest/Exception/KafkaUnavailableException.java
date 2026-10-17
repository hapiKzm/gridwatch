package com.hapi.gridwatch.ingest.Exception;

public class KafkaUnavailableException extends RuntimeException {
    public KafkaUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}