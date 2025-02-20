package com.inspection.dto;

import java.time.LocalDate;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class FireSafetyInspectionDTO {
    private Long fireInspectionId;
    private Long writerId;
    private String buildingName;
    private LocalDate inspectionDate;
    private String address;
    private Long companyId;
    private String buildingGrade;
    
    // 점검 상태
    private String fireExtinguisherStatus;
    private String fireAlarmStatus;
    private String fireEvacuationStatus;
    private String fireWaterStatus;
    private String fireFightingStatus;
    
    private String etcComment;
    private String inspectorSignature;
    private String managerSignature;
    private List<String> attachments;
    
    // 추가 정보 (조회시에만 사용)
    private String writerName;
    private String companyName;
    private LocalDate createdAt;
    private LocalDate updatedAt;
    private String phoneNumber;
} 