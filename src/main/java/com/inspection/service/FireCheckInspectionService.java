package com.inspection.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.inspection.repository.FireCheckInspectionRepository;
import com.inspection.entity.FireCheckInspection;
import com.inspection.dto.FireCheckInspectionDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FireCheckInspectionService {
    
    private final FireCheckInspectionRepository fireCheckInspectionRepository;
    
    // Create
    @Transactional
    public FireCheckInspectionDTO createInspection(FireCheckInspectionDTO dto) {
        FireCheckInspection entity = new FireCheckInspection();
        BeanUtils.copyProperties(dto, entity);
        FireCheckInspection savedEntity = fireCheckInspectionRepository.save(entity);
        return convertToDTO(savedEntity);
    }
    
    // Read
    public FireCheckInspectionDTO getInspection(Long id) {
        FireCheckInspection entity = fireCheckInspectionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("점검 기록을 찾을 수 없습니다."));
        return convertToDTO(entity);
    }
    
    // Read All
    public List<FireCheckInspectionDTO> getAllInspections() {
        return fireCheckInspectionRepository.findAll().stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    // Update
    @Transactional
    public FireCheckInspectionDTO updateInspection(Long id, FireCheckInspectionDTO dto) {
        FireCheckInspection entity = fireCheckInspectionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("점검 기록을 찾을 수 없습니다."));
        
        BeanUtils.copyProperties(dto, entity, "fireCheckInspectionId");
        FireCheckInspection updatedEntity = fireCheckInspectionRepository.save(entity);
        return convertToDTO(updatedEntity);
    }
    
    // Delete
    @Transactional
    public void deleteInspection(Long id) {
        fireCheckInspectionRepository.deleteById(id);
    }
    
    // Entity -> DTO 변환
    private FireCheckInspectionDTO convertToDTO(FireCheckInspection entity) {
        FireCheckInspectionDTO dto = new FireCheckInspectionDTO();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }
}
