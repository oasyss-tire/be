package com.inspection.dto;

import lombok.Getter;
import lombok.Setter;
import com.inspection.entity.ContractParticipant;
import com.inspection.enums.NotificationType;
import java.time.LocalDateTime;

@Getter @Setter
public class ParticipantDTO {
    private Long id;
    private String name;
    private String email;
    private String phoneNumber;
    private NotificationType notifyType;
    private boolean signed;
    private LocalDateTime signedAt;
    private String pdfId;
    private String signedPdfId;

    public ParticipantDTO(ContractParticipant participant) {
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