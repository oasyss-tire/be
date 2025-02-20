package com.inspection.dto;

import com.inspection.entity.ContractStatus;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@Builder
public class ContractResponse {
    private Long id;
    private String title;
    
    // 피계약자 정보
    private String contracteeName;
    private String contracteeEmail;
    private String contracteePhoneNumber;
    
    // 계약자 정보
    private String contractorName;
    private String contractorEmail;
    private String contractorPhoneNumber;
    
    private String contractType;
    private String description;
    private String pdfUrl;
    private String signedPdfUrl;
    private ContractStatus status;
    private String originalFileName;
    private Long fileSize;
    private LocalDateTime expirationDate;
    private LocalDateTime signedDate;
    private LocalDateTime createdDate;
    private String contractNumber;
    private boolean isEmailSent;
    private LocalDateTime emailSentDate;
    
} 