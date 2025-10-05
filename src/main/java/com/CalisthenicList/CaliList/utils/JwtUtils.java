package com.CalisthenicList.CaliList.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
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

	public boolean validateIfJwtSubjectMatchTheUser(String jwtSubject, UserDetails userDetails) {
		return validateIfJwtSubjectMatchTheUser(jwtSubject, userDetails.getUsername());
	}

	public boolean validateIfJwtSubjectMatchTheUser(String jwtSubject, String email) {
		return (jwtSubject.equals(email));
	}

	public String extractSubject(String jwt) {
		return extractClaim(jwt, Claims::getSubject);
	}

	private <T> T extractClaim(String jwt, Function<Claims, T> claimsResolver) {
		Claims claims = extractClaims(jwt);
		return claimsResolver.apply(claims);
	}

	private Claims extractClaims(String jwt) {
		try {
			return Jwts.parser()
					.verifyWith(secretKey)
					.build()
					.parseSignedClaims(jwt)
					.getPayload();
		} catch(SignatureException e) {
			//Validated if the secret is correct
			throw new SignatureException(e.getMessage());
		} catch(ExpiredJwtException e) {
			//Validated if jwt is expired
			throw new ExpiredJwtException(e.getHeader(), e.getClaims(), "JWT token is expired: " + e.getMessage());
		}
	}
}