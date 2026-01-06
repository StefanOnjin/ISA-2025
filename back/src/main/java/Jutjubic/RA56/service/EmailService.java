package Jutjubic.RA56.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
	private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

	private final JavaMailSender mailSender;
	private final String fromAddress;

	public EmailService(JavaMailSender mailSender,
			@Value("${spring.mail.username:}") String fromAddress) {
		this.mailSender = mailSender;
		this.fromAddress = fromAddress;
	}

	@Async
	public void sendActivationEmail(String to, String username, String activationLink) {
		SimpleMailMessage message = new SimpleMailMessage();
		message.setTo(to);
		if (fromAddress != null && !fromAddress.isBlank()) {
			message.setFrom(fromAddress);
		}
		message.setSubject("Activate your account");
		message.setText("Hello " + username + ",\n\n" +
				"Please activate your account by visiting the link below:\n" +
				activationLink + "\n\n" +
				"If you did not request this, you can ignore this email.");

		try {
			mailSender.send(message);
		} catch (Exception ex) {
			logger.warn("Failed to send activation email to {}", to, ex);
		}
	}
}
