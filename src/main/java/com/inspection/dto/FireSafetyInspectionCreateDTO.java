package com.inspection.dto;

import java.time.LocalDate;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class FireSafetyInspectionCreateDTO {
    private Long writerId;
    private String buildingName;
    private LocalDate inspectionDate;
    private String address;
    private Long companyId;
    private String buildingGrade;
    private String fireExtinguisherStatus;
    private String fireAlarmStatus;
    private String fireEvacuationStatus;
    private String fireWaterStatus;
    private String fireFightingStatus;
    private String etcComment;
    private String inspectorSignature;
    private String managerSignature;
    private List<String> attachments;
} 