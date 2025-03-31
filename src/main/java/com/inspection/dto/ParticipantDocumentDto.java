package com.inspection.dto;

import com.inspection.entity.ParticipantDocument;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantDocumentDto {
    private Long id;
    private Long contractId;
    private Long participantId;
    private String participantName;
    private String documentCodeId;
    private String documentCodeName;
    private String fileId;
    private String originalFileName;
    private boolean required;
    private LocalDateTime uploadedAt;
    private LocalDateTime createdAt;
    
    /**
     * ParticipantDocument 엔티티로부터 DTO 생성
     */
    public static ParticipantDocumentDto fromEntity(ParticipantDocument entity) {
        return ParticipantDocumentDto.builder()
                .id(entity.getId())
                .contractId(entity.getContract() != null ? entity.getContract().getId() : null)
                .participantId(entity.getParticipant() != null ? entity.getParticipant().getId() : null)
                .participantName(entity.getParticipant() != null ? entity.getParticipant().getName() : null)
                .documentCodeId(entity.getDocumentCode() != null ? entity.getDocumentCode().getCodeId() : null)
                .documentCodeName(entity.getDocumentCode() != null ? entity.getDocumentCode().getCodeName() : null)
                .fileId(entity.getFileId())
                .originalFileName(entity.getOriginalFileName())
                .required(entity.isRequired())
                .uploadedAt(entity.getUploadedAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }
} 