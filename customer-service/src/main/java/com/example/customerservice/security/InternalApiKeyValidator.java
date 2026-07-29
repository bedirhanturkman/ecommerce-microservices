package com.example.customerservice.security;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class InternalApiKeyValidator {

    @Value("${internal.api-key}")
    private String configuredApiKey;

    @PostConstruct
    void validateConfiguration() {
        if (configuredApiKey == null || configuredApiKey.isBlank()) {
            throw new IllegalStateException(
                    "INTERNAL_API_KEY must be configured"
            );
        }
    }

    public void validate(String providedApiKey) {
        if (providedApiKey == null || !MessageDigest.isEqual(
                configuredApiKey.getBytes(StandardCharsets.UTF_8),
                providedApiKey.getBytes(StandardCharsets.UTF_8)
        )) {
            throw new AccessDeniedException("Invalid internal API key");
        }
    }
}
