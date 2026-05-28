package ma.sgitu.g8.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * TEST-ONLY endpoint to generate valid JWT tokens for Postman cross-validation.
 * In production, tokens would be issued by the auth service (service-utilisateur).
 */
@RestController
@RequestMapping("/api/v1/auth")
public class TestTokenController {

    @Value("${jwt.secret:sgitu_g8_secret_key_2025_very_long_secret_for_analytics}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration;

    @GetMapping("/token")
    public Map<String, String> generateToken(
            @RequestParam(defaultValue = "test-user") String username,
            @RequestParam(defaultValue = "ROLE_USER") String role) {

        Key key = Keys.hmacShaKeyFor(jwtSecret.getBytes());

        String token = Jwts.builder()
                .setSubject(username)
                .claim("roles", List.of(role))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        return Map.of(
                "token", token,
                "type", "Bearer",
                "username", username,
                "role", role,
                "expiresIn", jwtExpiration + "ms",
                "usage", "Add header: Authorization: Bearer " + token
        );
    }
}
