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
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id", nullable = false)
    private ContractParticipant participant;
    
    private String pdfId;       // 참여자별 템플릿 PDF ID
    private String signedPdfId; // 서명 완료된 템플릿 PDF ID
    
    @Column(length = 100)
    private String serialNumber; // 서명 완료된 문서의 시리얼 넘버
    
    @Column(length = 255)
    private String documentPassword; // 암호화된 문서 비밀번호
    
    private boolean signed = false;
    private LocalDateTime signedAt;
    
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    // 재서명 관련 필드
    private boolean needsResign = false;           // 재서명 필요 여부
    private LocalDateTime resignRequestedAt;       // 재서명 요청 시간
    
    // 재서명 PDF 관련 필드
    private String resignedPdfId;                  // 재서명 완료된 PDF ID
    private LocalDateTime resignedAt;              // 재서명 완료 시간
} 