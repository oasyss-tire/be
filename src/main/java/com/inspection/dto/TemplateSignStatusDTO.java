package com.inspection.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class TemplateSignStatusDTO {
    private Long mappingId;         // 템플릿 매핑 ID
    private String pdfId;           // 원본 PDF ID
    private String signedPdfId;     // 서명 완료된 PDF ID
    private boolean signed;         // 서명 완료 여부
    private LocalDateTime signedAt; // 서명 완료 시간
    private String templateName;    // 템플릿 이름
} 