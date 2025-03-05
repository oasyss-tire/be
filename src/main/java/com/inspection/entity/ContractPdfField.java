package com.inspection.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

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
    private String value;
    
    private String fieldName;      // 필드 식별 이름

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @ManyToOne
    @JoinColumn(name = "template_id")
    private ContractTemplate template;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
} 