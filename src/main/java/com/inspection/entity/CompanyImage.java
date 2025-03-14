package com.inspection.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "company_images")
@Getter @Setter
public class CompanyImage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;
    
    @Column(length = 255)
    private String frontImage;      // 회사 정면사진
    
    @Column(length = 255)
    private String backImage;       // 회사 후면사진
    
    @Column(length = 255)
    private String leftSideImage;   // 회사 측면(좌)사진
    
    @Column(length = 255)
    private String rightSideImage;  // 회사 측면(우)사진
    
    @Column(length = 255)
    private String fullImage;       // 회사 전체사진
} 