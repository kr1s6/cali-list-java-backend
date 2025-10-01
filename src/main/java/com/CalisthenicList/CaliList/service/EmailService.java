package com.CalisthenicList.CaliList.service;

import com.CalisthenicList.CaliList.constants.Messages;
import com.CalisthenicList.CaliList.model.User;
import com.CalisthenicList.CaliList.repositories.UserRepository;
import com.CalisthenicList.CaliList.service.tokens.AccessTokenService;
import com.CalisthenicList.CaliList.utils.JwtUtils;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.xbill.DNS.*;
import org.xbill.DNS.Record;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.logging.Logger;

@Service
@RequiredArgsConstructor
public class EmailService {
	public final String VERIFICATION_BASE_URL = "http://localhost:8080/email-verification/";
	private final Logger logger = Logger.getLogger(EmailService.class.getName());
	private final JavaMailSender javaMailSender; //INFO - JavaMailSender @Bean is loaded automatically with "spring.mail" properties
	private final UserRepository userRepository;
	private final JwtUtils jwtUtils;
	private final UserDetailsService userDetailsService;
	private final AccessTokenService accessTokenService;

	@Async
	public void postEmailVerificationToUser(String userEmail) {
		//Generate token
		String token = accessTokenService.generateAccessToken(userEmail);

		//Create email
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

		//Send email
		javaMailSender.send(message);
	}

	public ResponseEntity<Map<String, String>> verifyEmail(String jwt) {
		//Validate jwt
		String userEmail = jwtUtils.extractEmail(jwt);
		if(userEmail == null) {
			logger.warning("Attempted verification with invalid token.");
			throw new IllegalArgumentException(Messages.TOKEN_INVALID);
		}
		UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
		boolean jwtIsValid = jwtUtils.validateJwt(jwt, userDetails);
		if(!jwtIsValid) {
			logger.warning("Attempted verification with invalid token.");
			throw new IllegalArgumentException(Messages.TOKEN_INVALID);
		}

		//Validate if user exists
		User user = userRepository.findByEmail(userEmail)
				.orElseThrow(() -> {
					logger.warning("Verification attempt for non-existing email.");
					return new UsernameNotFoundException(Messages.EMAIL_INVALID_ERROR);
				});

		//Check if the email is already verified
		if(user.isEmailVerified()) {
			logger.warning("Attempted verification of already verified email.");
			throw new IllegalStateException(Messages.EMAIL_ALREADY_VERIFIED);
		}

		//Set email verification to true
		user.setEmailVerified(true);
		userRepository.save(user);
		logger.info("Email verified successfully.");
		Map<String, String> responseMessage = Map.of("message", Messages.EMAIL_VERIFICATION_SUCCESS);
		return new ResponseEntity<>(responseMessage, HttpStatus.ACCEPTED);
	}

	//Validate if email has proper domain
	public boolean dnsEmailLookup(String email) {
		String domain = email.substring(email.indexOf("@") + 1);
		return hasMxRecord(domain);
	}

	private boolean hasMxRecord(String domain) {
		try {
			Lookup lookup = new Lookup(domain, Type.MX);
			lookup.run();
			if(lookup.getResult() != Lookup.SUCCESSFUL) {
				return false;
			} Record[] records = lookup.getAnswers();
			for(Record record : records) {
				if(record.getType() == Type.MX) {
					MXRecord mx = (MXRecord) record;
					int priority = mx.getPriority();
					return priority != 0;
				}
			} return false;
		} catch(TextParseException e) {
			return false;
		}
	}
}
