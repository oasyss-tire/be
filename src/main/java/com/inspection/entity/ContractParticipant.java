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
    
    private boolean signed;  // isSigned -> signed로 변경
    private LocalDateTime signedAt;         // 서명 일시
    
    private String pdfId;                   // 참여자별 서명용 PDF ID (첫 번째 템플릿의 PDF)
    private String signedPdfId;             // 서명 완료된 PDF ID (첫 번째 템플릿의 PDF)
    
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
        mapping.setPdfId(pdfId);
        mapping.setSignedPdfId(null); // 초기값은 null
        this.templateMappings.add(mapping);
    }
    
    public String getDecryptedEmail() {
        return encryptionUtil.decrypt(this.email);
    }
    
    public String getDecryptedPhoneNumber() {
        return encryptionUtil.decrypt(this.phoneNumber);
    }
} 