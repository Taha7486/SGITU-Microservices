package ma.sgitu.g5.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.sgitu.g5.service.TracingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * JWTAuthenticationFilter - Filtre d'authentification JWT
 * Accepte les tokens JWT provenant de tous les groupes (G1-G10)
 * 
 * APPROCHE DE TRAÇABILITÉ:
 * 1. Accepte tous les tokens sans validation stricte de signature
 * 2. Extrait les informations de traçabilité (source group, userId, etc.)
 * 3. Envoie les infos à G10 pour validation asynchrone
 * 4. G3 (Users) reçoit un traitement spécial
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JWTAuthenticationFilter extends OncePerRequestFilter {

    @Value("${jwt.secret}")
    private String jwtSecret;

    private final TracingService tracingService;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String X_SOURCE_GROUP = "X-Source-Group";
    private static final String X_TRACE_ID = "X-Trace-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        String sourceGroup = request.getHeader(X_SOURCE_GROUP);
        String traceId = request.getHeader(X_TRACE_ID);

        // Générer un trace ID si non fourni
        if (traceId == null) {
            traceId = UUID.randomUUID().toString();
        }

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            // Pas de token JWT, on continue sans authentification
            log.warn("[JWT-TRACE] Trace ID: {} - Pas de token JWT fourni", traceId);
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        String userId = null;
        String tokenSourceService = null;
        List<String> roles = null;
        boolean tokenValid = false;

        try {
            // Tentative de parsing du token (sans validation stricte de signature)
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            userId = claims.getSubject();
            tokenSourceService = claims.get("sourceService", String.class);
            roles = claims.get("roles", List.class);
            tokenValid = true;

            log.info("[JWT-TRACE] Trace ID: {} - Token parsé avec succès - User: {}, Token Source: {}, Header Source: {}", 
                    traceId, userId, tokenSourceService, sourceGroup);

        } catch (MalformedJwtException e) {
            // Token mal formé mais on continue pour traçabilité
            log.warn("[JWT-TRACE] Trace ID: {} - Token mal formé: {}", traceId, e.getMessage());
        } catch (Exception e) {
            // Autre erreur mais on continue pour traçabilité
            log.warn("[JWT-TRACE] Trace ID: {} - Erreur parsing token: {}", traceId, e.getMessage());
        }

        // Traitement spécial pour G3 (Users)
        if (sourceGroup != null && sourceGroup.equals("G3")) {
            log.info("[JWT-TRACE] Trace ID: {} - Traitement spécial pour G3 (Users) - User: {}", traceId, userId);
        }

        // Envoyer les infos de traçabilité à G10 pour validation asynchrone
        tracingService.sendTracingInfo(traceId, token, sourceGroup, tokenSourceService, userId, roles, tokenValid);

        // Créer l'authentification basique (sans validation stricte)
        if (userId != null) {
            List<SimpleGrantedAuthority> authorities = roles != null
                    ? roles.stream().map(SimpleGrantedAuthority::new).toList()
                    : Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    authorities
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // Ajouter le trace ID aux headers pour traçabilité downstream
        response.setHeader(X_TRACE_ID, traceId);

        filterChain.doFilter(request, response);
    }
}
