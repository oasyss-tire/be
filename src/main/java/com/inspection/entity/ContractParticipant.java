package com.inspection.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import com.inspection.enums.NotificationType;

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
    
    private String pdfId;                   // 참여자별 서명용 PDF ID
    private String signedPdfId;             // 서명 완료된 PDF ID
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    private Contract contract;             // 연관된 계약
} 