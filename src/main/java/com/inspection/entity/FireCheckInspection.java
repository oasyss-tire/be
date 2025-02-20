package com.inspection.entity;


import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor

public class FireCheckInspection {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long fireCheckInspectionId;
    
    // 기본정보
    @Column(name = "target_name")
    private String targetName; // 대상명
    
    @Column(name = "unit_number")
    private String unitNumber; // 동호수
    
    @Column(name = "inspection_date")
    private LocalDate inspectionDate; // 점검일
    
    @Column(name = "inspector_name")
    private String inspectorName; // 점검자
    
    @Column(name = "phone_number")
    private String phoneNumber; // 전화번호
    
    @Lob
    @Column(name = "inspector_signature" , columnDefinition = "LONGTEXT")
    private String inspectorSignature; // 점검자 사인 (Base64 인코딩)
    
    @Lob
    @Column(name = "company_signature" , columnDefinition = "LONGTEXT")
    private String companySignature; // 대상업체 사인 (Base64 인코딩)
    
    
    // 점검 정보 - 소화기
    @Column(name = "fire_ext_installed", length = 10)
    private String fireExtinguisherInstalled; // 소화기 설치여부
    
    @Column(name = "fire_ext_damage", length = 10)
    private String fireExtinguisherDamage; // 소화기 손상 여부
    
    @Column(name = "fire_ext_safety_pin", length = 10)
    private String fireExtinguisherSafetyPin; // 소화기 안전피 여부
    
    @Column(name = "fire_ext_pressure_gauge", length = 10)
    private String fireExtinguisherPressureGauge; // 소화기 지시압력계 여부
    
    @Column(name = "fire_ext_content_age_valid", length = 10)
    private String fireExtinguisherContentAgeValid; // 소화기 내용년수 적정 여부
    
    @Column(name = "fire_ext_remarks")
    private String fireExtinguisherRemarks; // 소화기 비고
    
    @Column(name = "fire_ext_image")
    private String fireExtinguisherImage; // 소화기 이미지
    
    
    // 점검 정보 - 자동확산소화기
    @Column(name = "auto_ext_inst_damage", length = 10)
    private String autoExtinguisherInstallationDamage; // 자동확산소화기 설치상태 및 손상 여부
    
    @Column(name = "auto_ext_pressure_gauge", length = 10)
    private String autoExtinguisherPressureGauge; // 자동확산소화기 지시압력계 여부
    
    @Column(name = "auto_ext_remarks")
    private String autoExtinguisherRemarks; // 자동확산소화기 비고
    
    @Column(name = "auto_ext_image")
    private String autoExtinguisherImage; // 자동확산 이미지
    
    
    // 점검 정보 - 주방자동소화장치
    @Column(name = "kitchen_auto_ext_pressure_gauge", length = 10)
    private String kitchenAutoExtinguisherPressureGauge; // 주방자동소화장치 지시압력계
    
    @Column(name = "kitchen_ext_power_indicator", length = 10)
    private String kitchenExtinguisherPowerIndicator; // 주방소화장치 전원표시등
    
    @Column(name = "kitchen_auto_ext_remarks")
    private String kitchenAutoExtinguisherRemarks; // 주방자동소화장치 비고
    
    @Column(name = "kitchen_auto_ext_image")
    private String kitchenAutoExtinguisherImage; // 주방자동소화장치 이미지
    
    
    // 점검 정보 - 스프링쿨러
    @Column(name = "sprinkler_damage", length = 10)
    private String sprinklerDamage; // 스프링쿨러 손상
    
    @Column(name = "sprinkler_remarks")
    private String sprinklerRemarks; // 스프링쿨러 비고
    
    @Column(name = "sprinkler_image")
    private String sprinklerImage; // 스프링쿨러 이미지
    
    
    // 점검 정보 - 자동화재 탐지설비
    @Column(name = "fire_detection_damage", length = 10)
    private String fireDetectionDamage; // 자동화재 탐지설비 손상
    
    @Column(name = "fire_detection_remarks")
    private String fireDetectionRemarks; // 자동화재 탐지설비 비고
    
    @Column(name = "fire_detection_image")
    private String fireDetectionImage; // 자동화재 탐지설비 이미지
    
    
    // 점검 정보 - 가스누설경보기
    @Column(name = "gas_alarm_indicator", length = 10)
    private String gasAlarmIndicator; // 가스누설경보기 점등
    
    @Column(name = "gas_alarm_remarks")
    private String gasAlarmRemarks; // 가스누설경보기 비고
    
    @Column(name = "gas_alarm_image")
    private String gasAlarmImage; // 가스누설경보기 이미지
    
    
    // 점검 정보 - 완강기
    @Column(name = "descending_location")
    private String descendingLocation; // 완강기 위치
    
    @Column(name = "descending_damage", length = 10)
    private String descendingDamage; // 완강기 손상
    
    @Column(name = "descending_obstruction", length = 10)
    private String descendingObstruction; // 완강기 장애물
    
    @Column(name = "descending_remarks")
    private String descendingRemarks; // 완강기 비고
    
    @Column(name = "descending_image")
    private String descendingImage; // 완강기 이미지
    
    
    // 점검 정보 - 내림식사다리
    @Column(name = "ladder_location")
    private String ladderLocation; // 내림식사다리 위치
    
    @Column(name = "ladder_signage", length = 10)
    private String ladderSignage; // 내림식사다리 표지
    
    @Column(name = "ladder_obstruction", length = 10)
    private String ladderObstruction; // 내림식사다리 장애물
    
    @Column(name = "ladder_remarks")
    private String ladderRemarks; // 내림식사다리 비고
    
    @Column(name = "ladder_image")
    private String ladderImage; // 내림식사다리 이미지
    
    
    // 점검 정보 - 대피공간
    @Column(name = "shelter_fire_door", length = 10)
    private String shelterFireDoor; // 대피공간 방화문
    
    @Column(name = "shelter_stored_items", length = 10)
    private String shelterStoredItems; // 대피공간 적치물
    
    @Column(name = "shelter_remarks")
    private String shelterRemarks; // 대피공간 비고
    
    @Column(name = "shelter_image")
    private String shelterImage; // 대피공간 이미지
    
    
    // 점검 정보 - 경량칸막이
    @Column(name = "partition_signage", length = 10)
    private String partitionSignage; // 경량칸막이 표지
    
    @Column(name = "partition_stored_items", length = 10)
    private String partitionStoredItems; // 경량칸막이 적치물
    
    @Column(name = "partition_remarks")
    private String partitionRemarks; // 경량칸막이 비고
    
    @Column(name = "partition_image")
    private String partitionImage; // 경량칸막이 이미지
    
    
    // 특이사항
    @Column(name = "special_notes")
    private String specialNotes; // 특이사항

}
