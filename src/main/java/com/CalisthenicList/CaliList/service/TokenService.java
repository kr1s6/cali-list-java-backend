package com.CalisthenicList.CaliList.service;

import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
public class TokenService {
	private static final int TokenExpirationTime = 8;

	public String createVerificationToken(String userId) {
		Instant now = Instant.now();
		Date issuedAt = Date.from(now);
		Date expiration = Date.from(now.plus(Duration.ofHours(TokenExpirationTime)));
		return Jwts.builder()
				.subject(userId)
				.issuedAt(issuedAt)
				.expiration(expiration)
				.signWith(getSecretKey())
				.compact();
	}

	private SecretKey getSecretKey() {
		return Jwts.SIG.HS256.key().build();
	}
}
