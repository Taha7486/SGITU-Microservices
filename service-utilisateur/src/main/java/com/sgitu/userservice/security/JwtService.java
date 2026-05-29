package com.sgitu.userservice.security;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.List;
/**
 * Service de generation de tokens JWT.
 * G3 est l emetteur officiel des JWT pour tout le systeme SGITU.
 * G10 (API Gateway) se contente de valider la signature.
 */
@Component
public class JwtService {
    @Value("${jwt.secret}")
    private String jwtSecret;
    @Value("${jwt.expiration:86400}")
    private long expirationSeconds;
    public String generateToken(Long userId, String email, List<String> roles) {
        Key key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationSeconds * 1000L);
        return Jwts.builder()
                .setSubject(email)
                .claim("userId", userId)
                .claim("roles", roles)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Date getTokenExpiration(String token) {
        try {
            Key key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getExpiration();
        } catch (Exception e) {
            return null;
        }
    }

    public long getTokenTtlSeconds(String token) {
        Date expiration = getTokenExpiration(token);
        if (expiration == null) {
            return 0L;
        }
        long ttl = (expiration.getTime() - new Date().getTime()) / 1000L;
        return Math.max(ttl, 0L);
    }
}