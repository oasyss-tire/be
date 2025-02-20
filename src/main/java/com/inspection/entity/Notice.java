package com.inspection.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
public class Notice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long noticeId;
    
    private String title;           // 제목
    
    @Column(columnDefinition = "TEXT")
    private String content;         // 내용
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User writer;            // 작성자
    
    private LocalDateTime createdAt;    // 작성일시
    private boolean popup;              // 팝업 여부
    private LocalDateTime popupStartDate; // 팝업 시작일
    private LocalDateTime popupEndDate;   // 팝업 종료일
    
    @ElementCollection
    @CollectionTable(name = "notice_images", 
        joinColumns = @JoinColumn(name = "notice_id"))
    @Column(name = "image_url")
    private List<String> imageUrls = new ArrayList<>();
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
} 