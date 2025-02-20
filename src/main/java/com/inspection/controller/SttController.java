package com.inspection.controller;

import java.io.IOException;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.inspection.service.SttService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/stt")
@RequiredArgsConstructor
public class SttController {

    private final SttService sttService;

    @PostMapping("/convert")
    public ResponseEntity<String> convertSpeechToText(
            @RequestParam("audio") MultipartFile audioFile) {
        try {
            log.info("음성 파일 수신. 크기: {} bytes, ContentType: {}", 
                audioFile.getSize(), 
                audioFile.getContentType());
            
            if (audioFile.isEmpty()) {
                return ResponseEntity.badRequest().body("음성 파일이 비어있습니다.");
            }

            String text = sttService.convertSpeechToText(audioFile);
            log.info("변환된 텍스트: {}", text);
            return ResponseEntity.ok(text);
        } catch (IOException e) {
            log.error("STT 변환 실패", e);
            return ResponseEntity.internalServerError()
                .body("음성 인식 서비스 연결에 실패했습니다: " + e.getMessage());
        }
    }
}