package com.inspection.dto;

import java.time.LocalDate;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Getter @Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class FireSafetyInspectionUpdateDTO {
    private Long fireInspectionId;
    private Long writerId;
    private Long companyId;
    private String writerName;
    private String companyName;
    
    private String buildingName;
    private LocalDate inspectionDate;
    private String address;
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
    private LocalDate createdAt;
    private LocalDate updatedAt;
} 