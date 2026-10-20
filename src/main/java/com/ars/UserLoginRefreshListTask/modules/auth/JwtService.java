package com.ars.UserLoginRefreshListTask.modules.auth;

import com.ars.UserLoginRefreshListTask.modules.user.entities.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Servico JWT: gera e valida access token (1h) e refresh token (30h).
 *
 * Access token claims:  sub=email, name, email, role, type=access
 * Refresh token claims: sub=email, type=refresh (mais leve, sem dados pessoais)
 *
 * Segredo vem do .env (JWT_SECRET) - minimo 32 caracteres (HS256).
 */
@Service
public class JwtService {

    private static final String TYPE_CLAIM = "type";
    private static final String ACCESS_TYPE = "access";
    private static final String REFRESH_TYPE = "refresh";

    private final SecretKey key;
    private final long accessTtlMs;
    private final long refreshTtlMs;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-ttl-ms}") long accessTtlMs,
            @Value("${jwt.refresh-ttl-ms}") long refreshTtlMs) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException(
                    "jwt.secret deve ter no minimo 32 caracteres (gere um aleatorio e coloque no .env)");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTtlMs = accessTtlMs;
        this.refreshTtlMs = refreshTtlMs;
    }

    // ========== GERACAO ==========

    public String generateAccessToken(User user) {
        Date now = new Date();
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("name", user.getName())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .claim(TYPE_CLAIM, ACCESS_TYPE)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessTtlMs))
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(User user) {
        Date now = new Date();
        return Jwts.builder()
                .subject(user.getEmail())
                .claim(TYPE_CLAIM, REFRESH_TYPE)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + refreshTtlMs))
                .signWith(key)
                .compact();
    }

    // ========== VALIDACAO ==========

    /** Valida assinatura + expiracao. Retorna false se invalido (nao lanca excecao). */
    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isRefreshToken(String token) {
        return REFRESH_TYPE.equals(parse(token).get(TYPE_CLAIM, String.class));
    }

    public String extractEmail(String token) {
        return parse(token).getSubject();
    }

    public String extractRole(String token) {
        return parse(token).get("role", String.class);
    }

    // ========== INTERNAL ==========

    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
