package com.inspection.dto;

import com.inspection.entity.ContractParticipant;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import com.inspection.enums.NotificationType;

@Getter @Setter
public class ParticipantDetailDTO {
    private Long id;
    private String name;
    private String email;
    private String phoneNumber;
    private String pdfId;
    private String signedPdfId;
    private boolean signed;
    private LocalDateTime signedAt;
    private NotificationType notifyType;
    
    public ParticipantDetailDTO(ContractParticipant participant) {
        this.id = participant.getId();
        this.name = participant.getName();
        this.email = participant.getEmail();
        this.phoneNumber = participant.getPhoneNumber();
        this.pdfId = participant.getPdfId();
        this.signedPdfId = participant.getSignedPdfId();
        this.signed = participant.isSigned();
        this.signedAt = participant.getSignedAt();
        this.notifyType = participant.getNotifyType();
    }
} 