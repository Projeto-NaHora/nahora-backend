package com.nahora.services;

import com.nahora.model.Admin;
import com.nahora.model.Cliente;
import com.nahora.model.Profissional;
import com.nahora.model.Usuario;
import com.nahora.model.enums.TipoUsuario;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms}")
    private long jwtExpirationMs;

    @Value("${jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    private final StringRedisTemplate redisTemplate;

    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(Usuario user) {
        TipoUsuario tipo = resolverTipo(user);
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("id", user.getId())
                .claim("nome", user.getNome())
                .claim("tipoUsuario", tipo.name())
                .issuedAt(new Date())
                .expiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    public TipoUsuario extractTipoUsuario(Usuario user) {
        return resolverTipo(user);
    }

    private TipoUsuario resolverTipo(Usuario user) {
        if (user instanceof Admin) return TipoUsuario.ADMIN;
        if (user instanceof Profissional) return TipoUsuario.PROFISSIONAL;
        return TipoUsuario.CLIENTE;
    }

    public String extractEmail(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String generateRefreshToken(Usuario user) {
        String token = UUID.randomUUID().toString();

        redisTemplate.opsForValue().set("refresh_token:" + user.getEmail(), token, refreshExpirationMs, TimeUnit.MILLISECONDS);
        redisTemplate.opsForValue().set("refresh_token_lookup:" + token, user.getEmail(), refreshExpirationMs, TimeUnit.MILLISECONDS);

        return token;
    }

    /** Valida o token e retorna o e-mail do dono, ou null se inválido/expirado. */
    public String validateRefreshToken(String token) {
        String email = redisTemplate.opsForValue().get("refresh_token_lookup:" + token);
        if (email == null) return null;

        String stored = redisTemplate.opsForValue().get("refresh_token:" + email);
        return token.equals(stored) ? email : null;
    }

    public void deleteRefreshToken(String email) {
        String token = redisTemplate.opsForValue().get("refresh_token:" + email);
        if (token != null) {
            redisTemplate.delete("refresh_token_lookup:" + token);
        }
        redisTemplate.delete("refresh_token:" + email);
    }
}