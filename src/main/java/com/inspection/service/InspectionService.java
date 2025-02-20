package com.inspection.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inspection.dto.InspectionBoardDTO;
import com.inspection.dto.InspectionCreateDTO;
import com.inspection.dto.InspectionDetailDTO;
import com.inspection.dto.InspectionListDTO;
import com.inspection.entity.Company;
import com.inspection.entity.Inspection;
import com.inspection.entity.User;
import com.inspection.exception.InspectionNotFoundException;
import com.inspection.repository.CompanyRepository;
import com.inspection.repository.InspectionRepository;
import com.inspection.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import com.inspection.util.AESEncryption;

@Service
@RequiredArgsConstructor
@Slf4j
public class InspectionService {
    
    private final InspectionRepository inspectionRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final AESEncryption aesEncryption;

    @Transactional
    public Long createInspection(InspectionCreateDTO dto) {
        try {
            // Company 엔티티 조회
            Company company = companyRepository.findById(dto.getCompanyId())
                .orElseThrow(() -> new RuntimeException("Company not found"));

            User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

            // Inspection 엔티티 생성
            Inspection inspection = new Inspection();
            inspection.setCompany(company);
            inspection.setUser(user);
            inspection.setInspectionDate(dto.getInspectionDate());
            inspection.setManagerName(dto.getManagerName());
            
            // 기본사항
            inspection.setFaucetVoltage(dto.getFaucetVoltage());
            inspection.setFaucetCapacity(dto.getFaucetCapacity());
            inspection.setGenerationVoltage(dto.getGenerationVoltage());
            inspection.setGenerationCapacity(dto.getGenerationCapacity());
            inspection.setSolarCapacity(dto.getSolarCapacity());
            inspection.setContractCapacity(dto.getContractCapacity());
            inspection.setInspectionType(dto.getInspectionType());
            inspection.setInspectionCount(dto.getInspectionCount());
            
            // 점검내역
            inspection.setWiringInlet(dto.getWiringInlet());
            inspection.setDistributionPanel(dto.getDistributionPanel());
            inspection.setMoldedCaseBreaker(dto.getMoldedCaseBreaker());
            inspection.setEarthLeakageBreaker(dto.getEarthLeakageBreaker());
            inspection.setSwitchGear(dto.getSwitchGear());
            inspection.setWiring(dto.getWiring());
            inspection.setMotor(dto.getMotor());
            inspection.setHeatingEquipment(dto.getHeatingEquipment());
            inspection.setWelder(dto.getWelder());
            inspection.setCapacitor(dto.getCapacitor());
            inspection.setLighting(dto.getLighting());
            inspection.setGrounding(dto.getGrounding());
            inspection.setInternalWiring(dto.getInternalWiring());
            inspection.setGenerator(dto.getGenerator());
            inspection.setOtherEquipment(dto.getOtherEquipment());

            // 고압설비 업데이트
            inspection.setAerialLine(dto.getAerialLine());
            inspection.setUndergroundWireLine(dto.getUndergroundWireLine());
            inspection.setPowerSwitch(dto.getPowerSwitch());
            inspection.setBusbar(dto.getBusbar());
            inspection.setLightningArrester(dto.getLightningArrester());
            inspection.setTransformer(dto.getTransformer());
            inspection.setPowerFuse(dto.getPowerFuse());
            inspection.setPowerTransformer(dto.getPowerTransformer());
            inspection.setIncomingPanel(dto.getIncomingPanel());
            inspection.setRelay(dto.getRelay());
            inspection.setCircuitBreaker(dto.getCircuitBreaker());
            inspection.setPowerCapacitor(dto.getPowerCapacitor());
            inspection.setProtectionEquipment(dto.getProtectionEquipment());
            inspection.setLoadEquipment(dto.getLoadEquipment());
            inspection.setGroundingSystem(dto.getGroundingSystem());
            
            

            // measurements를 JSON 문자열로 변환
            if (dto.getMeasurements() != null) {
                String measurementsJson = new ObjectMapper().writeValueAsString(dto.getMeasurements());
                inspection.setMeasurements(measurementsJson);
            }
            
            // 특이사항
            inspection.setSpecialNotes(dto.getSpecialNotes());
            
            // 서명
            inspection.setSignature(dto.getSignature());
            
            // 이미지
            if (dto.getImages() != null && !dto.getImages().isEmpty()) {
                inspection.setImages(new ObjectMapper().writeValueAsString(dto.getImages()));
            }
            
            // 저장
            inspection = inspectionRepository.save(inspection);
            return inspection.getInspectionId();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 처리 중 오류 발생", e);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("잘못된 데이터 입력", e);
        } catch (RuntimeException e) {
            throw e;
        }
    }
    
