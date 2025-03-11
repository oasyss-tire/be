package com.inspection.entity;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Getter @Setter
@NoArgsConstructor
public class ContractTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String templateName;        // 템플릿 이름
    private String description;         // 템플릿 설명
    private String originalPdfId;       // 원본 PDF ID
    private String processedPdfId;      // 서명 영역이 지정된 PDF ID
    private LocalDateTime createdAt;
    private boolean isActive;
    
    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL)
    private List<ContractPdfField> fields;  // ContractPdfField의 template 필드와 매핑
    
    // 편의 메서드 수정
    public void addFields(List<ContractPdfField> fields) {
        this.fields = fields;
        fields.forEach(field -> field.setTemplate(this));  // 양방향 관계 설정
    }
} 