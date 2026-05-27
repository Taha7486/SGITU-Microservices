package com.sgitu.userservice.controller;
import com.sgitu.userservice.dto.LoginRequestDTO;
import com.sgitu.userservice.dto.LoginResponseDTO;
import com.sgitu.userservice.entity.Role;
import com.sgitu.userservice.entity.User;
import com.sgitu.userservice.repository.UserRepository;
import com.sgitu.userservice.security.JwtService;
import com.sgitu.userservice.security.RedisTokenBlacklistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentification", description = "Connexion et emission de tokens JWT (G3 est l emetteur officiel)")
public class AuthController {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RedisTokenBlacklistService blacklistService;

    @Operation(summary = "Connexion utilisateur",
        description = "Valide les identifiants et retourne un JWT signe. G10 doit forwarder les requetes de login vers cet endpoint.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Authentification reussie, token JWT retourne"),
        @ApiResponse(responseCode = "401", description = "Email ou mot de passe incorrect"),
        @ApiResponse(responseCode = "403", description = "Compte desactive")
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Email ou mot de passe incorrect"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email ou mot de passe incorrect");
        }
        if (!user.getActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Compte desactive - contactez un administrateur");
        }
        List<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toList());
        String token = jwtService.generateToken(user.getId(), user.getEmail(), roles);
        return ResponseEntity.ok(LoginResponseDTO.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .roles(roles)
                .build());
    }

    @Operation(summary = "Déconnexion utilisateur",
        description = "Révoque le JWT courant et le stocke dans Redis jusqu'à expiration.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Déconnexion réussie"),
        @ApiResponse(responseCode = "400", description = "Token invalide ou absent")
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        System.out.println("LOGOUT EXECUTED");
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Authorization header manquant ou incorrect");
        }

        String token = authHeader.substring(7);
        long ttlSeconds = jwtService.getTokenTtlSeconds(token);
        if (ttlSeconds <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token invalide ou expiré");
        }

        blacklistService.revokeToken(token, Duration.ofSeconds(ttlSeconds));
        return ResponseEntity.noContent().build();
    }
}