package com.CalisthenicList.CaliList.service;

import com.CalisthenicList.CaliList.model.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

//@ExtendWith(SpringExtension.class)
//INFO ExtendWith(SpringExtension.class) is more lightweight than @SpringBootTest, because it doesn't start the whole Spring context'
//@ContextConfiguration(classes = {EmailService.class, TokenService.class, EmailConfig.class})
@SpringBootTest
class EmailServiceTest {
	private static User user;
	@Autowired
	private EmailService emailService;

	@BeforeAll
	static void setUp() {
		user = new User();
		user.setId(1L);
	}

	@Test
	@DisplayName("✅ Happy Case: Email is properly sent.")
	void givenEmailService_whenEmailVerification_thenEmailSent() {
		user.setEmail("krzysztof.bielkiewicz@interia.pl");
		emailService.emailVerification(user);
	}

}