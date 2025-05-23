package com.inspection.facility.dto;

import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class FacilitySearchRequest {
    
    private String search;                 // 통합 검색 (관리번호, 시리얼번호, 매장명, 품목, 시설물유형 중 하나라도 일치하는 항목)
    private String keyword;                // 기존 키워드 검색 (모델번호, 시리얼번호, 관리번호에 적용)
    private String managementNumber;       // 관리번호 검색
    private String serialNumber;           // 시리얼번호 검색
    private String brandCode;              // 브랜드 코드
    private String facilityTypeCode;       // 시설물 유형 코드
    private String statusCode;             // 상태 코드
    private String installationTypeCode;   // 설치 유형 코드
    private String location;               // 위치 검색 (주소 기준)
    private Long companyId;                // 회사 ID (위치 회사 기준으로만 검색)
    private Boolean isActive;              // 활성 상태 여부
    
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime installationStartDate;  // 설치일 시작
    
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime installationEndDate;    // 설치일 종료
    
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime warrantyStartDate;      // A/S 보증기간 시작
    
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime warrantyEndDate;        // A/S 보증기간 종료
} 