    @Transactional(readOnly = true)
    public InspectionDetailDTO getInspectionDetail(Long inspectionId) {
        try {
            Inspection inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new InspectionNotFoundException(inspectionId));
            
            InspectionDetailDTO detailDTO = new InspectionDetailDTO();
            
            // 기본 정보 매핑
            detailDTO.setInspectionId(inspection.getInspectionId());
            
            // Company 정보 매핑
            Company company = inspection.getCompany();
            detailDTO.setCompanyId(company.getCompanyId());
            detailDTO.setCompanyName(company.getCompanyName());

            // 🔹 전화번호 복호화 처리
            if (company.getPhoneNumber() != null && !company.getPhoneNumber().isEmpty()) {
                try {
                    detailDTO.setPhoneNumber(aesEncryption.decrypt(company.getPhoneNumber()));
                } catch (Exception e) {
                    detailDTO.setPhoneNumber(company.getPhoneNumber()); // 복호화 실패 시 원본 저장
                }
            }

            // User(작성자) 정보 매핑
            User user = inspection.getUser();
            if (user != null) {
                detailDTO.setUserId(user.getUserId());
                detailDTO.setUsername(user.getUsername());  // 또는 user.getName() 등 표시하고 싶은 사용자 정보
            }
            
            // 나머지 필드들 매핑
            detailDTO.setInspectionDate(inspection.getInspectionDate());
            detailDTO.setManagerName(inspection.getManagerName());
            
            // 기본사항
            detailDTO.setFaucetVoltage(inspection.getFaucetVoltage());
            detailDTO.setFaucetCapacity(inspection.getFaucetCapacity());
            detailDTO.setGenerationVoltage(inspection.getGenerationVoltage());
            detailDTO.setGenerationCapacity(inspection.getGenerationCapacity());
            detailDTO.setSolarCapacity(inspection.getSolarCapacity());
            detailDTO.setContractCapacity(inspection.getContractCapacity());
            detailDTO.setInspectionType(inspection.getInspectionType());
            detailDTO.setInspectionCount(inspection.getInspectionCount());
            
            // 점검내역
            detailDTO.setWiringInlet(inspection.getWiringInlet());
            detailDTO.setDistributionPanel(inspection.getDistributionPanel());
            detailDTO.setMoldedCaseBreaker(inspection.getMoldedCaseBreaker());
            detailDTO.setEarthLeakageBreaker(inspection.getEarthLeakageBreaker());
            detailDTO.setSwitchGear(inspection.getSwitchGear());
            detailDTO.setWiring(inspection.getWiring());
            detailDTO.setMotor(inspection.getMotor());
            detailDTO.setHeatingEquipment(inspection.getHeatingEquipment());
            detailDTO.setWelder(inspection.getWelder());
            detailDTO.setCapacitor(inspection.getCapacitor());
            detailDTO.setLighting(inspection.getLighting());
            detailDTO.setGrounding(inspection.getGrounding());
            detailDTO.setInternalWiring(inspection.getInternalWiring());
            detailDTO.setGenerator(inspection.getGenerator());
            detailDTO.setOtherEquipment(inspection.getOtherEquipment());

            // 고압설비
            detailDTO.setAerialLine(inspection.getAerialLine());
            detailDTO.setUndergroundWireLine(inspection.getUndergroundWireLine());
            detailDTO.setPowerSwitch(inspection.getPowerSwitch());
            detailDTO.setBusbar(inspection.getBusbar());
            detailDTO.setLightningArrester(inspection.getLightningArrester());
            detailDTO.setTransformer(inspection.getTransformer());
            detailDTO.setPowerFuse(inspection.getPowerFuse());
            detailDTO.setPowerTransformer(inspection.getPowerTransformer());
            detailDTO.setIncomingPanel(inspection.getIncomingPanel());
            detailDTO.setRelay(inspection.getRelay());
            detailDTO.setCircuitBreaker(inspection.getCircuitBreaker());    
            detailDTO.setPowerCapacitor(inspection.getPowerCapacitor());
            detailDTO.setProtectionEquipment(inspection.getProtectionEquipment());
            detailDTO.setLoadEquipment(inspection.getLoadEquipment());
            detailDTO.setGroundingSystem(inspection.getGroundingSystem());

            

            
            // 측정개소
            detailDTO.setMeasurements(inspection.getMeasurements());
            
            // 특이사항
            detailDTO.setSpecialNotes(inspection.getSpecialNotes());
            
            // 서명 정보 매핑 - 필드명 일치시키기
            detailDTO.setSignature(inspection.getSignature());  // inspectorSignature가 아닌 signature
            detailDTO.setManagerSignature(inspection.getManagerSignature());
            
            // 이미지 데이터 처리
            if (inspection.getImages() != null && !inspection.getImages().isEmpty()) {
                List<String> imagesList = new ObjectMapper().readValue(
                    inspection.getImages(),
                    new TypeReference<List<String>>() {}
                );
                detailDTO.setImages(imagesList);
            }
            
            return detailDTO;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 데이터 처리 중 오류 발생", e);
        } catch (InspectionNotFoundException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new RuntimeException("점검 상세 정보 조회 중 오류 발생", e);
        }
    }
    
    @Transactional(readOnly = true)
    public List<InspectionListDTO> getAllInspections() {
        return inspectionRepository.findAll().stream()
            .map(inspection -> {
                InspectionListDTO dto = new InspectionListDTO();
                dto.setInspectionId(inspection.getInspectionId());
                dto.setCompanyId(inspection.getCompany().getCompanyId());
                dto.setInspectionDate(inspection.getInspectionDate());
                dto.setManagerName(inspection.getManagerName());
                return dto;
            })
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<InspectionBoardDTO> getInspections(Pageable pageable) {
        return inspectionRepository.findAll(pageable)
            .map(inspection -> {
                InspectionBoardDTO dto = new InspectionBoardDTO();
                dto.setInspectionId(inspection.getInspectionId()); // 점검 id
                dto.setCompanyId(inspection.getCompany().getCompanyId()); //업체 id
                dto.setCompanyName(inspection.getCompany().getCompanyName());  // 업체명 
                dto.setInspectionDate(inspection.getInspectionDate()); // 점검일
                dto.setManagerName(inspection.getManagerName()); // 담당자명
                return dto;

            });
    }


    // 업체별 점검 내역 조회 메서드 추가
    @Transactional(readOnly = true)
    public List<InspectionListDTO> getInspectionsByCompany(Long companyId) {
        return inspectionRepository.findByCompany_CompanyId(companyId).stream()
            .map(inspection -> {
                InspectionListDTO dto = new InspectionListDTO();
                dto.setInspectionId(inspection.getInspectionId());
                dto.setCompanyId(inspection.getCompany().getCompanyId());
                dto.setInspectionDate(inspection.getInspectionDate());
                dto.setManagerName(inspection.getManagerName());
                return dto;
            })
            .collect(Collectors.toList());
    }

    @Transactional
    public InspectionDetailDTO saveManagerSignature(Long inspectionId, String signature) {
        Inspection inspection = inspectionRepository.findById(inspectionId)
            .orElseThrow(() -> new InspectionNotFoundException(inspectionId));
        
        inspection.setManagerSignature(signature);
        inspectionRepository.save(inspection);
        
        return getInspectionDetail(inspectionId);
    }

    @Transactional
    public InspectionDetailDTO updateInspection(Long id, InspectionCreateDTO updateData) {
        try {
            Inspection inspection = inspectionRepository.findById(id)
                .orElseThrow(() -> new InspectionNotFoundException(id));

            // Company 정보 업데이트
            if (updateData.getCompanyId() != null) {
                Company company = companyRepository.findById(updateData.getCompanyId())
                    .orElseThrow(() -> new RuntimeException("Company not found"));
                inspection.setCompany(company);
            }

            // 기본 정보 업데이트
            inspection.setInspectionDate(updateData.getInspectionDate());
            inspection.setManagerName(updateData.getManagerName());

            // 기본사항 업데이트
            inspection.setFaucetVoltage(updateData.getFaucetVoltage());
            inspection.setFaucetCapacity(updateData.getFaucetCapacity());
            inspection.setGenerationVoltage(updateData.getGenerationVoltage());
            inspection.setGenerationCapacity(updateData.getGenerationCapacity());
            inspection.setSolarCapacity(updateData.getSolarCapacity());
            inspection.setContractCapacity(updateData.getContractCapacity());
            inspection.setInspectionType(updateData.getInspectionType());
            inspection.setInspectionCount(updateData.getInspectionCount());

            // 점검내역 업데이트
            inspection.setWiringInlet(updateData.getWiringInlet());
            inspection.setDistributionPanel(updateData.getDistributionPanel());
            inspection.setMoldedCaseBreaker(updateData.getMoldedCaseBreaker());
            inspection.setEarthLeakageBreaker(updateData.getEarthLeakageBreaker());
            inspection.setSwitchGear(updateData.getSwitchGear());
            inspection.setWiring(updateData.getWiring());
            inspection.setMotor(updateData.getMotor());
            inspection.setHeatingEquipment(updateData.getHeatingEquipment());
            inspection.setWelder(updateData.getWelder());
            inspection.setCapacitor(updateData.getCapacitor());
            inspection.setLighting(updateData.getLighting());
            inspection.setGrounding(updateData.getGrounding());
            inspection.setInternalWiring(updateData.getInternalWiring());
            inspection.setGenerator(updateData.getGenerator());
            inspection.setOtherEquipment(updateData.getOtherEquipment());

            // 고압설비 업데이트
            inspection.setAerialLine(updateData.getAerialLine());
            inspection.setUndergroundWireLine(updateData.getUndergroundWireLine());
            inspection.setPowerSwitch(updateData.getPowerSwitch());
            inspection.setBusbar(updateData.getBusbar());
            inspection.setLightningArrester(updateData.getLightningArrester());
            inspection.setTransformer(updateData.getTransformer());
            inspection.setPowerFuse(updateData.getPowerFuse());
            inspection.setPowerTransformer(updateData.getPowerTransformer());
            inspection.setIncomingPanel(updateData.getIncomingPanel());
            inspection.setRelay(updateData.getRelay());
            inspection.setCircuitBreaker(updateData.getCircuitBreaker());
            inspection.setPowerCapacitor(updateData.getPowerCapacitor());
            inspection.setProtectionEquipment(updateData.getProtectionEquipment());
            inspection.setLoadEquipment(updateData.getLoadEquipment());
            inspection.setGroundingSystem(updateData.getGroundingSystem());
            


            // 측정개소 업데이트
            if (updateData.getMeasurements() != null) {
                String measurementsJson = new ObjectMapper().writeValueAsString(updateData.getMeasurements());
                inspection.setMeasurements(measurementsJson);
            }

            // 특이사항 업데이트
            inspection.setSpecialNotes(updateData.getSpecialNotes());

            // 서명 정보 업데이트 (기존 서명은 유지)
            if (updateData.getSignature() != null) {
                inspection.setSignature(updateData.getSignature());
            }

            // 이미지 업데이트
            if (updateData.getImages() != null) {
                inspection.setImages(new ObjectMapper().writeValueAsString(updateData.getImages()));
            }

            Inspection updatedInspection = inspectionRepository.save(inspection);
            return getInspectionDetail(updatedInspection.getInspectionId());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("이미지 데이터 처리 중 오류 발생", e);
        }
    }

    @Transactional
    public void deleteInspection(Long id) {
        try {
            Inspection inspection = inspectionRepository.findById(id)
                .orElseThrow(() -> new InspectionNotFoundException(id));
            
            // 이미지 파일 삭제
            if (inspection.getImages() != null && !inspection.getImages().isEmpty()) {
                List<String> imagesList = new ObjectMapper().readValue(
                    inspection.getImages(),
                    new TypeReference<List<String>>() {}
                );
                for (String image : imagesList) {
                    deleteImage(image);
                }
            }
            
            inspectionRepository.delete(inspection);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("이미지 데이터 처리 중 오류 발생", e);
        }
    }

    private void deleteImage(String imageName) {
        try {
            Path imagePath = Paths.get("/root/inspection-app/backend/uploads/images").resolve(imageName);
            Files.deleteIfExists(imagePath);
        } catch (IOException e) {
            log.error("이미지 파일 삭제 실패: {}", e.getMessage());
        }
    }

    public Page<InspectionBoardDTO> getInspectionsByCompany(Long companyId, Pageable pageable) {
        // 회사 존재 여부 확인
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new RuntimeException("회사를 찾을 수 없습니다."));

        // 페이징된 점검 결과 조회
        Page<Inspection> inspections = inspectionRepository.findByCompany_CompanyId(companyId, pageable);
        
        // DTO로 변환
        return inspections.map(inspection -> new InspectionBoardDTO(
            inspection.getInspectionId(),
            inspection.getCompany().getCompanyName(),
            inspection.getInspectionDate(),
            inspection.getManagerName(),
            inspection.getCompany().getCompanyId()
        ));
    }
} 