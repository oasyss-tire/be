package com.inspection.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import java.time.LocalDate;
import com.inspection.entity.Facility;
import com.inspection.entity.FacilityStatus;
import com.inspection.repository.FacilityRepository;
import com.inspection.repository.CompanyRepository;
import com.inspection.repository.FacilityContractRepository;
import com.inspection.dto.FacilityDTO;
import com.inspection.dto.FacilityContractDTO;
import com.inspection.entity.Company;
import com.inspection.entity.FacilityContract;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.stream.Collectors;
import com.inspection.dto.FacilityDetailDTO;
import com.inspection.dto.FacilityImageDTO;
import com.inspection.entity.FacilityImage;
import org.springframework.web.multipart.MultipartFile;
import com.inspection.repository.FacilityImageRepository;
import com.inspection.service.ImageService;
import java.util.List;



@Service
@RequiredArgsConstructor
public class FacilityService {
    private final FacilityRepository facilityRepository;
    private final CompanyRepository companyRepository;
    private final FacilityContractRepository contractRepository;
    private final FacilityImageRepository facilityImageRepository;
    private final ImageService imageService;  // FileService 대신 ImageService 주입

    // 시설물 등록
    @Transactional
    public FacilityDTO registerFacility(FacilityDTO dto) {
        Company company = companyRepository.findById(dto.getCompanyId())
            .orElseThrow(() -> new RuntimeException("회사를 찾을 수 없습니다."));
        
        Facility facility = new Facility();
        facility.setCompany(company);
        facility.setName(dto.getName());
        facility.setCode(dto.getCode());
        facility.setLocation(dto.getLocation());
        facility.setAcquisitionCost(dto.getAcquisitionCost());
        facility.setAcquisitionDate(dto.getAcquisitionDate());
        facility.setStatus(FacilityStatus.IN_USE);
        facility.setCurrentLocation(dto.getCurrentLocation());
        facility.setDescription(dto.getDescription());
        
        return convertToDTO(facilityRepository.save(facility));
    }

    // 시설물 납품/이동 등록
    @Transactional
    public FacilityContractDTO createContract(FacilityContractDTO dto) {
        Facility facility = facilityRepository.findById(dto.getFacilityId())
            .orElseThrow(() -> new RuntimeException("시설물을 찾을 수 없습니다."));
        
        FacilityContract contract = new FacilityContract();
        contract.setFacility(facility);
        contract.setContractNumber(dto.getContractNumber());
        contract.setStartDate(dto.getStartDate());
        contract.setEndDate(dto.getEndDate());
        contract.setContractAmount(dto.getContractAmount());
        contract.setVendorName(dto.getVendorName());
        contract.setVendorContact(dto.getVendorContact());
        contract.setDescription(dto.getDescription());
        
        // 시설물 상태 및 위치 업데이트
        facility.setStatus(FacilityStatus.MOVED);
        facility.setStatusChangeDate(LocalDate.now());
        facility.setCurrentLocation(dto.getVendorName());
        
        return convertToContractDTO(contractRepository.save(contract));
    }

    // 시설물 상태 변경
    @Transactional
    public FacilityDTO updateStatus(Long facilityId, FacilityStatus newStatus) {
        Facility facility = facilityRepository.findById(facilityId)
            .orElseThrow(() -> new RuntimeException("시설물을 찾을 수 없습니다."));
        
        facility.setStatus(newStatus);
        facility.setStatusChangeDate(LocalDate.now());
        
        return convertToDTO(facilityRepository.save(facility));
    }

    // Entity -> DTO 변환 메서드 추가
    private FacilityDTO convertToDTO(Facility facility) {
        FacilityDTO dto = new FacilityDTO();
        dto.setId(facility.getId());
        dto.setCompanyId(facility.getCompany().getCompanyId());
        dto.setName(facility.getName());
        dto.setCode(facility.getCode());
        dto.setLocation(facility.getLocation());
        dto.setAcquisitionCost(facility.getAcquisitionCost());
        dto.setAcquisitionDate(facility.getAcquisitionDate());
        dto.setStatus(facility.getStatus());
        dto.setCurrentLocation(facility.getCurrentLocation());
        dto.setDescription(facility.getDescription());
        
        // 첫 번째 이미지를 썸네일로 설정
        if (!facility.getImages().isEmpty()) {
            dto.setThumbnailUrl(facility.getImages().get(0).getImageUrl());
        }
        
        return dto;
    }

