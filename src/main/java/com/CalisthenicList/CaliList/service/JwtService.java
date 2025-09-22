package com.CalisthenicList.CaliList.service;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class JwtService {
	private final SecretKey secretKey;

	public JwtService(@Value("${jwt.secret}") String jwtSecret) {
		secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes());
	}

	public String generateJwtToken(String email) {
		return Jwts.builder()
				.subject(email)
				.issuedAt(new Date())
				.expiration(Date.from(Instant.now().plus(10, ChronoUnit.DAYS)))
				.signWith(secretKey)
				.compact();
	}

	public String extractEmail(String token) {
		return Jwts.parser()
				.verifyWith(secretKey)
				.build()
				.parseSignedClaims(token)
				.getPayload()
				.getSubject();
	}

	public boolean validateJwtToken(String token) {
		try {
			Jwts.parser()
					.verifyWith(secretKey)
					.build()
					.parseSignedClaims(token);
			return true;
		} catch(SecurityException e) {
			System.out.println("Invalid JWT signature: " + e.getMessage());
		} catch(MalformedJwtException e) {
			System.out.println("Invalid JWT token: " + e.getMessage());
		} catch(ExpiredJwtException e) {
			System.out.println("JWT token is expired: " + e.getMessage());
		} catch(UnsupportedJwtException e) {
			System.out.println("JWT token is unsupported: " + e.getMessage());
		} catch(IllegalArgumentException e) {
			System.out.println("JWT claims string is empty: " + e.getMessage());
		}
		return false;
	}
}