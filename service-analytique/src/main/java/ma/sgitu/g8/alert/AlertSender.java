package ma.sgitu.g8.alert;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class AlertSender {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${g5.notification.url}")
    private String g5Url;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @CircuitBreaker(name = "g5AlertCircuitBreaker", fallbackMethod = "sendFallback")
    public void send(Map<String, Object> payload) {
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, buildHeaders());
        restTemplate.postForEntity(g5Url, request, Void.class);
        log.info("Alert sent to G5: {}", payload.get("eventType"));
    }

    public void sendFallback(Map<String, Object> payload, Throwable ex) {
        log.warn("G5 circuit breaker OPEN — alert dropped [{}]: {}",
                payload.get("eventType"), ex.getMessage());
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(createServiceToken());
        headers.set("X-Source-Group", "G8");
        return headers;
    }

    private String createServiceToken() {
        Key key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        Date expiry = new Date(now.getTime() + 7200_000L);
        return Jwts.builder()
                .setSubject("g8-analytics-service")
                .claim("sourceService", "G8_ANALYTICS")
                .claim("roles", List.of("ROLE_SERVICE", "ROLE_ADMIN"))
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}
