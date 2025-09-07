package com.CalisthenicList.CaliList.service;

import com.CalisthenicList.CaliList.repositories.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.SignatureException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@RequiredArgsConstructor
class EmailServiceTest {

	private final CharSequence testSecretKey = Encoders.BASE64.encode(Jwts.SIG.HS256.key().build().getEncoded());
	@Mock
	private JavaMailSender javaMailSender;
	@Mock
	private UserRepository userRepository;
	@InjectMocks
	private EmailService emailService;

	@Test
	@DisplayName("✅ Happy Case: Verification token has expected values.")
	void postEmailVerificationToUser() throws Exception {
//		Given
		MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
		when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
		EmailService emailService = spy(new EmailService(javaMailSender, userRepository));

		UUID userId = UUID.randomUUID();
		String userEmail = "biwoya8899@knilok.com";
		String token = "header.payload.signature";
		doReturn(token).when(emailService).createVerificationTokenWithId(userId);
//		When
		emailService.postEmailVerificationToUser(userId, userEmail);
//		Then
		verify(javaMailSender).send(mimeMessage);
		InternetAddress from = new InternetAddress("no-reply@CaliList.com", "CaliList");
		assertEquals(from, mimeMessage.getFrom()[0]);
		assertEquals(userEmail, mimeMessage.getAllRecipients()[0].toString());
		assertEquals("Email verification", mimeMessage.getSubject());
		String verifyUrl = this.emailService.VERIFICATION_BASE_URL + URLEncoder.encode(token, StandardCharsets.UTF_8);
		String expectedContent = "Click below to verify your email:\n" + verifyUrl;
		assertEquals(expectedContent, mimeMessage.getContent().toString());
	}

	@Test
	@DisplayName("✅ Happy Case: Verification token has expected claims.")
	void createVerificationTokenWithExpectedClaims() {
//		Given
		this.emailService.secretKey = testSecretKey;
		UUID userId = UUID.randomUUID();
//		When
		String token = emailService.createVerificationTokenWithId(userId);
		Claims claims = Jwts.parser()
				.verifyWith(emailService.getSecretKey(testSecretKey))
				.build()
				.parseSignedClaims(token)
				.getPayload();
//		Then
		assertEquals(claims.getSubject(), userId.toString());
		Date shortAfterExpirationTime = Date.from(Instant.now().plus(Duration.ofHours(emailService.TokenExpirationTime)));
		Date shortBeforeExpirationTime = Date.from(Instant.now().plus(Duration.ofHours(emailService.TokenExpirationTime)).minus(Duration.ofMinutes(1)));
		assertTrue(claims.getExpiration().before(shortAfterExpirationTime));
		assertTrue(claims.getExpiration().after(shortBeforeExpirationTime));
		assertTrue(claims.getIssuedAt().before(Date.from(Instant.now())));
	}

	@Test
	@DisplayName("❌ Negative case: Verification should fail with invalid secret key.")
	void createVerificationTokenWithInvalidSecretFails() {
//		Given
		this.emailService.secretKey = testSecretKey;
		UUID userId = UUID.randomUUID();
		String token = emailService.createVerificationTokenWithId(userId);
		CharSequence wrongSecretKey = Encoders.BASE64.encode(Jwts.SIG.HS256.key().build().getEncoded());
//		Then
		assertThrows(SignatureException.class, () ->
				Jwts.parser()
						.verifyWith(emailService.getSecretKey(wrongSecretKey))
						.build()
						.parseSignedClaims(token)
						.getPayload()
		);
	}
}












