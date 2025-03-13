package com.inspection.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class InquiryDTO {
    private Long inquiryId;
    private String inquiryTitle;
    private String inquiryContent;
    private LocalDateTime createdAt;
    private List<String> imageUrls;
    private String writerName;
    private Long writerId;
    private String contactNumber;
    private boolean processed;
    private String processContent;
    private String memo;
} 