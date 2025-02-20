package com.inspection.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class FireCheckInspectionDTO {
    private Long fireCheckInspectionId;
    private String targetName;
    private String unitNumber;
    private LocalDate inspectionDate;
    private String inspectorName;
    private String phoneNumber;
    private String inspectorSignature;
    private String companySignature;
    
    // 소화기 정보
    private String fireExtinguisherInstalled;
    private String fireExtinguisherDamage;
    private String fireExtinguisherSafetyPin;
    private String fireExtinguisherPressureGauge;
    private String fireExtinguisherContentAgeValid;
    private String fireExtinguisherRemarks;
    private String fireExtinguisherImage;
    
    // 자동확산소화기 정보
    private String autoExtinguisherInstallationDamage;
    private String autoExtinguisherPressureGauge;
    private String autoExtinguisherRemarks;
    private String autoExtinguisherImage;
    
    // 주방자동소화장치 정보
    private String kitchenAutoExtinguisherPressureGauge;
    private String kitchenExtinguisherPowerIndicator;
    private String kitchenAutoExtinguisherRemarks;
    private String kitchenAutoExtinguisherImage;
    
    // 스프링쿨러 정보
    private String sprinklerDamage;
    private String sprinklerRemarks;
    private String sprinklerImage;
    
    // 자동화재 탐지설비 정보
    private String fireDetectionDamage;
    private String fireDetectionRemarks;
    private String fireDetectionImage;
    
    // 가스누설경보기 정보
    private String gasAlarmIndicator;
    private String gasAlarmRemarks;
    private String gasAlarmImage;
    
    // 완강기 정보
    private String descendingLocation;
    private String descendingDamage;
    private String descendingObstruction;
    private String descendingRemarks;
    private String descendingImage;
    
    // 내림식사다리 정보
    private String ladderLocation;
    private String ladderSignage;
    private String ladderObstruction;
    private String ladderRemarks;
    private String ladderImage;
    
    // 대피공간 정보
    private String shelterFireDoor;
    private String shelterStoredItems;
    private String shelterRemarks;
    private String shelterImage;
    
    // 경량칸막이 정보
    private String partitionSignage;
    private String partitionStoredItems;
    private String partitionRemarks;
    private String partitionImage;
    
    private String specialNotes;
} 