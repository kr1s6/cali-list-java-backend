package com.CalisthenicList.CaliList.service;

import com.CalisthenicList.CaliList.constants.Messages;
import com.CalisthenicList.CaliList.model.ApiResponse;
import com.CalisthenicList.CaliList.model.User;
import com.CalisthenicList.CaliList.repositories.UserRepository;
import com.CalisthenicList.CaliList.service.tokens.AccessTokenService;
import com.CalisthenicList.CaliList.utils.JwtUtils;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@RequiredArgsConstructor
class EmailServiceTest {

	@Mock
	private JavaMailSender javaMailSender;
	@Mock
	private UserRepository userRepository;
	@Mock
	private AccessTokenService accessTokenService;
	@Mock
	private JwtUtils jwtUtils;
	@InjectMocks
	private EmailService emailService;

	private Optional<User> findByEmail(String email) {
		return userRepository.findByEmail(email);
	}

	@Test
	@DisplayName("✅ Happy Case: Verification email is composed and sent correctly.")
	void postEmailVerificationToUserTest() throws Exception {
		// Given
		String userEmail = "test@exmaple.com";
		String token = "header.payload.signature";
		MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
		Mockito.when(accessTokenService.generateAccessToken(anyString())).thenReturn(token);
		when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
		EmailService spyService = spy(this.emailService);
		// When
		spyService.postEmailVerificationToUser(userEmail);
		// Then
		verify(javaMailSender).send(mimeMessage);
		InternetAddress from = new InternetAddress("no-reply@CaliList.com", "CaliList");
		assertEquals(from, mimeMessage.getFrom()[0], "Wrong email sender.");
		assertEquals(userEmail, mimeMessage.getAllRecipients()[0].toString(), "Wrong email recipient.");
		assertEquals("Email verification", mimeMessage.getSubject(), "Wrong email subject.");
		String verifyUrl = spyService.VERIFICATION_BASE_URL + URLEncoder.encode(token, StandardCharsets.UTF_8);
		String expectedContent = "Click below to verify your email:\n" + verifyUrl;
		assertEquals(expectedContent, mimeMessage.getContent().toString(), "Wrong email content.");
	}

	@Nested
	@DisplayName("dnsEmailLookup")
	class DnsEmailLookupTest {

		private boolean dnsEmailLookup(String email) {
			return emailService.dnsEmailLookup(email);
		}

		@DisplayName("✅ Happy Case: Valid email domain.")
		@ParameterizedTest(name = "Valid domain: \"{0}\"")
		@ValueSource(strings = {"test@gmail.com", "test@icloud.com", "test@outlook.com", "test@yahoo.com"})
		void givenValidEmailDomain_whenDnsEmailLookup_thenReturnTrue(String email) {
			// When
			boolean isValidEmailDomain = dnsEmailLookup(email);
			// Then
			assertTrue(isValidEmailDomain, "Domain should be valid.");
		}

		@DisplayName("❌ Negative Case: Invalid email domain")
		@ParameterizedTest(name = "Invalid domain: \"{0}\"")
		@ValueSource(strings = {"test@example.com", "test@gmial.com", "test@notarealdomain.fake"})
		void givenInvalidEmailDomain_whenDnsEmailLookup_thenReturnFalse(String email) {
			// When
			boolean isValidEmailDomain = dnsEmailLookup(email);
			// Then
			assertFalse(isValidEmailDomain, "Domain should be invalid.");
		}
	}

	@Nested
	@DisplayName("verifyEmail")
	class VerifyEmailTest {
		private final String jwtToken = "header.payload.signature";
		private final String userEmail = "test@example.com";
		private User testUser;

		private ResponseEntity<ApiResponse<Object>> verifyEmail() {
			return emailService.verifyEmail(jwtToken);
		}

		@BeforeEach
		void initEach() {
			testUser = new User("testUser", userEmail, "encodedPassword");
			testUser.setEmailVerified(false);
			emailService = spy(emailService);
		}

		@Test
		@DisplayName("✅ Happy Case: Email verified successfully.")
		void givenValidJwt_whenVerifyEmail_thenReturnAccepted() {
			// Given
			when(jwtUtils.extractSubject(jwtToken)).thenReturn(userEmail);
			when(findByEmail(userEmail)).thenReturn(Optional.of(testUser));
			when(jwtUtils.validateIfJwtSubjectMatchTheUser(userEmail, testUser.getEmail())).thenReturn(true);
			// When
			ResponseEntity<ApiResponse<Object>> response = verifyEmail();
			// Then
			assertEquals(HttpStatus.ACCEPTED, response.getStatusCode(), "Should return ACCEPTED");
			Assertions.assertNotNull(response.getBody());
			assertEquals(Messages.EMAIL_VERIFICATION_SUCCESS, response.getBody().getMessage(), "Wrong success message");
			assertTrue(testUser.isEmailVerified(), "User email should be marked as verified");
			verify(userRepository).save(testUser);
		}

		@Test
		@DisplayName("❌ Negative case: Invalid token.")
		void givenInvalidJwt_whenVerifyEmail_thenThrowIllegalArgumentException() {
			// Given
			when(jwtUtils.extractSubject(jwtToken)).thenReturn(null);
			// When & Then
			IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, this::verifyEmail);
			assertEquals(Messages.TOKEN_INVALID, ex.getMessage());
		}

		@Test
		@DisplayName("❌ Negative Case: Email already verified")
		void givenAlreadyVerifiedUser_whenVerifyEmail_thenThrowIllegalStateException() {
			// Given
			testUser.setEmailVerified(true);
			when(jwtUtils.extractSubject(jwtToken)).thenReturn(userEmail);
			when(findByEmail(userEmail)).thenReturn(Optional.of(testUser));
			when(jwtUtils.validateIfJwtSubjectMatchTheUser(userEmail, testUser.getEmail())).thenReturn(true);
			// When & Then
			IllegalStateException ex = assertThrows(IllegalStateException.class, this::verifyEmail);
			assertEquals(Messages.EMAIL_ALREADY_VERIFIED, ex.getMessage());
		}
	}
}