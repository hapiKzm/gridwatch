package com.hapi.gridwatch.ingest.config;

import org.springframework.boot.context.properties.ConfigurationProperties;



@ConfigurationProperties(prefix = "alert-service")
public record AlertServiceProperties(String url, String username, String password) {}