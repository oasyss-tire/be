package com.inspection.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.inspection.service.SMSService;

@RestController
@RequestMapping("/api/sms")
@RequiredArgsConstructor
@Slf4j
public class SMSController {
    
    private final SMSService smsService;

    @PostMapping("/send")
    public ResponseEntity<String> sendSMS(
            @RequestParam String name,
            @RequestParam String phone,
            @RequestParam String content) {
        try {
            String response = smsService.sendSMS(name, phone, content);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("SMS 발송 실패: ", e);
            return ResponseEntity.internalServerError().body("SMS 발송에 실패했습니다.");
        }
    }
} 