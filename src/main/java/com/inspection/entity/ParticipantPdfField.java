package com.inspection.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "participant_pdf_fields")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter
@NoArgsConstructor
public class ParticipantPdfField {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "participant_id", nullable = false)
    private ContractParticipant participant;
    
    @Column(nullable = false)
    private String pdfId;
    
    @Column(nullable = false)
    private String fieldId;
    
    private String fieldName;
    
    @Column(nullable = false)
    private String type;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "format_code_id")
    private Code format;
    
    @Column(name = "relativex", nullable = false)
    private Double relativeX;
    
    @Column(name = "relativey", nullable = false)
    private Double relativeY;
    
    @Column(name = "relative_width", nullable = false)
    private Double relativeWidth;
    
    @Column(name = "relative_height", nullable = false)
    private Double relativeHeight;
    
    @Column(nullable = false)
    private Integer page;
    
    @Column(columnDefinition = "LONGTEXT")
    private String confirmText;
    
    @Column(length = 500)
    private String description;
    
    @Column(columnDefinition = "LONGTEXT")
    private String value;
    
    private Boolean needsCorrection = false;
    
    private String correctionComment;
    
    private LocalDateTime correctionRequestedAt;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    @ManyToOne
    @JoinColumn(name = "template_id")
    private ContractTemplate template;
    
    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }
} 