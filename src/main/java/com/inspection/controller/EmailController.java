package com.inspection.controller;

import com.inspection.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
public class EmailController {

    private final EmailService emailService;
    
    @Value("${frontend.base-url}")
    private String frontendBaseUrl;

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

    /**
     * 계약 참여자에게 토큰이 포함된 서명 요청 이메일을 발송합니다.
     * 기존 send-html 엔드포인트와 달리, 토큰을 자동으로 생성하고 포함합니다.
     */
    @PostMapping("/send-signature-request")
    public ResponseEntity<?> sendSignatureRequestEmail(
            @RequestParam Long participantId,
            @RequestParam String to,
            @RequestParam String name,
            @RequestParam String contractTitle,
            @RequestParam(required = false) String baseUrl) {
        try {
            log.info("서명 요청 이메일 전송 시도 - 참여자ID: {}, 수신자: {}", participantId, to);
            
            // baseUrl이 제공되지 않은 경우 환경변수 값 사용
            String finalBaseUrl = (baseUrl != null && !baseUrl.isEmpty()) ? baseUrl : frontendBaseUrl;
            
            String token = emailService.sendSignatureRequestWithToken(
                participantId, 
                to, 
                name,
                contractTitle, 
                finalBaseUrl
            );
            
            return ResponseEntity.ok(
                Map.of(
                    "success", true, 
                    "message", "서명 요청 이메일 발송 성공",
                    "token", token
                )
            );
        } catch (Exception e) {
            log.error("서명 요청 이메일 전송 실패", e);
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "success", false,
                    "message", "서명 요청 이메일 전송 실패: " + e.getMessage()
                ));
        }
    }
} 