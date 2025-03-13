package com.inspection.dto;

import com.inspection.entity.ContractParticipant;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import com.inspection.enums.NotificationType;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantDetailDTO {
    private Long id;
    private String name;
    private String email;
    private String phoneNumber;
    private NotificationType notifyType;
    private boolean signed;
    private LocalDateTime signedAt;
    private String pdfId;
    private String signedPdfId;
    
    public ParticipantDetailDTO(ContractParticipant participant) {
        this.id = participant.getId();
        this.name = participant.getName();
        this.email = participant.getEmail();
        this.phoneNumber = participant.getPhoneNumber();
        this.notifyType = participant.getNotifyType();
        this.signed = participant.isSigned();
        this.signedAt = participant.getSignedAt();
        this.pdfId = participant.getPdfId();
        this.signedPdfId = participant.getSignedPdfId();
    }
} 