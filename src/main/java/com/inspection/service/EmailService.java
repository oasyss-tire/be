package com.inspection.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {
    
    private final JavaMailSender emailSender;
    
    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendSimpleMessage(String to, String subject, String text) {
        try {
            log.info("이메일 전송 시도 - 수신자: {}, 제목: {}", to, subject);
            
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, false);
            
            emailSender.send(message);
            log.info("이메일 전송 성공");
        } catch (Exception e) {
            log.error("이메일 전송 실패: {}", e.getMessage(), e);
            throw new RuntimeException("이메일 전송 실패: " + e.getMessage(), e);
        }
    }

    public void sendHtmlMessage(String to, String subject, String htmlContent) {
        try {
            log.info("HTML 이메일 전송 시도 - 수신자: {}, 제목: {}", to, subject);
            
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            
            emailSender.send(message);
            log.info("HTML 이메일 전송 성공");
        } catch (Exception e) {
            log.error("HTML 이메일 전송 실패: {}", e.getMessage(), e);
            throw new RuntimeException("이메일 전송 실패: " + e.getMessage(), e);
        }
    }

    public void sendMessageWithAttachment(
            String to, 
            String subject, 
            String content,
            MultipartFile attachment) {
        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);  // HTML 지원
            
            if (attachment != null && !attachment.isEmpty()) {
                helper.addAttachment(
                    attachment.getOriginalFilename(),
                    new ByteArrayResource(attachment.getBytes())
                );
            }
            
            emailSender.send(message);
            log.info("첨부파일 이메일 전송 성공 - 파일명: {}", attachment.getOriginalFilename());
        } catch (Exception e) {
            log.error("첨부파일 이메일 전송 실패", e);
            throw new RuntimeException("이메일 전송 실패: " + e.getMessage(), e);
        }
    }
} 