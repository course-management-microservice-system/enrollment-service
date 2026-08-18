package com.nexusenroll.enrollment.logic;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;

public class JwtUtility {
    // In a real production environment, load this from a secure vault/environment
    // variable.
    // For this architecture, we use a static key so the Student/Faculty services
    // can verify the token.
    private static final String SECRET = "NexusEnrollSuperSecretKeyMustBeVeryLongToWorkWithHS256Algorithm12345!";
    private static final Key SECRET_KEY = Keys.hmacShaKeyFor(SECRET.getBytes());

    public static String generateToken(String username, String role) {
        long expirationTime = 1000 * 60 * 60 * 24; // 24 Hours

        return Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(SECRET_KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    public static Key getSecretKey() {
        return SECRET_KEY;
    }

}