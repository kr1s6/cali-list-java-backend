package com.CalisthenicList.CaliList.service;

import com.CalisthenicList.CaliList.constants.Messages;
import com.CalisthenicList.CaliList.model.User;
import com.CalisthenicList.CaliList.repositories.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.xbill.DNS.*;
import org.xbill.DNS.Record;

import javax.crypto.SecretKey;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.logging.Logger;

@Service
@RequiredArgsConstructor
public class EmailService {
	public final int TokenExpirationTimeHours = 2;
	public final String VERIFICATION_BASE_URL = "http://localhost:8080/api/user/email-verification/";
	private final Logger logger = Logger.getLogger(EmailService.class.getName());
	private final JavaMailSender javaMailSender; //INFO - JavaMailSender @Bean is loaded automatically with "spring.mail" properties
	private final UserRepository userRepository;
	@Value("${secret.key}")
	private CharSequence secretKey;

	public boolean dnsEmailLookup(String email) {
		String domain = email.substring(email.indexOf("@") + 1);
		return hasMxRecord(domain);
	}

	public void postEmailVerificationToUser(UUID userId, String userEmail) {
		String token = createVerificationTokenWithId(userId);
		String verifyUrl = VERIFICATION_BASE_URL + URLEncoder.encode(token, StandardCharsets.UTF_8);
		MimeMessage message = javaMailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
		try {
			InternetAddress from = new InternetAddress("no-reply@CaliList.com", "CaliList");
			helper.setFrom(from);
			helper.setTo(userEmail);
			helper.setSubject("Email verification");
			String content = "Click below to verify your email:\n" + verifyUrl;
			helper.setText(content);
		} catch(UnsupportedEncodingException | MessagingException e) {
			logger.warning("Failed to compose verification email.");
			throw new IllegalStateException("Failed to compose verification email", e);
		}
		javaMailSender.send(message);
	}

	public String createVerificationTokenWithId(UUID userId) {
		Instant now = Instant.now();
		Date issuedAt = Date.from(now);
		Date expiration = Date.from(now.plus(Duration.ofHours(TokenExpirationTimeHours)));
		SecretKey key = getSecretKey(secretKey);
		return Jwts.builder()
				.subject(String.valueOf(userId))
				.issuedAt(issuedAt)
				.expiration(expiration)
				.signWith(key, Jwts.SIG.HS256)
				.compact();
	}

	public ResponseEntity<String> verifyEmail(String token) {
		SecretKey key = getSecretKey(secretKey);
		Claims claims;
		try {
			claims = Jwts.parser()
					.verifyWith(key)
					.build()
					.parseSignedClaims(token)
					.getPayload();
		} catch(ExpiredJwtException e) {
			logger.warning("Attempted verification of expired token.");
			return new ResponseEntity<>(Messages.TOKEN_EXPIRED, HttpStatus.BAD_REQUEST);
		} catch(SignatureException | MalformedJwtException e) {
			logger.warning("Attempted verification of invalid token.");
			return new ResponseEntity<>(Messages.TOKEN_INVALID, HttpStatus.BAD_REQUEST);
		}
		UUID userId = UUID.fromString(claims.getSubject());
		User user = userRepository.findById(userId).orElseThrow();
		if(user.isEmailVerified()) {
			logger.warning("Attempted verification of already verified email.");
			return new ResponseEntity<>(Messages.EMAIL_ALREADY_VERIFIED, HttpStatus.ALREADY_REPORTED);
		}
		user.setEmailVerified(true);
		userRepository.save(user);
		logger.info("Email verified successfully.");
		return new ResponseEntity<>(Messages.EMAIL_VERIFICATION_SUCCESS, HttpStatus.ACCEPTED);
	}

	public SecretKey getSecretKey(CharSequence secretKey) {
		byte[] keyBytes = Decoders.BASE64.decode(secretKey);
		return Keys.hmacShaKeyFor(keyBytes);
	}

	private boolean hasMxRecord(String domain) {
		try {
			Lookup lookup = new Lookup(domain, Type.MX);
			lookup.run();
			if(lookup.getResult() != Lookup.SUCCESSFUL) {
				return false;
			}
			Record[] records = lookup.getAnswers();
			for(Record record : records) {
				if(record.getType() == Type.MX) {
					MXRecord mx = (MXRecord) record;
					int priority = mx.getPriority();
					return priority != 0;
				}
			}
			return false;
		} catch(TextParseException e) {
			return false;
		}
	}
}
