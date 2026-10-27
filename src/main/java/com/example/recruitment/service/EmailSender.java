package com.example.recruitment.service;

/** Gửi email hệ thống. Có thể thay bằng triển khai dùng SMTP mà không ảnh hưởng nghiệp vụ. */
public interface EmailSender {

	void send(String to, String subject, String body);
}
