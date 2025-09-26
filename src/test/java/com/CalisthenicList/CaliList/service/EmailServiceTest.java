//package com.CalisthenicList.CaliList.service;
//
//import com.CalisthenicList.CaliList.constants.Messages;
//import com.CalisthenicList.CaliList.model.User;
//import com.CalisthenicList.CaliList.repositories.UserRepository;
//import io.jsonwebtoken.Claims;
//import io.jsonwebtoken.Jwts;
//import io.jsonwebtoken.io.Encoders;
//import io.jsonwebtoken.security.SignatureException;
//import jakarta.mail.Session;
//import jakarta.mail.internet.InternetAddress;
//import jakarta.mail.internet.MimeMessage;
//import lombok.RequiredArgsConstructor;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.junit.jupiter.params.ParameterizedTest;
//import org.junit.jupiter.params.provider.ValueSource;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.Mockito;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.mail.javamail.JavaMailSender;
//
//import javax.crypto.SecretKey;
//import java.net.URLEncoder;
//import java.nio.charset.StandardCharsets;
//import java.time.Duration;
//import java.time.Instant;
//import java.util.Date;
//import java.util.Optional;
//import java.util.Properties;
//import java.util.UUID;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//@RequiredArgsConstructor
//class EmailServiceTest {
//
//	@Mock
//	private JavaMailSender javaMailSender;
//	@Mock
//	private UserRepository userRepository;
//	@InjectMocks
//	private EmailService emailService;
//
//	@Test
//	@DisplayName("✅ Happy Case: Verification token has expected values.")
//	void postEmailVerificationToUserTest() throws Exception {
//		// Given
//		UUID userId = UUID.randomUUID();
//		String userEmail = "test@exmaple.com";
//		String token = "header.payload.signature";
//		MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
//		when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
//		EmailService emailService = spy(new EmailService(javaMailSender, userRepository));
//		doReturn(token).when(emailService).createVerificationTokenWithId(userId);
//		// When
//		emailService.postEmailVerificationToUser(userId, userEmail);
//		// Then
//		verify(javaMailSender).send(mimeMessage);
//		InternetAddress from = new InternetAddress("no-reply@CaliList.com", "CaliList");
//		assertEquals(from, mimeMessage.getFrom()[0], "Wrong email sender.");
//		assertEquals(userEmail, mimeMessage.getAllRecipients()[0].toString(), "Wrong email recipient.");
//		assertEquals("Email verification", mimeMessage.getSubject(), "Wrong email subject.");
//		String verifyUrl = emailService.VERIFICATION_BASE_URL + URLEncoder.encode(token, StandardCharsets.UTF_8);
//		String expectedContent = "Click below to verify your email:\n" + verifyUrl;
//		assertEquals(expectedContent, mimeMessage.getContent().toString(), "Wrong email content.");
//	}
//
//	@Nested
//	@DisplayName("dnsEmailLookup")
//	class DnsEmailLookupTest {
//
//		@DisplayName("✅ Happy Case: Valid email domain.")
//		@ParameterizedTest(name = "Valid domain: \"{0}\"")
//		@ValueSource(strings = {"test@gmail.com", "test@icloud.com", "test@outlook.com", "test@yahoo.com"})
//		void givenValidEmailDomain_whenDnsEmailLookup_thenReturnTrue(String email) {
//			// When
//			boolean isValidEmailDomain = emailService.dnsEmailLookup(email);
//			// Then
//			assertTrue(isValidEmailDomain, "Domain should be valid.");
//		}
//
//		@DisplayName("❌ Negative Case: Invalid email domain")
//		@ParameterizedTest(name = "Invalid domain: \"{0}\"")
//		@ValueSource(strings = {"test@example.com", "test@gmial.com", "test@notarealdomain.fake"})
//		void givenInvalidEmailDomain_whenDnsEmailLookup_thenReturnFalse(String email) {
//			// When
//			boolean isValidEmailDomain = emailService.dnsEmailLookup(email);
//			// Then
//			assertFalse(isValidEmailDomain, "Domain should be invalid.");
//		}
//	}
//
//	@Nested
//	@DisplayName("createVerificationTokenWithId")
//	class CreateVerificationTokenWithIdTest {
//
//		private final CharSequence testSecretKey = Encoders.BASE64.encode(Jwts.SIG.HS256.key().build().getEncoded());
//		private EmailService emailServiceSpy;
//
//		@BeforeEach
//		void initEach() {
//			emailServiceSpy = spy(new EmailService(javaMailSender, userRepository));
//		}
//
//		@Test
//		@DisplayName("✅ Happy Case: Verification token has expected claims.")
//		void givenUserId_whenCreateVerificationToken_thenTokenHasExpectedClaims() {
//			// Given
//			SecretKey encodedSecretKey = emailServiceSpy.getJwtSecret(testSecretKey);
//			doReturn(encodedSecretKey).when(emailServiceSpy).getJwtSecret(Mockito.any());
//			UUID userId = UUID.randomUUID();
//			// When
//			String token = emailServiceSpy.createVerificationTokenWithId(userId);
//			Claims claims = Jwts.parser().verifyWith(encodedSecretKey).build().parseSignedClaims(token).getPayload();
//			// Then
//			assertEquals(claims.getSubject(), userId.toString(), "Wrong token subject.");
//			Date shortAfterExpirationTime = Date.from(Instant.now().plus(Duration.ofHours(emailServiceSpy.TokenExpirationTimeHours)));
//			Date shortBeforeExpirationTime = Date.from(Instant.now().plus(Duration.ofHours(emailServiceSpy.TokenExpirationTimeHours)).minus(Duration.ofMinutes(1)));
//			assertTrue(claims.getExpiration().before(shortAfterExpirationTime), "Token expiration time is too long.");
//			assertTrue(claims.getExpiration().after(shortBeforeExpirationTime), "Token expiration time is too short.");
//			assertTrue(claims.getIssuedAt().before(Date.from(Instant.now())), "Token issued time is in the future.");
//		}
//
//		@Test
//		@DisplayName("❌ Negative case: Verification token has invalid secret key.")
//		void givenInvalidSecretKey_whenParseVerificationToken_thenThrowsSignatureException() {
//			//Given
//			CharSequence wrongSecretKey = Encoders.BASE64.encode(Jwts.SIG.HS256.key().build().getEncoded());
//			SecretKey validEncodedSecretKey = emailServiceSpy.getJwtSecret(testSecretKey);
//			SecretKey invalidEncodedSecretKey = emailServiceSpy.getJwtSecret(wrongSecretKey);
//			doReturn(invalidEncodedSecretKey).when(emailServiceSpy).getJwtSecret(Mockito.any());
//			UUID userId = UUID.randomUUID();
//			// When
//			String token = emailServiceSpy.createVerificationTokenWithId(userId);
//			//Then
//			SignatureException thrown = assertThrows(SignatureException.class, () -> Jwts.parser().verifyWith(validEncodedSecretKey).build().parseSignedClaims(token).getPayload(), "Verification passed with invalid secret key.");
//			System.out.println("Caught exception: " + thrown);
//		}
//	}
//
//	@Nested
//	@DisplayName("verifyEmail")
//	class VerifyEmailTest {
//
//		private final CharSequence testSecretKey = Encoders.BASE64.encode(Jwts.SIG.HS256.key().build().getEncoded());
//		private final UUID userId = UUID.randomUUID();
//		private SecretKey encodedSecretKey;
//		private EmailService emailServiceSpy;
//
//		@BeforeEach
//		void initEach() {
//			// Given
//			encodedSecretKey = emailService.getJwtSecret(testSecretKey);
//			emailServiceSpy = spy(new EmailService(javaMailSender, userRepository));
//		}
//
//		@Test
//		@DisplayName("✅ Happy Case: Valid email verification.")
//		void givenValidToken_whenVerifyEmail_thenReturnEmailVerificationSuccess() {
//			// Given
//			doReturn(encodedSecretKey).when(emailServiceSpy).getJwtSecret(Mockito.any());
//			Mockito.when(userRepository.findById(userId)).thenReturn(Optional.of(new User("test", "test", "test")));
//			String token = emailServiceSpy.createVerificationTokenWithId(userId);
//			// When
//			ResponseEntity<String> response = emailServiceSpy.verifyEmail(token);
//			// Then
//			System.out.println(response);
//			assertTrue(response.getStatusCode().isSameCodeAs(HttpStatus.ACCEPTED), "Should return Accepted.");
//			assertEquals(Messages.EMAIL_VERIFICATION_SUCCESS, response.getBody(), "Wrong error message.");
//		}
//
//		@Test
//		@DisplayName("❌ Negative Case: Expired token.")
//		void givenExpiredToken_whenVerifyEmail_thenReturnTokenExpiredError() {
//			// When
//			doReturn(encodedSecretKey).when(emailServiceSpy).getJwtSecret(Mockito.any());
//			Instant issuedAtDate = Instant.now().minus(Duration.ofHours(2));
//			String expiredToken = custom_createVerificationTokenWithId(userId, issuedAtDate);
//			ResponseEntity<String> response = emailServiceSpy.verifyEmail(expiredToken);
//			// Then
//			System.out.println(response);
//			assertTrue(response.getStatusCode().isSameCodeAs(HttpStatus.BAD_REQUEST), "Should return Bad Request.");
//			assertEquals(Messages.TOKEN_EXPIRED, response.getBody(), "Wrong error message.");
//		}
//
//		private String custom_createVerificationTokenWithId(UUID userId, Instant issuedAtDate) {
//			Date issuedAt = Date.from(issuedAtDate);
//			Date expiration = Date.from(issuedAtDate.plus(Duration.ofHours(emailService.TokenExpirationTimeHours)));
//			SecretKey key = emailService.getJwtSecret(testSecretKey);
//			return Jwts.builder()
//					.subject(String.valueOf(userId)).
//					issuedAt(issuedAt).expiration(expiration).
//					signWith(key, Jwts.SIG.HS256)
//					.compact();
//		}
//
//		@Test
//		@DisplayName("❌ Negative Case: Invalid token.")
//		void givenInvalidToken_whenVerifyEmail_thenReturnTokenInvalidError() {
//			// Given
//			doReturn(encodedSecretKey).when(emailServiceSpy).getJwtSecret(Mockito.any());
//			String token = "invalid-token";
//			// When
//			ResponseEntity<String> response = emailServiceSpy.verifyEmail(token);
//			// Then
//			System.out.println(response);
//			assertTrue(response.getStatusCode().isSameCodeAs(HttpStatus.BAD_REQUEST), "Should return Bad Request.");
//			assertEquals(Messages.TOKEN_INVALID, response.getBody(), "Wrong error message.");
//		}
//
//		@Test
//		@DisplayName("❌ Negative Case: Invalid secret key.")
//		void givenTokenWithInvalidSecretKey_whenVerifyEmail_thenReturnTokenInvalidError() {
//			// Given
//			// Create a token with a wrong secret key
//			CharSequence wrongSecretKey = Encoders.BASE64.encode(Jwts.SIG.HS256.key().build().getEncoded());
//			SecretKey wrongEncodedSecretKey = emailService.getJwtSecret(wrongSecretKey);
//			doReturn(wrongEncodedSecretKey).when(emailServiceSpy).getJwtSecret(Mockito.any());
//			String invalidToken = emailServiceSpy.createVerificationTokenWithId(userId);
//			// Insert valid secret key for function
//			doReturn(encodedSecretKey).when(emailServiceSpy).getJwtSecret(Mockito.any());
//			// When
//			ResponseEntity<String> response = emailServiceSpy.verifyEmail(invalidToken);
//			// Then
//			System.out.println(response);
//			assertTrue(response.getStatusCode().isSameCodeAs(HttpStatus.BAD_REQUEST), "Should return Bad Request.");
//			assertEquals(Messages.TOKEN_INVALID, response.getBody(), "Wrong error message.");
//		}
//
//
//		@Test
//		@DisplayName("❌ Negative Case: Email already verified.")
//		void givenAlreadyVerifiedUserToken_whenVerifyEmail_thenReturnEmailAlreadyVerified() {
//			// Given
//			doReturn(encodedSecretKey).when(emailServiceSpy).getJwtSecret(Mockito.any());
//			User alreadyVerifiedUser = new User("test", "test", "test");
//			alreadyVerifiedUser.setEmailVerified(true);
//			Mockito.when(userRepository.findById(userId)).thenReturn(Optional.of(alreadyVerifiedUser));
//			String token = emailServiceSpy.createVerificationTokenWithId(userId);
//			// When
//			ResponseEntity<String> response = emailServiceSpy.verifyEmail(token);
//			// Then
//			System.out.println(response);
//			assertTrue(response.getStatusCode().isSameCodeAs(HttpStatus.ALREADY_REPORTED), "Should return Already Reported.");
//			assertEquals(Messages.EMAIL_ALREADY_VERIFIED, response.getBody(), "Wrong message.");
//		}
//	}
//}