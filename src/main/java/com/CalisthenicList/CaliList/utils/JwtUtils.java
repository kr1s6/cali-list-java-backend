package com.CalisthenicList.CaliList.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.function.Function;

@Component
public class JwtUtils {
	@Value("${jwt.secret}")
	private String jwtSecret;
	private SecretKey secretKey;

	@PostConstruct
	public void init() {
		this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
	}

	public String generateJwt(String subject, Duration jwtDuration) {
		return Jwts.builder()
				.subject(subject)
				.issuedAt(new Date())
				.expiration(new Date(new Date().getTime() + jwtDuration.toMillis()))
				.signWith(secretKey, Jwts.SIG.HS256)
				.compact();
	}

	public String extractEmail(String jwt) {
		return extractClaim(jwt, Claims::getSubject);
	}

	public Date extractExpiration(String jwt) {
		return extractClaim(jwt, Claims::getExpiration);
	}

	public <T> T extractClaim(String jwt, Function<Claims, T> claimsResolver) {
		final Claims claims = extractAllClaims(jwt);
		return claimsResolver.apply(claims);
	}

	public boolean validateJwt(String jwt, UserDetails userDetails) {
		final String jwtSubject = extractEmail(jwt);
		return (jwtSubject.equals(userDetails.getUsername())) && isJwtNotExpired(jwt);
	}

	public boolean validateJwt(String jwt, String email) {
		final String jwtSubject = extractEmail(jwt);
		return (jwtSubject.equals(email)) && isJwtNotExpired(jwt);
	}

	private boolean isJwtNotExpired(String jwt) {
		return extractExpiration(jwt).after(new Date());
	}

	private Claims extractAllClaims(String jwt) {
		return Jwts.parser()
				.verifyWith(secretKey)
				.build()
				.parseSignedClaims(jwt)
				.getPayload();
	}
}