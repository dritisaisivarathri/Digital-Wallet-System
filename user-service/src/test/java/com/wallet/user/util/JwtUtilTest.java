package com.wallet.user.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private final String secret = "9a721adbd045f54f13453b664321683416b2ea8ec5a1b3a16b2ea8ec5a1b3a1";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", secret);
    }

    private String createTestToken(String userId, String email, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", role);
        claims.put("fullName", "Test User");
        claims.put("phoneNumber", "1234567890");

        byte[] keyBytes = Decoders.BASE64.decode(secret);
        Key key = Keys.hmacShaKeyFor(keyBytes);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(email)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    @Test
    void testExtractionMethods() {
        String userId = "user-123";
        String email = "test@example.com";
        String role = "ROLE_USER";
        String token = createTestToken(userId, email, role);

        assertEquals(userId, jwtUtil.extractUserId(token));
        assertEquals(email, jwtUtil.extractEmail(token));
        assertEquals(role, jwtUtil.extractRole(token));
        assertEquals("Test User", jwtUtil.extractFullName(token));
        assertEquals("1234567890", jwtUtil.extractPhoneNumber(token));
    }

    @Test
    void testExtractAllClaimsWithBearerPrefix() {
        String token = createTestToken("id", "mail", "role");
        Claims claims = jwtUtil.extractAllClaims("Bearer " + token);
        assertNotNull(claims);
        assertEquals("mail", claims.getSubject());
    }

    @Test
    void testExtractAllClaimsWithoutPrefix() {
        String token = createTestToken("id", "mail", "role");
        Claims claims = jwtUtil.extractAllClaims(token);
        assertNotNull(claims);
        assertEquals("mail", claims.getSubject());
    }
}
