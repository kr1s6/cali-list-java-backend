package com.CalisthenicList.CaliList.service.authorization;

import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.HashMap;
import java.util.Map;

/**
 * At application startup time, a new secret is created for HS256 algorithm.
 * @see <a href="https://www.baeldung.com/java-json-web-tokens-jjwt"> java-json-web-tokens-jjwt </a>
 */
@Service
public class SecretService {
	private final Map<String, SecretKey> secretKeys = new HashMap<>();

	@PostConstruct
	public void setup() {
		refreshSecrets();
	}

	public SecretKey getHS256SecretKey() {
		return secretKeys.get(Jwts.SIG.HS256.toString());
	}

	private void refreshSecrets() {
		SecretKey key = Jwts.SIG.HS256.key().build();
		secretKeys.put(Jwts.SIG.HS256.toString(), key);
	}
}
