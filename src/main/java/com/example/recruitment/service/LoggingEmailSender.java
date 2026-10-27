package com.example.recruitment.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Triển khai dùng khi phát triển: ghi nội dung email ra log thay vì gửi thật.
 * Không dùng trên môi trường triển khai, vì log sẽ chứa link có token.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.mail.mode", havingValue = "log", matchIfMissing = true)
public class LoggingEmailSender implements EmailSender {

	@Override
	public void send(String to, String subject, String body) {
		log.info("[EMAIL] to={} subject={}\n{}", to, subject, body);
	}
}
