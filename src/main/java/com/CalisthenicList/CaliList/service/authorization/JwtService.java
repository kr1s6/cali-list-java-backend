package com.CalisthenicList.CaliList.service.authorization;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;
import java.util.function.Function;

/**
 * Jwt service to build, parse(read) and validate JWT values.
 * @see <a href="https://www.baeldung.com/java-json-web-tokens-jjwt">Info reference</a>
 * @see <a href="https://cheatsheetseries.owasp.org/cheatsheets/JSON_Web_Token_for_Java_Cheat_Sheet.html">OWASP reference</a>
 */
@Service
public class JwtService {
	@Autowired
	private SecretService secretService;
	private final String issuer = "CaliList";

	/**
	 * Three-step process:
	 * 1. The definition of the internal claims of the token: Issuer, Subject, IssuedAt, Expiration
	 * 2. The cryptographic signing of the JWT (making it a JWS)
	 * 3. The compaction of the JWT to a URL-safe string, according to the JWT Compact Serialization rules
	 * @param subject     The Email of the {@code User}
	 * @param jwtDuration The duration of expiration claim
	 * @return The JWT with three-part base64-encoded string, signed with the signature algorithm SIG HS256, using generated secret beforehand.
	 * After this point, JWT is ready to share with another party.
	 */
	public String buildJwt(String subject, Duration jwtDuration) {
		Date now = new Date();
		Date expiration = new Date(now.getTime() + jwtDuration.toMillis());
		return Jwts.builder()
				.issuer(issuer)
				.subject(subject)
				.issuedAt(now)
				.expiration(expiration)
				.signWith(secretService.getHS256SecretKey(), Jwts.SIG.HS256)
				.compact();
	}

	public boolean validateIfJwtSubjectMatchTheUser(String jwtSubject, String email) {
		return jwtSubject.equals(email);
	}

	public String extractSubject(String jwt) {
		return extractClaim(jwt, Claims::getSubject);
	}

	private <T> T extractClaim(String jwt, Function<Claims, T> claimsResolver) {
		Claims claims = getJwtPayload(jwt);
		return claimsResolver.apply(claims);
	}

	public Claims getJwtPayload(String jwt) {
		return parseJwt(jwt).getPayload();
	}

	/**
	 * Function to parse jwt if signed with valid secret.
	 * @param jwt Jwt you want to parse
	 * @return Jws<Claims> with body, digest, header, payload and signature.
	 */
	public Jws<Claims> parseJwt(String jwt) {
		try {
			return Jwts.parser()
					.requireIssuer(issuer)
					.verifyWith(secretService.getHS256SecretKey())
					.build()
					.parseSignedClaims(jwt);
		} catch(SignatureException e) {
			throw new JwtException("Invalid JWT signature.", e);
		} catch(ExpiredJwtException e) {
			throw new JwtException("JWT expired.", e);
		} catch(IncorrectClaimException e) {
			throw new JwtException("JWT contains incorrect claim", e);
		} catch(JwtException e) {
			throw new JwtException("Invalid JWT", e);
		}
	}
}