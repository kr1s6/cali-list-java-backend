package com.CalisthenicList.CaliList.service;

import com.CalisthenicList.CaliList.model.User;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.Type;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class EmailService {
	//INFO - JavaMailSender @Bean is loaded automatically with "spring.mail" properties
	private final JavaMailSender javaMailSender;
	private final TokenService tokenService;

	@SneakyThrows
	public void emailVerification(User user) {
		String token = tokenService.createVerificationToken(String.valueOf(user.getId()));
		String verifyUrl = "https://your-app.com/verify?token=" +
				URLEncoder.encode(token, StandardCharsets.UTF_8);
		MimeMessage message = javaMailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
		helper.setFrom(new InternetAddress("no-reply@CaliList.com", "CaliList"));
		helper.setReplyTo("no-reply@CaliList.com");
//        TODO - w Gmailu/Google Workspace skonfigurujesz i zweryfikujesz alias „Send mail as”
//         (Wyślij jako) dla tego adresu, łącznie z poprawnym SPF/DKIM/DMARC dla domeny
		helper.setTo(user.getEmail());
		helper.setSubject("Verify your email");
		String content = "Click below to verify your email:\n" + verifyUrl;
		helper.setText(content);
//		TODO check if link is single use
		javaMailSender.send(message);
	}

//	INFO Sending a confirmation email
//  - Ultimately, you should send a confirmation email with a one-time token
//  to the given email address, and store that token in your database for future reference.
//  If the user can click a link in the email sent (ie. can send the token back),
//  he proves that the email address is valid and it actually belongs to him.

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
			}
			Record[] records = lookup.getAnswers();
			for(Record record : records) {
				if(record.getType() == Type.MX) {
					return true;
				}
			}
			return false;
		} catch(Exception e) {
			return false;
		}
	}
}
