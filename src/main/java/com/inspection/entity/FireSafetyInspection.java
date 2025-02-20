package com.inspection.entity;

import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.Convert;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Entity
@Table(name = "fire_safety_inspections")
@Getter @Setter
public class FireSafetyInspection {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long fireInspectionId;  // PK

    // 기본 정보
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User writer;  // 작성자

    @Column(name = "building_name")
    private String buildingName;  // 건물명

    @Column(name = "inspection_date")
    private LocalDate inspectionDate;  // 점검일

    @Column(name = "address")
    private String address;  // 주소

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;  // 점검업체명

    @Column(name = "building_grade")
    private String buildingGrade;  // 건물등급

    // 점검 내역 필드들
    @Column(name = "fire_extinguisher_status", columnDefinition = "TEXT")
    private String fireExtinguisherStatus;  // 소화설비 상태
    
    @Column(name = "fire_alarm_status", columnDefinition = "TEXT")
    private String fireAlarmStatus;  // 경보설비 상태
    
    @Column(name = "fire_evacuation_status", columnDefinition = "TEXT")
    private String fireEvacuationStatus;  // 피난구조설비 상태
    
    @Column(name = "fire_water_status", columnDefinition = "TEXT")
    private String fireWaterStatus;  // 소화용수설비 상태
    
    @Column(name = "fire_fighting_status", columnDefinition = "TEXT")
    private String fireFightingStatus;  // 소화활동설비 상태

    // 기타
    @Column(name = "etc_comment", columnDefinition = "TEXT")
    private String etcComment;  // 기타 의견

    // 서명
    @Lob
    @Column(name = "inspector_signature", columnDefinition = "LONGTEXT")
    private String inspectorSignature;  // 점검자 서명 데이터

    @Lob
    @Column(name = "manager_signature", columnDefinition = "LONGTEXT")
    private String managerSignature;  // 관리자 서명 데이터

    // 첨부파일 (JSON 형식으로 여러 이미지 저장)
    @Column(name = "attachments", columnDefinition = "JSON")
    @Convert(converter = JsonListConverter.class)
    private List<String> attachments;

    @Column(name = "created_at")
    private LocalDate createdAt;

    @Column(name = "updated_at")
    private LocalDate updatedAt;
}

@Converter
class JsonListConverter implements AttributeConverter<List<String>, String> {
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        try {
            return mapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        try {
            return mapper.readValue(dbData, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }
} 