package com.inspection.entity;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
public class FacilityImage extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_id")
    private Facility facility;
    
    private String imageUrl;           // 이미지 경로
    private String imageType;          // 이미지 종류(시설물/계약서/기타)
    private String description;        // 이미지 설명
    private LocalDate captureDate;     // 촬영일자
    private String fileName;           // 파일명
    private Long fileSize;            // 파일크기
} 