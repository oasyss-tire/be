package com.inspection.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class GuestInquiryListDTO {
    private Long id;
    private String guestName;
    private String content;
    private Boolean answered;
    private LocalDateTime createdAt;
    
    // 민감한 정보(전화번호, 답변내용 등)는 제외
} 