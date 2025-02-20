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
public class Inquiry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long inquiryId;        // 문의사항 번호
    
    private String inquiryTitle;   // 문의사항 제목
    
    @Column(columnDefinition = "TEXT")
    private String inquiryContent; // 문의사항 내용
    
    private LocalDateTime createdAt;    // 작성시간
    
    @ElementCollection
    @CollectionTable(name = "inquiry_images", 
        joinColumns = @JoinColumn(name = "inquiry_id"))
    @Column(name = "image_url")
    private List<String> imageUrls = new ArrayList<>();  // 첨부파일(이미지)
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User writer;           // 작성자
    
    private String contactNumber;  // 작성자 연락처
    
    private boolean processed;     // 처리여부
    
    @Column(columnDefinition = "TEXT")
    private String processContent; // 처리내용
    
    @Column(columnDefinition = "TEXT")
    private String memo;          // 메모
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
} 