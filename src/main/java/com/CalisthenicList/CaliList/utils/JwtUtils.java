package com.CalisthenicList.CaliList.utils;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Component
public class JwtUtils {
	@Value("${jwt.secret}")
	private String jwtSecret;
	private SecretKey secretKey;

	@PostConstruct
	public void init() {
		this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
	}

	public String generateJwtToken(String email) {
		int minutesToExpire = 15;
		return Jwts.builder()
				.subject(email)
				.issuedAt(new Date())
				.expiration(Date.from(Instant.now().plus(minutesToExpire, ChronoUnit.MINUTES)))
				.signWith(secretKey, Jwts.SIG.HS256)
				.compact();
	}

	public String extractEmailFromToken(String jwtToken) {
		return Jwts.parser()
				.verifyWith(secretKey)
				.build()
				.parseSignedClaims(jwtToken)
				.getPayload()
				.getSubject();
	}

	public boolean validateJwtToken(String jwtToken) {
		try {
			Jwts.parser()
					.verifyWith(secretKey)
					.build()
					.parseSignedClaims(jwtToken);
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