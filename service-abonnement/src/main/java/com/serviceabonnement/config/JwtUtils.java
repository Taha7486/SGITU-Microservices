package com.serviceabonnement.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.Map;

@Component
public class JwtUtils {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String extractUsername(String token) {
        return (String) extractAllClaims(token).get("sub");
    }

    @SuppressWarnings("unchecked")
    public java.util.List<String> extractRoles(String token) {
        Object roles = extractAllClaims(token).get("roles");
        if (roles instanceof java.util.List) {
            return (java.util.List<String>) roles;
        }
        return java.util.Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractAllClaims(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                throw new IllegalArgumentException("Invalid JWT token format");
            }
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            return objectMapper.readValue(payload, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decode JWT payload", e);
        }
    }

    public boolean isTokenValid(String token) {
        // En mode "total trust", on vérifie juste que le format est correct
        try {
            extractAllClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
