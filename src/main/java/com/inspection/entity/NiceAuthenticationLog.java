package com.inspection.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * NICE 본인인증 이력 관리 엔티티
 * 개인정보는 저장하지 않고 인증 메타데이터와 CI만 저장
 */
@Entity
@Table(name = "nice_authentication_log")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NiceAuthenticationLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // 비즈니스 연결 정보
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;                    // 관련 계약
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_participant_id", nullable = false)
    private ContractParticipant participant;     // 관련 계약 참여자
    
    // NICE 인증 정보 (개인정보 제외, 메타데이터만)
    @Column(name = "request_no", length = 30, nullable = false)
    private String requestNo;                    // 요청 고유번호
    
    @Column(name = "response_no", length = 24, nullable = false)
    private String responseNo;                   // 응답 고유번호
    
    @Column(name = "result_code", length = 4, nullable = false)
    private String resultCode;                   // 결과코드 (0000=성공)
    
    @Column(name = "auth_type", length = 1, nullable = false)
    private String authType;                     // 인증수단 (M:휴대폰, X:공인인증서, C:카드, U:금융인증서)
    
    @Column(name = "enc_time", length = 14, nullable = false)
    private String encTime;                      // NICE 인증일시 (YYYYMMDDHHmmss)
    
    @Column(name = "ci", length = 88, nullable = true)
    private String ci;                           // 개인식별코드 (중복확인/매칭용, 개인정보 아님)
    
    // 추가 메타데이터
    @Column(name = "mobile_co", length = 1, nullable = true)
    private String mobileCo;                     // 통신사 코드 (1:SKT, 2:KT, 3:LGU+)
    
    @Column(name = "national_info", length = 1, nullable = true)
    private String nationalInfo;                 // 내외국인 구분 (0:내국인, 1:외국인)
    
    // 보안 및 추적 정보
    @Column(name = "ip_address", length = 45)
    private String ipAddress;                    // 접속 IP 주소 (IPv6 지원)
    
    @Column(name = "user_agent", length = 500)
    private String userAgent;                    // 브라우저 정보
    
    @Column(name = "auth_purpose", length = 50)
    private String authPurpose;                  // 인증 목적 (CONTRACT_SIGN, DOCUMENT_VIEW 등)
    
    // 시간 정보
    @Column(name = "authenticated_at", nullable = false)
    private LocalDateTime authenticatedAt;       // 실제 NICE 인증 완료 시간
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;             // 로그 생성 시간
    
    // 편의 메서드
    
    /**
     * 인증 성공 여부 확인
     */
    public boolean isAuthenticationSuccess() {
        return "0000".equals(this.resultCode);
    }
    
    /**
     * 휴대폰 인증 여부 확인
     */
    public boolean isMobileAuth() {
        return "M".equals(this.authType);
    }
    
    /**
     * 내국인 여부 확인
     */
    public boolean isKorean() {
        return "0".equals(this.nationalInfo);
    }
    
    /**
     * 통신사 이름 반환
     */
    public String getMobileCarrierName() {
        if (mobileCo == null) return null;
        return switch (mobileCo) {
            case "1" -> "SKT";
            case "2" -> "KT";
            case "3" -> "LGU+";
            case "5" -> "SKT 알뜰폰";
            case "6" -> "KT 알뜰폰";
            case "7" -> "LGU+ 알뜰폰";
            default -> "기타";
        };
    }
    
    /**
     * 정적 팩토리 메서드 - NICE 인증 결과로부터 생성
     */
    public static NiceAuthenticationLog from(
            Contract contract,
            ContractParticipant participant,
            com.inspection.nice.dto.NiceCertificationResultDto niceResult,
            String ipAddress,
            String userAgent,
            String authPurpose) {
        
        return NiceAuthenticationLog.builder()
                .contract(contract)
                .participant(participant)
                .requestNo(niceResult.getRequestNo())
                .responseNo(niceResult.getResponseNo())
                .resultCode(niceResult.getResultCode())
                .authType(niceResult.getAuthType())
                .encTime(niceResult.getEncTime())
                .ci(niceResult.getCi())
                .mobileCo(niceResult.getMobileCo())
                .nationalInfo(niceResult.getNationalInfo())
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .authPurpose(authPurpose)
                .authenticatedAt(parseNiceEncTime(niceResult.getEncTime()))
                .build();
    }
    
    /**
     * NICE encTime을 LocalDateTime으로 변환
     */
    private static LocalDateTime parseNiceEncTime(String encTime) {
        if (encTime == null || encTime.length() != 14) {
            return LocalDateTime.now();
        }
        try {
            int year = Integer.parseInt(encTime.substring(0, 4));
            int month = Integer.parseInt(encTime.substring(4, 6));
            int day = Integer.parseInt(encTime.substring(6, 8));
            int hour = Integer.parseInt(encTime.substring(8, 10));
            int minute = Integer.parseInt(encTime.substring(10, 12));
            int second = Integer.parseInt(encTime.substring(12, 14));
            return LocalDateTime.of(year, month, day, hour, minute, second);
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }
} 