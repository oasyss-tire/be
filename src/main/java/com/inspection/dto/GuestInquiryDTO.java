package com.inspection.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter @Setter
public class GuestInquiryDTO {
    private Long id;
    private String guestName;
    private String phoneNumber;
    private String password;  // 요청 시에만 사용
    private String content;
    private String answer;
    private LocalDateTime answerTime;
    private Boolean answered;
    private LocalDateTime createdAt;
} 