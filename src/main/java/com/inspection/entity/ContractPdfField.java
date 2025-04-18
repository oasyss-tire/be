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
@Table(name = "contract_pdf_fields")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter
@NoArgsConstructor
public class ContractPdfField {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fieldId;        // 프론트엔드에서 생성한 ID

    @Column(nullable = false)
    private String pdfId;          // PDF 문서 식별자

    @Column(nullable = false)
    private String type;           // 필드 타입 (signature, text 등)

    // 입력 형식 코드와 연결
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "format_code_id")
    private Code format;          // 필드 입력 형식 (전화번호, 주민번호 등)

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
    private String confirmText;    // 따라 작성해야 하는 원본 텍스트
    
    @Column(length = 500)
    private String description;    // 필드에 대한 설명 (용도, 작성 지침 등)
    
    @Column(columnDefinition = "LONGTEXT")
    private String value;
    
    private String fieldName;      // 필드 식별 이름

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @ManyToOne
    @JoinColumn(name = "template_id")  // 실제 DB 컬럼명
    private ContractTemplate template;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

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