    private FacilityContractDTO convertToContractDTO(FacilityContract contract) {
        FacilityContractDTO dto = new FacilityContractDTO();
        dto.setId(contract.getId());
        dto.setFacilityId(contract.getFacility().getId());
        dto.setContractNumber(contract.getContractNumber());
        dto.setStartDate(contract.getStartDate());
        dto.setEndDate(contract.getEndDate());
        dto.setContractAmount(contract.getContractAmount());
        dto.setIsPaid(contract.getIsPaid());
        dto.setVendorName(contract.getVendorName());
        dto.setVendorContact(contract.getVendorContact());
        dto.setDescription(contract.getDescription());
        return dto;
    }

    // 시설물 목록 조회 (페이징, 검색)
    public Page<FacilityDTO> getFacilities(
            String keyword,
            FacilityStatus status,
            String location,
            Pageable pageable) {
        
        Page<Facility> facilities;
        if (keyword != null && !keyword.trim().isEmpty()) {
            facilities = facilityRepository.findByNameContainingOrCodeContaining(
                keyword, keyword, pageable);
        } else if (status != null) {
            facilities = facilityRepository.findByStatus(status, pageable);
        } else if (location != null) {
            facilities = facilityRepository.findByCurrentLocation(location, pageable);
        } else {
            facilities = facilityRepository.findAll(pageable);
        }
        
        return facilities.map(this::convertToDTO);
    }

    // 시설물 상세 조회
    public FacilityDetailDTO getFacilityDetail(Long facilityId) {
        Facility facility = facilityRepository.findById(facilityId)
            .orElseThrow(() -> new RuntimeException("시설물을 찾을 수 없습니다."));

        FacilityDetailDTO detailDTO = new FacilityDetailDTO();
        // 기본 정보 설정
        detailDTO.setId(facility.getId());
        detailDTO.setCompanyId(facility.getCompany().getCompanyId());
        detailDTO.setCompanyName(facility.getCompany().getCompanyName());
        detailDTO.setName(facility.getName());
        detailDTO.setCode(facility.getCode());
        detailDTO.setLocation(facility.getLocation());
        detailDTO.setAcquisitionCost(facility.getAcquisitionCost());
        detailDTO.setAcquisitionDate(facility.getAcquisitionDate());
        detailDTO.setStatus(facility.getStatus());
        detailDTO.setStatusChangeDate(facility.getStatusChangeDate());
        detailDTO.setCurrentLocation(facility.getCurrentLocation());
        detailDTO.setDescription(facility.getDescription());

        // 계약 정보 설정
        detailDTO.setContracts(facility.getContracts().stream()
            .map(this::convertToContractDTO)
            .collect(Collectors.toList()));

        // 이미지 정보 설정
        detailDTO.setImages(facility.getImages().stream()
            .map(this::convertToImageDTO)
            .collect(Collectors.toList()));

        return detailDTO;
    }

    private FacilityImageDTO convertToImageDTO(FacilityImage image) {
        FacilityImageDTO dto = new FacilityImageDTO();
        dto.setId(image.getId());
        dto.setFacilityId(image.getFacility().getId());
        dto.setUrl(image.getImageUrl());
        dto.setDescription(image.getDescription());
        return dto;
    }

