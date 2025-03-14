package com.inspection.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.inspection.dto.CompanyDTO;
import com.inspection.dto.CreateCompanyRequest;
import com.inspection.entity.Company;
import com.inspection.entity.CompanyImage;
import com.inspection.repository.CompanyRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyService {
    
    private final CompanyRepository companyRepository;
    private final CompanyImageStorageService companyImageStorageService;
    
    /**
     * 새로운 회사를 생성합니다.
     */
    @Transactional
    public CompanyDTO createCompany(CreateCompanyRequest request) {
        // 매장코드 중복 체크
        if (companyRepository.findByStoreCode(request.getStoreCode()).isPresent()) {
            throw new RuntimeException("이미 존재하는 매장코드입니다: " + request.getStoreCode());
        }
        
        // 사업자번호 중복 체크 (사업자번호가 있는 경우)
        if (request.getBusinessNumber() != null && !request.getBusinessNumber().isEmpty() &&
            companyRepository.findByBusinessNumber(request.getBusinessNumber()).isPresent()) {
            throw new RuntimeException("이미 등록된 사업자번호입니다: " + request.getBusinessNumber());
        }
        
        // storeNumber 자동 생성 (001~999)
        String storeNumber = generateNextStoreNumber();
        
        Company company = request.toEntity();
        company.setStoreNumber(storeNumber); // 자동 생성된 storeNumber 설정
        
        // 등록자 정보가 없는 경우 기본값 설정
        if (company.getCreatedBy() == null || company.getCreatedBy().isEmpty()) {
            company.setCreatedBy("시스템");
        }
        
        Company savedCompany = companyRepository.save(company);
        
        log.info("회사 생성 완료: {}, 매장번호: {}, 등록자: {}", 
                savedCompany.getStoreName(), 
                savedCompany.getStoreNumber(),
                savedCompany.getCreatedBy());
        return CompanyDTO.fromEntity(savedCompany);
    }
    
    // 매장번호 자동생성
    private String generateNextStoreNumber() {
        // 가장 큰 매장 번호 조회
        String maxStoreNumber = companyRepository.findMaxStoreNumber();
        
        int nextNumber = 1; // 기본값은 1
        
        if (maxStoreNumber != null && !maxStoreNumber.isEmpty()) {
            try {
                nextNumber = Integer.parseInt(maxStoreNumber) + 1;
            } catch (NumberFormatException e) {
                log.warn("매장 번호 파싱 오류, 기본값 1로 설정: {}", maxStoreNumber);
            }
        }
        
        // 999를 초과하면 예외 발생
        if (nextNumber > 999) {
            throw new RuntimeException("매장 번호가 최대값(999)을 초과했습니다.");
        }
        
        // 3자리 숫자로 포맷팅 (001, 002, ...)
        return String.format("%03d", nextNumber);
    }
    
    /**
     * 회사 이미지를 업로드합니다.
     */
    @Transactional
    public CompanyDTO uploadCompanyImages(Long companyId, 
                                         MultipartFile frontImage,
                                         MultipartFile backImage,
                                         MultipartFile leftSideImage,
                                         MultipartFile rightSideImage,
                                         MultipartFile fullImage) {
        
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new RuntimeException("회사를 찾을 수 없습니다. ID: " + companyId));
        
        CompanyImage companyImage = company.getCompanyImage();
        if (companyImage == null) {
            companyImage = new CompanyImage();
            companyImage.setCompany(company);
            company.setCompanyImage(companyImage);
        }
        
        // 이미지 저장 및 URL 설정
        if (frontImage != null && !frontImage.isEmpty()) {
            String frontImageUrl = companyImageStorageService.storeFile(frontImage, "company");
            companyImage.setFrontImage(frontImageUrl);
        }
        
        if (backImage != null && !backImage.isEmpty()) {
            String backImageUrl = companyImageStorageService.storeFile(backImage, "company");
            companyImage.setBackImage(backImageUrl);
        }
        
        if (leftSideImage != null && !leftSideImage.isEmpty()) {
            String leftSideImageUrl = companyImageStorageService.storeFile(leftSideImage, "company");
            companyImage.setLeftSideImage(leftSideImageUrl);
        }
        
        if (rightSideImage != null && !rightSideImage.isEmpty()) {
            String rightSideImageUrl = companyImageStorageService.storeFile(rightSideImage, "company");
            companyImage.setRightSideImage(rightSideImageUrl);
        }
        
        if (fullImage != null && !fullImage.isEmpty()) {
            String fullImageUrl = companyImageStorageService.storeFile(fullImage, "company");
            companyImage.setFullImage(fullImageUrl);
        }
        
        Company savedCompany = companyRepository.save(company);
        log.info("회사 이미지 업로드 완료: {}", savedCompany.getStoreName());
        
        return CompanyDTO.fromEntity(savedCompany);
    }
    
    /**
     * 모든 회사 목록을 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<CompanyDTO> getAllCompanies() {
        return companyRepository.findAll().stream()
            .map(CompanyDTO::fromEntity)
            .collect(Collectors.toList());
    }
    
    /**
     * 활성화된 회사 목록을 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<CompanyDTO> getActiveCompanies() {
        return companyRepository.findByActiveTrue().stream()
            .map(CompanyDTO::fromEntity)
            .collect(Collectors.toList());
    }
    
    /**
     * 회사 ID로 회사 정보를 조회합니다.
     */
    @Transactional(readOnly = true)
    public CompanyDTO getCompanyById(Long companyId) {
        Company company = companyRepository.findWithImageById(companyId)
            .orElseThrow(() -> new RuntimeException("회사를 찾을 수 없습니다. ID: " + companyId));
        
        return CompanyDTO.fromEntity(company);
    }
    
    /**
     * 회사 정보를 수정합니다.
     */
    @Transactional
    public CompanyDTO updateCompany(Long companyId, CreateCompanyRequest request) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new RuntimeException("회사를 찾을 수 없습니다. ID: " + companyId));
        
        // 매장코드 중복 체크 (변경된 경우)
        if (!company.getStoreCode().equals(request.getStoreCode()) &&
            companyRepository.findByStoreCode(request.getStoreCode()).isPresent()) {
            throw new RuntimeException("이미 존재하는 매장코드입니다: " + request.getStoreCode());
        }
        
        // 사업자번호 중복 체크 (변경된 경우)
        if (request.getBusinessNumber() != null && !request.getBusinessNumber().isEmpty() &&
            !request.getBusinessNumber().equals(company.getBusinessNumber()) &&
            companyRepository.findByBusinessNumber(request.getBusinessNumber()).isPresent()) {
            throw new RuntimeException("이미 등록된 사업자번호입니다: " + request.getBusinessNumber());
        }
        
        // 기존 등록자 정보 저장 (수정 시에는 등록자 정보를 변경하지 않음)
        String originalCreatedBy = company.getCreatedBy();
        
        // 회사 정보 업데이트
        company.setStoreCode(request.getStoreCode());
        company.setStoreNumber(request.getStoreNumber());
        company.setStoreName(request.getStoreName());
        company.setTrustee(request.getTrustee());
        company.setTrusteeCode(request.getTrusteeCode());
        company.setBusinessNumber(request.getBusinessNumber());
        company.setCompanyName(request.getCompanyName());
        company.setRepresentativeName(request.getRepresentativeName());
        company.setActive(request.isActive());
        company.setStartDate(request.getStartDate());
        company.setEndDate(request.getEndDate());
        company.setManagerName(request.getManagerName());
        company.setEmail(request.getEmail());
        company.setSubBusinessNumber(request.getSubBusinessNumber());
        company.setPhoneNumber(request.getPhoneNumber());
        company.setAddress(request.getAddress());
        company.setBusinessType(request.getBusinessType());
        company.setBusinessCategory(request.getBusinessCategory());
        
        // 기존 등록자 정보 복원 (수정 시에는 등록자 정보를 변경하지 않음)
        company.setCreatedBy(originalCreatedBy);
        
        Company updatedCompany = companyRepository.save(company);
        log.info("회사 정보 수정 완료: {}", updatedCompany.getStoreName());
        
        return CompanyDTO.fromEntity(updatedCompany);
    }
    
    /**
     * 회사를 삭제합니다.
     */
    @Transactional
    public void deleteCompany(Long companyId) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new RuntimeException("회사를 찾을 수 없습니다. ID: " + companyId));
        
        // 회사 이미지가 있는 경우 이미지 파일 삭제
        if (company.getCompanyImage() != null) {
            CompanyImage image = company.getCompanyImage();
            
            if (image.getFrontImage() != null) {
                companyImageStorageService.deleteFile(image.getFrontImage());
            }
            if (image.getBackImage() != null) {
                companyImageStorageService.deleteFile(image.getBackImage());
            }
            if (image.getLeftSideImage() != null) {
                companyImageStorageService.deleteFile(image.getLeftSideImage());
            }
            if (image.getRightSideImage() != null) {
                companyImageStorageService.deleteFile(image.getRightSideImage());
            }
            if (image.getFullImage() != null) {
                companyImageStorageService.deleteFile(image.getFullImage());
            }
        }
        
        companyRepository.delete(company);
        log.info("회사 삭제 완료: {}", company.getStoreName());
    }
    
    /**
     * 회사 상태를 변경합니다 (활성화/비활성화).
     */
    @Transactional
    public CompanyDTO toggleCompanyStatus(Long companyId) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new RuntimeException("회사를 찾을 수 없습니다. ID: " + companyId));
        
        company.setActive(!company.isActive());
        Company updatedCompany = companyRepository.save(company);
        
        String status = updatedCompany.isActive() ? "활성화" : "비활성화";
        log.info("회사 상태 변경 완료: {} - {}", updatedCompany.getStoreName(), status);
        
        return CompanyDTO.fromEntity(updatedCompany);
    }
    
    /**
     * 매장명으로 회사를 검색합니다.
     */
    @Transactional(readOnly = true)
    public List<CompanyDTO> searchCompaniesByName(String storeName) {
        return companyRepository.findByStoreNameContaining(storeName).stream()
            .map(CompanyDTO::fromEntity)
            .collect(Collectors.toList());
    }
    
    /**
     * 회사 이미지를 수정합니다. 기존 이미지가 있으면 삭제하고 새 이미지로 교체합니다.
     */
    @Transactional
    public CompanyDTO updateCompanyImages(Long companyId, 
                                         MultipartFile frontImage,
                                         MultipartFile backImage,
                                         MultipartFile leftSideImage,
                                         MultipartFile rightSideImage,
                                         MultipartFile fullImage) {
        
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new RuntimeException("회사를 찾을 수 없습니다. ID: " + companyId));
        
        CompanyImage companyImage = company.getCompanyImage();
        if (companyImage == null) {
            companyImage = new CompanyImage();
            companyImage.setCompany(company);
            company.setCompanyImage(companyImage);
        }
        
        // 이미지 수정 (기존 이미지 삭제 후 새 이미지 저장)
        if (frontImage != null && !frontImage.isEmpty()) {
            // 기존 이미지 삭제
            if (companyImage.getFrontImage() != null) {
                companyImageStorageService.deleteFile(companyImage.getFrontImage());
            }
            // 새 이미지 저장
            String frontImageUrl = companyImageStorageService.storeFile(frontImage, "company");
            companyImage.setFrontImage(frontImageUrl);
        }
        
        if (backImage != null && !backImage.isEmpty()) {
            // 기존 이미지 삭제
            if (companyImage.getBackImage() != null) {
                companyImageStorageService.deleteFile(companyImage.getBackImage());
            }
            // 새 이미지 저장
            String backImageUrl = companyImageStorageService.storeFile(backImage, "company");
            companyImage.setBackImage(backImageUrl);
        }
        
        if (leftSideImage != null && !leftSideImage.isEmpty()) {
            // 기존 이미지 삭제
            if (companyImage.getLeftSideImage() != null) {
                companyImageStorageService.deleteFile(companyImage.getLeftSideImage());
            }
            // 새 이미지 저장
            String leftSideImageUrl = companyImageStorageService.storeFile(leftSideImage, "company");
            companyImage.setLeftSideImage(leftSideImageUrl);
        }
        
        if (rightSideImage != null && !rightSideImage.isEmpty()) {
            // 기존 이미지 삭제
            if (companyImage.getRightSideImage() != null) {
                companyImageStorageService.deleteFile(companyImage.getRightSideImage());
            }
            // 새 이미지 저장
            String rightSideImageUrl = companyImageStorageService.storeFile(rightSideImage, "company");
            companyImage.setRightSideImage(rightSideImageUrl);
        }
        
        if (fullImage != null && !fullImage.isEmpty()) {
            // 기존 이미지 삭제
            if (companyImage.getFullImage() != null) {
                companyImageStorageService.deleteFile(companyImage.getFullImage());
            }
            // 새 이미지 저장
            String fullImageUrl = companyImageStorageService.storeFile(fullImage, "company");
            companyImage.setFullImage(fullImageUrl);
        }
        
        Company savedCompany = companyRepository.save(company);
        log.info("회사 이미지 수정 완료: {}", savedCompany.getStoreName());
        
        return CompanyDTO.fromEntity(savedCompany);
    }
    
    /**
     * 특정 회사 이미지를 삭제합니다.
     * 
     * @param companyId 회사 ID
     * @param imageType 이미지 타입 (front, back, leftSide, rightSide, full)
     * @return 업데이트된 회사 정보
     */
    @Transactional
    public CompanyDTO deleteCompanyImage(Long companyId, String imageType) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new RuntimeException("회사를 찾을 수 없습니다. ID: " + companyId));
        
        CompanyImage companyImage = company.getCompanyImage();
        if (companyImage == null) {
            throw new RuntimeException("회사 이미지 정보가 없습니다. 회사 ID: " + companyId);
        }
        
        // 이미지 타입에 따라 해당 이미지 삭제
        switch (imageType.toLowerCase()) {
            case "front":
                if (companyImage.getFrontImage() != null) {
                    companyImageStorageService.deleteFile(companyImage.getFrontImage());
                    companyImage.setFrontImage(null);
                    log.info("회사 전면 이미지 삭제 완료: {}", company.getStoreName());
                }
                break;
            case "back":
                if (companyImage.getBackImage() != null) {
                    companyImageStorageService.deleteFile(companyImage.getBackImage());
                    companyImage.setBackImage(null);
                    log.info("회사 후면 이미지 삭제 완료: {}", company.getStoreName());
                }
                break;
            case "leftside":
                if (companyImage.getLeftSideImage() != null) {
                    companyImageStorageService.deleteFile(companyImage.getLeftSideImage());
                    companyImage.setLeftSideImage(null);
                    log.info("회사 좌측 이미지 삭제 완료: {}", company.getStoreName());
                }
                break;
            case "rightside":
                if (companyImage.getRightSideImage() != null) {
                    companyImageStorageService.deleteFile(companyImage.getRightSideImage());
                    companyImage.setRightSideImage(null);
                    log.info("회사 우측 이미지 삭제 완료: {}", company.getStoreName());
                }
                break;
            case "full":
                if (companyImage.getFullImage() != null) {
                    companyImageStorageService.deleteFile(companyImage.getFullImage());
                    companyImage.setFullImage(null);
                    log.info("회사 전체 이미지 삭제 완료: {}", company.getStoreName());
                }
                break;
            default:
                throw new RuntimeException("지원하지 않는 이미지 타입입니다: " + imageType);
        }
        
        Company savedCompany = companyRepository.save(company);
        return CompanyDTO.fromEntity(savedCompany);
    }
} 