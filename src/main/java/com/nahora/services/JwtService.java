package com.nahora.services;

import com.nahora.model.Usuario;
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
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("id", user.getId())
                .claim("nome", user.getNome())
                .issuedAt(new Date())
                .expiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    public String generateRefreshToken(Usuario user) {
        String refreshToken = UUID.randomUUID().toString();
        String redisKey = "refresh_token:" + user.getEmail();

        // Salva o Refresh Token no Redis com o TTL configurado
        redisTemplate.opsForValue().set(redisKey, refreshToken, refreshExpirationMs, TimeUnit.MILLISECONDS);

        return refreshToken;
    }
}