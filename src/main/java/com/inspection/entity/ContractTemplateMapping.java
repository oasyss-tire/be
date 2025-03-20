package com.inspection.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "contract_template_mappings")
@Getter @Setter
@NoArgsConstructor
public class ContractTemplateMapping {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private ContractTemplate template;
    
    private Integer sortOrder; // 템플릿 순서 (서명 순서)
    
    // 각 템플릿별 복사본 PDF ID 저장
    private String processedPdfId; // 처리된 PDF ID (템플릿의 복사본)
    
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    // 편의 메서드
    public static ContractTemplateMapping create(Contract contract, ContractTemplate template, int sortOrder, String processedPdfId) {
        ContractTemplateMapping mapping = new ContractTemplateMapping();
        mapping.setContract(contract);
        mapping.setTemplate(template);
        mapping.setSortOrder(sortOrder);
        mapping.setProcessedPdfId(processedPdfId);
        return mapping;
    }
} 