package com.inspection.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.inspection.service.KakaoAlertService;
import com.inspection.dto.KakaoAlertRequestDTO;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import io.jsonwebtoken.Jwt;
import com.inspection.security.JwtTokenProvider;
import com.inspection.dto.KakaoAlertDTO;
import java.util.List;

@RestController
@RequestMapping("/api/kakao-alert")
@RequiredArgsConstructor
@Slf4j
public class KakaoAlertController {

    private final KakaoAlertService kakaoAlertService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/inspection/{id}")
    public ResponseEntity<?> sendInspectionAlert(
        @PathVariable Long id,
        @RequestBody KakaoAlertRequestDTO request,
        @RequestHeader("Authorization") String authHeader
    ) {
        try {
            log.info("Sending inspection result alert to: {}", request.getPhoneNumber());
            String url = String.format("safe.jebee.net/inspection/%d", id);
            
            // Bearer 토큰에서 실제 토큰 추출
            String token = authHeader.substring(7);  // "Bearer " 제거
            Long userId = jwtTokenProvider.getUserIdFromToken(token);
            
            kakaoAlertService.sendAlert(request.getPhoneNumber(), url, userId);
            return ResponseEntity.ok().body("알림톡 전송 성공");
        } catch (Exception e) {
            log.error("Failed to send inspection alert: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("알림톡 전송 실패: " + e.getMessage());
        }
    }


    @PostMapping("/fire-safety-inspection/{id}")
    public ResponseEntity<?> sendFireSafetyInspectionAlert(
        @PathVariable Long id,
        @RequestBody KakaoAlertRequestDTO request,
        @RequestHeader("Authorization") String authHeader
    ) {
        try {
            log.info("Sending fire safety inspection result alert to: {}", request.getPhoneNumber());
            String url = String.format("safe.jebee.net/fire-safety-inspection/%d", id);
            
            String token = authHeader.substring(7);
            Long userId = jwtTokenProvider.getUserIdFromToken(token);
            
            kakaoAlertService.sendAlert(request.getPhoneNumber(), url, userId);
            return ResponseEntity.ok().body("알림톡 전송 성공");
        } catch (Exception e) {
            log.error("Failed to send fire safety inspection alert: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping
    public ResponseEntity<List<KakaoAlertDTO>> getAllAlerts() {
        List<KakaoAlertDTO> alerts = kakaoAlertService.getAllAlerts();
        return ResponseEntity.ok(alerts);
    }

    @GetMapping("/{alertId}")
    public ResponseEntity<KakaoAlertDTO> getAlertById(@PathVariable Long alertId) {
        KakaoAlertDTO alert = kakaoAlertService.getAlertById(alertId);
        return ResponseEntity.ok(alert);
    }
} 