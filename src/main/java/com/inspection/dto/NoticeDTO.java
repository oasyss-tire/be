package com.inspection.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class NoticeDTO {
    private Long noticeId;
    private String title;
    private String content;
    private String writerName;
    private Long writerId;
    private LocalDateTime createdAt;
    private boolean popup;
    private LocalDateTime popupStartDate;
    private LocalDateTime popupEndDate;
    private List<String> imageUrls;
} 