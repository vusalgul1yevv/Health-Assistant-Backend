package bda.cypher.healthAssistant.security;

import bda.cypher.healthAssistant.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {
    private final Key signingKey;
    private final long expirationMs;
    private final String issuer;
    private final String audience;
    private final long clockSkewSeconds;

    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.expiration-ms}") long expirationMs,
                      @Value("${jwt.issuer}") String issuer,
                      @Value("${jwt.audience}") String audience,
                      @Value("${jwt.clock-skew-seconds:60}") long clockSkewSeconds) {
        this.signingKey = buildSigningKey(secret);
        this.expirationMs = expirationMs;
        this.issuer = issuer;
        this.audience = audience;
        this.clockSkewSeconds = clockSkewSeconds;
    }

    public String generateToken(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .setSubject(user.getEmail())
                .claim("role", user.getRole())
                .setIssuer(issuer)
                .setAudience(audience)
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(now)
                .setNotBefore(now)
                .setExpiration(expiry)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String extractRole(String token) {
        Object role = extractAllClaims(token).get("role");
        return role == null ? null : role.toString();
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception ex) {
            return false;
        }
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .requireIssuer(issuer)
                .requireAudience(audience)
                .setAllowedClockSkewSeconds(clockSkewSeconds)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key buildSigningKey(String secret) {
        byte[] keyBytes = resolveSecretBytes(secret);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT secret is too short. Use at least 32 bytes.");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private byte[] resolveSecretBytes(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT secret is missing.");
        }
        if (secret.startsWith("base64:")) {
            String base64 = secret.substring("base64:".length());
            return Base64.getDecoder().decode(base64);
        }
        return secret.getBytes(StandardCharsets.UTF_8);
    }
}
