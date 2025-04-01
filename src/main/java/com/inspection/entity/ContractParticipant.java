package com.inspection.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.inspection.enums.NotificationType;
import com.inspection.util.EncryptionUtil;

@Entity
@Getter @Setter
@NoArgsConstructor
public class ContractParticipant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;                    // 참여자 이름
    private String email;                   // 이메일
    private String phoneNumber;             // 전화번호
    
    @Enumerated(EnumType.STRING)
    private NotificationType notifyType;    // 발송방법 (EMAIL, SMS, KAKAO)
    
    // 참여자 상태 코드 (코드로 관리)
    @ManyToOne
    @JoinColumn(name = "status_code_id")
    private Code statusCode;               // 참여자 상태 코드 (서명대기/서명중/승인대기/승인완료/승인거부)
    
    // 연결된 User 정보 (사용자가 로그인한 경우)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;                     // 연결된 사용자 (null 가능)
    
    // 기존 필드 유지 (호환성)
    private boolean signed;                // 서명 여부 (호환성 유지)
    private LocalDateTime signedAt;         // 서명 일시
    
    private String pdfId;                   // 참여자별 서명용 PDF ID (첫 번째 템플릿의 PDF)
    private String signedPdfId;             // 서명 완료된 PDF ID (첫 번째 템플릿의 PDF)
    
    // 관리자 승인 관련 필드 (호환성 유지)
    private boolean approved;              // 승인 여부
    private LocalDateTime approvedAt;      // 승인 일시
    private String approvalComment;        // 승인 코멘트
    private String rejectionReason;        // 거부 사유
    
    // 재서명 관련 필드
    private String resignRequestReason;     // 재서명 요청 이유
    private LocalDateTime resignRequestedAt; // 재서명 요청 시간
    private String resignApprovedBy;        // 재서명 승인자
    private LocalDateTime resignApprovedAt; // 재서명 승인 시간
    
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "participant_id")
    private List<ParticipantTemplateMapping> templateMappings = new ArrayList<>();
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    private Contract contract;             // 연관된 계약
    
    @Transient  // DB에 저장되지 않는 필드
    private EncryptionUtil encryptionUtil;
    
    public void addTemplateMapping(ContractTemplateMapping contractTemplateMapping, String pdfId) {
        ParticipantTemplateMapping mapping = new ParticipantTemplateMapping();
        mapping.setContractTemplateMapping(contractTemplateMapping);
        mapping.setParticipant(this); // 참여자 객체 명시적으로 설정
        mapping.setPdfId(pdfId);
        mapping.setSignedPdfId(null); // 초기값은 null
        mapping.setCreatedAt(LocalDateTime.now()); // 생성 시간 설정
        this.templateMappings.add(mapping);
    }
    
    public String getDecryptedEmail() {
        return encryptionUtil.decrypt(this.email);
    }
    
    public String getDecryptedPhoneNumber() {
        return encryptionUtil.decrypt(this.phoneNumber);
    }
} 