package com.CalisthenicList.CaliList.service;

import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.sql.Date;
import java.time.Duration;
import java.time.Instant;

@Service
public class TokenService {
	public String createVerificationToken(String userId) {
		Instant now = Instant.now();
		return Jwts.builder()
				.subject(userId)
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plus(Duration.ofHours(8))))
				.signWith(getSecretKey())
				.compact();
	}

	private SecretKey getSecretKey() {
		return Jwts.SIG.HS256.key().build();
	}
}
