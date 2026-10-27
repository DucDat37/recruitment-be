package com.example.recruitment.service;

import com.example.recruitment.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Gửi email thật qua SMTP (Gmail). Khi gửi lỗi, ném ApiException để transaction được rollback,
 * tránh tình trạng tài khoản đã tạo nhưng người dùng không nhận được token xác thực.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.mail.mode", havingValue = "smtp")
public class SmtpEmailSender implements EmailSender {

	private final JavaMailSender mailSender;
	private final String from;

	public SmtpEmailSender(JavaMailSender mailSender, @Value("${spring.mail.username}") String from) {
		this.mailSender = mailSender;
		this.from = from;
	}

	@Override
	public void send(String to, String subject, String body) {
		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom(from);
		message.setTo(to);
		message.setSubject(subject);
		message.setText(body);
		try {
			mailSender.send(message);
		} catch (MailException ex) {
			log.error("Không gửi được email tới {}: {}", to, ex.getMessage());
			throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "MAIL_SEND_FAILED",
					"Không gửi được email, vui lòng thử lại sau");
		}
	}
}
