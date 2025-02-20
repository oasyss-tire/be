package com.inspection.controller;

import com.inspection.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
public class EmailController {

    private final EmailService emailService;

    @PostMapping("/send")
    public ResponseEntity<String> sendEmail(
            @RequestParam String to,
            @RequestParam String subject,
            @RequestParam String content) {
        try {
            log.info("이메일 전송 요청 수신 - 수신자: {}, 제목: {}", to, subject);
            emailService.sendSimpleMessage(to, subject, content);
            return ResponseEntity.ok("이메일 발송 성공");
        } catch (Exception e) {
            log.error("이메일 전송 컨트롤러 에러", e);
            return ResponseEntity.internalServerError()
                .body("이메일 발송 실패: " + e.getMessage());
        }
    }

    @PostMapping("/send-html")
    public ResponseEntity<String> sendHtmlEmail(
            @RequestParam String to,
            @RequestParam String subject,
            @RequestParam String content) {
        try {
            emailService.sendHtmlMessage(to, subject, content);
            return ResponseEntity.ok("HTML 이메일 발송 성공");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("이메일 발송 실패: " + e.getMessage());
        }
    }

    @PostMapping("/send-with-attachment")
    public ResponseEntity<String> sendEmailWithAttachment(
            @RequestParam String to,
            @RequestParam String subject,
            @RequestParam String content,
            @RequestParam MultipartFile attachment) {
        try {
            log.info("첨부파일 이메일 전송 시도 - 수신자: {}, 제목: {}, 파일명: {}", 
                to, subject, attachment.getOriginalFilename());
            
            emailService.sendMessageWithAttachment(to, subject, content, attachment);
            return ResponseEntity.ok("이메일 발송 성공");
        } catch (Exception e) {
            log.error("이메일 전송 실패", e);
            return ResponseEntity.internalServerError()
                .body("이메일 발송 실패: " + e.getMessage());
        }
    }
} 