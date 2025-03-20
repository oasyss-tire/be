package com.inspection.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "participant_template_mappings")
@Getter @Setter
@NoArgsConstructor
public class ParticipantTemplateMapping {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_template_mapping_id", nullable = false)
    private ContractTemplateMapping contractTemplateMapping;
    
    private String pdfId;       // 참여자별 템플릿 PDF ID
    private String signedPdfId; // 서명 완료된 템플릿 PDF ID
    
    private boolean signed = false;
    private LocalDateTime signedAt;
    
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
} 