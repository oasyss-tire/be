package com.inspection.dto;

import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InspectionBoardDTO {
    private Long inspectionId; // 점검 아이디
    private String companyName;    // 업체명 
    private LocalDate inspectionDate; // 점검일
    private String managerName; // 담당자명
    private Long companyId;    // 업체 아이디(번호)
} 