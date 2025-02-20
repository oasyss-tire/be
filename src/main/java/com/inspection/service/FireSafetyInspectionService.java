package com.inspection.service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.inspection.entity.Company;
import com.inspection.entity.FireSafetyInspection;
import com.inspection.entity.User;
import com.inspection.repository.CompanyRepository;
import com.inspection.repository.FireSafetyInspectionRepository;
import com.inspection.repository.UserRepository;
import com.inspection.dto.FireSafetyInspectionDTO;
import com.inspection.dto.FireSafetyInspectionCreateDTO;
import com.inspection.dto.FireSafetyInspectionUpdateDTO;
import com.inspection.util.AESEncryption;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FireSafetyInspectionService {
    
    private final FireSafetyInspectionRepository fireSafetyInspectionRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final AESEncryption aesEncryption;

    // 전체 점검 목록 조회
    @Transactional(readOnly = true)
    public List<FireSafetyInspectionDTO> getAllInspections() {
        return fireSafetyInspectionRepository.findAll().stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    // 단일 점검 조회
    @Transactional(readOnly = true)
    public FireSafetyInspectionDTO getInspectionById(Long id) {
        FireSafetyInspection inspection = fireSafetyInspectionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("점검 기록을 찾을 수 없습니다. ID: " + id));
        return convertToDTO(inspection);
    }

    // 점검 생성
    @Transactional
    public FireSafetyInspectionDTO createInspection(FireSafetyInspectionCreateDTO dto) {
        User writer = userRepository.findById(dto.getWriterId())
            .orElseThrow(() -> new RuntimeException("작성자를 찾을 수 없습니다."));
        
        Company company = companyRepository.findById(dto.getCompanyId())
            .orElseThrow(() -> new RuntimeException("점검업체를 찾을 수 없습니다."));
        
        FireSafetyInspection inspection = new FireSafetyInspection();
        inspection.setWriter(writer);
        inspection.setCompany(company);
        inspection.setBuildingName(dto.getBuildingName());
        inspection.setInspectionDate(dto.getInspectionDate());
        inspection.setAddress(dto.getAddress());
        inspection.setBuildingGrade(dto.getBuildingGrade());
        inspection.setFireExtinguisherStatus(dto.getFireExtinguisherStatus());
        inspection.setFireAlarmStatus(dto.getFireAlarmStatus());
        inspection.setFireEvacuationStatus(dto.getFireEvacuationStatus());
        inspection.setFireWaterStatus(dto.getFireWaterStatus());
        inspection.setFireFightingStatus(dto.getFireFightingStatus());
        inspection.setEtcComment(dto.getEtcComment());
        inspection.setInspectorSignature(dto.getInspectorSignature());
        inspection.setManagerSignature(dto.getManagerSignature());
        inspection.setAttachments(dto.getAttachments());
        inspection.setCreatedAt(LocalDate.now());
        inspection.setUpdatedAt(LocalDate.now());
        
        FireSafetyInspection savedInspection = fireSafetyInspectionRepository.save(inspection);
        return convertToDTO(savedInspection);
    }

    private FireSafetyInspectionDTO convertToDTO(FireSafetyInspection inspection) {
        FireSafetyInspectionDTO dto = new FireSafetyInspectionDTO();
        dto.setFireInspectionId(inspection.getFireInspectionId());
        dto.setWriterId(inspection.getWriter().getUserId());
        dto.setWriterName(inspection.getWriter().getFullName());
        dto.setBuildingName(inspection.getBuildingName());
        dto.setInspectionDate(inspection.getInspectionDate());
        dto.setAddress(inspection.getAddress());
        dto.setCompanyId(inspection.getCompany().getCompanyId());
        dto.setCompanyName(inspection.getCompany().getCompanyName());
        dto.setBuildingGrade(inspection.getBuildingGrade());
        dto.setFireExtinguisherStatus(inspection.getFireExtinguisherStatus());
        dto.setFireAlarmStatus(inspection.getFireAlarmStatus());
        dto.setFireEvacuationStatus(inspection.getFireEvacuationStatus());
        dto.setFireWaterStatus(inspection.getFireWaterStatus());
        dto.setFireFightingStatus(inspection.getFireFightingStatus());
        dto.setEtcComment(inspection.getEtcComment());
        dto.setInspectorSignature(inspection.getInspectorSignature());
        dto.setManagerSignature(inspection.getManagerSignature());
        dto.setAttachments(inspection.getAttachments());
        dto.setCreatedAt(inspection.getCreatedAt());
        dto.setUpdatedAt(inspection.getUpdatedAt());

                // 전화번호 복호화 처리 추가
        if (inspection.getCompany().getPhoneNumber() != null && 
            !inspection.getCompany().getPhoneNumber().isEmpty()) {
            try {
                dto.setPhoneNumber(aesEncryption.decrypt(inspection.getCompany().getPhoneNumber()));
            } catch (Exception e) {
                // 복호화 실패 시 원본 값 사용
                dto.setPhoneNumber(inspection.getCompany().getPhoneNumber());
            }
        }
        return dto;
    }

    // 점검 수정
    @Transactional
    public FireSafetyInspectionDTO updateInspection(Long id, FireSafetyInspectionUpdateDTO dto) {
        FireSafetyInspection existingInspection = fireSafetyInspectionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("점검 기록을 찾을 수 없습니다. ID: " + id));
        
        // 기본 정보 업데이트
        existingInspection.setBuildingName(dto.getBuildingName());
        existingInspection.setInspectionDate(dto.getInspectionDate());
        existingInspection.setAddress(dto.getAddress());
        existingInspection.setBuildingGrade(dto.getBuildingGrade());
        existingInspection.setFireExtinguisherStatus(dto.getFireExtinguisherStatus());
        existingInspection.setFireAlarmStatus(dto.getFireAlarmStatus());
        existingInspection.setFireEvacuationStatus(dto.getFireEvacuationStatus());
        existingInspection.setFireWaterStatus(dto.getFireWaterStatus());
        existingInspection.setFireFightingStatus(dto.getFireFightingStatus());
        existingInspection.setEtcComment(dto.getEtcComment());
        existingInspection.setInspectorSignature(dto.getInspectorSignature());
        existingInspection.setManagerSignature(dto.getManagerSignature());
        
        // 새로운 이미지가 있다면 attachments 업데이트
        if (dto.getAttachments() != null) {
            existingInspection.setAttachments(dto.getAttachments());
        }
        
        existingInspection.setUpdatedAt(LocalDate.now());
        
        FireSafetyInspection savedInspection = fireSafetyInspectionRepository.save(existingInspection);
        return convertToDTO(savedInspection);
    }

    private void deleteImage(String fileName) {
        try {
            Path imagePath = Paths.get("uploads/fire-safety-images/" + fileName);
            Files.deleteIfExists(imagePath);
        } catch (IOException e) {
            throw new RuntimeException("이미지 파일 삭제 중 오류 발생", e);
        }
    }

    // 점검 삭제
    @Transactional
    public void deleteInspection(Long id) {
        FireSafetyInspection inspection = fireSafetyInspectionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("점검 기록을 찾을 수 없습니다. ID: " + id));
        
        // 이미지 파일 삭제
        if (inspection.getAttachments() != null && !inspection.getAttachments().isEmpty()) {
            for (String image : inspection.getAttachments()) {
                deleteImage(image);
            }
        }
        
        fireSafetyInspectionRepository.delete(inspection);
    }

    // 회사별 점검 목록 조회
    @Transactional(readOnly = true)
    public List<FireSafetyInspectionDTO> getInspectionsByCompany(Long companyId) {
        return fireSafetyInspectionRepository.findByCompany_CompanyId(companyId).stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    // 작성자별 점검 목록 조회
    @Transactional(readOnly = true)
    public List<FireSafetyInspectionDTO> getInspectionsByWriter(Long userId) {
        return fireSafetyInspectionRepository.findByWriter_UserId(userId).stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    @Transactional
    public FireSafetyInspectionDTO saveManagerSignature(Long id, String signature) {
        FireSafetyInspection inspection = fireSafetyInspectionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("점검 기록을 찾을 수 없습니다. ID: " + id));
        
        inspection.setManagerSignature(signature);
        inspection.setUpdatedAt(LocalDate.now());
        
        FireSafetyInspection savedInspection = fireSafetyInspectionRepository.save(inspection);
        return convertToDTO(savedInspection);
    }
} 