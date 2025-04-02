package com.inspection.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.inspection.dto.KakaoAlertDTO;
import com.inspection.dto.KakaoAlertRequestDTO;
import com.inspection.dto.TokenRequestDTO;
import com.inspection.security.JwtTokenProvider;
import com.inspection.service.KakaoAlertService;
import com.inspection.service.ParticipantTokenService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/kakao-alert")
@RequiredArgsConstructor
@Slf4j
public class KakaoAlertController {

    private final KakaoAlertService kakaoAlertService;
    private final JwtTokenProvider jwtTokenProvider;
    private final ParticipantTokenService participantTokenService;


    @PostMapping("/contract-signature")
    public ResponseEntity<?> sendContractSignatureAlert(
        @RequestBody KakaoAlertRequestDTO request,
        @RequestHeader("Authorization") String authHeader
    ) {
        try {
            log.info("Sending contract signature alert to: {}", request.getPhoneNumber());
            
            String token = authHeader.substring(7);
            Long userId = jwtTokenProvider.getUserIdFromToken(token);
            
            kakaoAlertService.sendAlert(
                request.getPhoneNumber(), 
                request.getName(), 
                request.getTitle(), 
                request.getRequester(), 
                request.getContractDate(), 
                request.getUrl(), 
                userId
            );
            return ResponseEntity.ok().body("알림톡 전송 성공");
        } catch (Exception e) {
            log.error("Failed to send contract signature alert: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("알림톡 전송 실패: " + e.getMessage());
        }
    }
    
    /**
     * 계약 참여자를 위한 카카오 알림톡 서명 토큰을 생성합니다.
     * 생성된 토큰은 24시간 동안 유효합니다.
     * 
     * @param request 토큰 요청 DTO (참여자 ID 포함)
     * @param authHeader 인증 헤더
     * @return 생성된 토큰
     */
    @PostMapping("/generate-token")
    public ResponseEntity<?> generateSignatureToken(
        @RequestBody TokenRequestDTO request,
        @RequestHeader("Authorization") String authHeader
    ) {
        try {
            log.info("Generating kakao signature token for participant ID: {}", request.getParticipantId());
            
            // 카카오 알림톡용 토큰 생성
            String generatedToken = participantTokenService.generateKakaoSignatureToken(request.getParticipantId());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "token", generatedToken,
                "message", "카카오 알림톡 서명 토큰이 성공적으로 생성되었습니다"
            ));
        } catch (Exception e) {
            log.error("Failed to generate kakao signature token: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "message", "카카오 알림톡 서명 토큰 생성 실패: " + e.getMessage()
                ));
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