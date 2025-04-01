package com.inspection.dto;

import lombok.Getter;
import lombok.Setter;
import com.inspection.enums.NotificationType;
import java.time.LocalDateTime;

@Getter @Setter
public class CreateParticipantRequest {
    private String name;                    // 참여자 이름
    private String email;                   // 이메일
    private String phoneNumber;             // 연락처
    private NotificationType notifyType;    // 발송방법
    private boolean signed;
    private LocalDateTime signedAt;
    private String pdfId;                   // PDF ID
    private String signedPdfId;             // 서명된 PDF ID
    private Long userId;     // 연결할 사용자 ID (PK)
} 