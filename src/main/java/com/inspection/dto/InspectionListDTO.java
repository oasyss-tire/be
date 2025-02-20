package com.inspection.dto;

import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class InspectionListDTO {
    private Long inspectionId; // 점검 아이디
    private Long companyId;    // 업체명
    private LocalDate inspectionDate; // 점검일
    private String managerName; // 담당자명
} 