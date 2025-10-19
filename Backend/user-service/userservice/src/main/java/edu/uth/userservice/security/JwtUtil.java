package edu.uth.userservice.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

/**
 * JwtUtil: helper để tạo/kiểm tra token và trích claim.
 * Lưu ý: ensure jwt.secret có đủ độ dài cho HS256 (ít nhất 32 bytes recommended).
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String jwtSecret;

    // 24 hours
    private final long jwtExpirationMs = 24 * 60 * 60 * 1000L;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    /**
     * Tạo token với subject (ví dụ email) và claim "id" lưu userId (Integer).
     */
    public String generateToken(String subject, Integer userId) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + jwtExpirationMs);
        Key key = getSigningKey();
        return Jwts.builder()
                .setSubject(subject)
                .claim("id", userId)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Trích Claims từ token; trả null nếu token không hợp lệ.
     */
    private Claims parseClaims(String token) {
        try {
            Jws<Claims> jws = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return jws.getBody();
        } catch (JwtException | IllegalArgumentException ex) {
            // token invalid / expired / malformed
            return null;
        }
    }

    /**
     * Lấy userId (Integer) từ claim "id". Trả null nếu không tìm được hoặc token invalid.
     */
    public Integer extractUserId(String token) {
        Claims claims = parseClaims(token);
        if (claims == null) return null;
        Object idObj = claims.get("id");
        if (idObj == null) return null;
        if (idObj instanceof Integer) {
            return (Integer) idObj;
        } else if (idObj instanceof Number) {
            return ((Number) idObj).intValue();
        } else {
            try {
                return Integer.valueOf(String.valueOf(idObj));
            } catch (NumberFormatException ex) {
                return null;
            }
        }
    }

    /**
     * Lấy subject (String) từ token (thường là email hoặc identifier). Trả null nếu token invalid.
     */
    public String extractSubject(String token) {
        Claims claims = parseClaims(token);
        if (claims == null) return null;
        return claims.getSubject();
    }

    /**
     * Validate token (signature + expiration). Trả true nếu hợp lệ.
     */
    public boolean validateToken(String token) {
        Claims claims = parseClaims(token);
        return claims != null;
    }
}