    @Transactional
    public FacilityDTO updateFacility(Long facilityId, FacilityDTO dto) {
        Facility facility = facilityRepository.findById(facilityId)
            .orElseThrow(() -> new RuntimeException("시설물을 찾을 수 없습니다."));
        
        // 회사 정보가 변경된 경우
        if (!facility.getCompany().getCompanyId().equals(dto.getCompanyId())) {
            Company newCompany = companyRepository.findById(dto.getCompanyId())
                .orElseThrow(() -> new RuntimeException("회사를 찾을 수 없습니다."));
            facility.setCompany(newCompany);
        }
        
        // 기본 정보 업데이트
        facility.setName(dto.getName());
        facility.setCode(dto.getCode());
        facility.setLocation(dto.getLocation());
        facility.setAcquisitionCost(dto.getAcquisitionCost());
        facility.setAcquisitionDate(dto.getAcquisitionDate());
        facility.setCurrentLocation(dto.getCurrentLocation());
        facility.setDescription(dto.getDescription());
        
        // 상태가 변경된 경우
        if (dto.getStatus() != null && facility.getStatus() != dto.getStatus()) {
            facility.setStatus(dto.getStatus());
            facility.setStatusChangeDate(LocalDate.now());
        }
        
        return convertToDTO(facilityRepository.save(facility));
    }

    // 이미지 업로드
    @Transactional
    public FacilityImageDTO uploadImage(Long facilityId, MultipartFile file, String description) {
        Facility facility = facilityRepository.findById(facilityId)
            .orElseThrow(() -> new RuntimeException("시설물을 찾을 수 없습니다."));

        // null 파라미터 제거하고 file만 전달
        String imageUrl = imageService.saveImage(file);
        
        FacilityImage image = new FacilityImage();
        image.setFacility(facility);
        image.setImageUrl(imageUrl);
        image.setDescription(description);
        image.setFileName(file.getOriginalFilename());
        image.setFileSize(file.getSize());
        
        facility.getImages().add(image);
        facilityRepository.save(facility);
        
        return convertToImageDTO(image);
    }

    // 이미지 삭제
    @Transactional
    public void deleteImage(Long facilityId, Long imageId) {
        FacilityImage image = facilityImageRepository.findByIdAndFacilityId(imageId, facilityId)
            .orElseThrow(() -> new RuntimeException("이미지를 찾을 수 없습니다."));
        
        // 실제 파일 삭제
        imageService.deleteImage(image.getImageUrl());
        
        facilityImageRepository.delete(image);
    }

    // 이미지 정보 수정
    @Transactional
    public FacilityImageDTO updateImage(Long facilityId, Long imageId, String description) {
        FacilityImage image = facilityImageRepository.findByIdAndFacilityId(imageId, facilityId)
            .orElseThrow(() -> new RuntimeException("이미지를 찾을 수 없습니다."));
        
        image.setDescription(description);
        return convertToImageDTO(facilityImageRepository.save(image));
    }

    // 계약 정보 수정
    @Transactional
    public FacilityContractDTO updateContract(Long facilityId, Long contractId, FacilityContractDTO dto) {
        FacilityContract contract = contractRepository.findByIdAndFacilityId(contractId, facilityId)
            .orElseThrow(() -> new RuntimeException("계약을 찾을 수 없습니다."));
        
        contract.setContractNumber(dto.getContractNumber());
        contract.setStartDate(dto.getStartDate());
        contract.setEndDate(dto.getEndDate());
        contract.setContractAmount(dto.getContractAmount());
        contract.setIsPaid(dto.getIsPaid());
        contract.setPaidDate(dto.getPaidDate());
        contract.setVendorName(dto.getVendorName());
        contract.setVendorContact(dto.getVendorContact());
        contract.setDescription(dto.getDescription());
        
        // 시설물 위치 업데이트
        Facility facility = contract.getFacility();
        facility.setCurrentLocation(dto.getVendorName());
        
        return convertToContractDTO(contractRepository.save(contract));
    }

    // 계약 삭제
    @Transactional
    public void deleteContract(Long facilityId, Long contractId) {
        FacilityContract contract = contractRepository.findByIdAndFacilityId(contractId, facilityId)
            .orElseThrow(() -> new RuntimeException("계약을 찾을 수 없습니다."));
        
        contractRepository.delete(contract);
    }

    // 시설물별 이미지 목록 조회
    public List<FacilityImageDTO> getFacilityImages(Long facilityId) {
        Facility facility = facilityRepository.findById(facilityId)
            .orElseThrow(() -> new RuntimeException("시설물을 찾을 수 없습니다."));
        
        return facility.getImages().stream()
            .map(this::convertToImageDTO)
            .collect(Collectors.toList());
    }
} 