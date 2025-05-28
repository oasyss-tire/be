package com.inspection.service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;
import java.time.LocalDate;
import java.util.HashMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.inspection.dto.CompanyDTO;
import com.inspection.dto.CreateCompanyRequest;
import com.inspection.dto.UserResponseDTO;
import com.inspection.dto.BatchResponseDTO;
import com.inspection.entity.Company;
import com.inspection.entity.CompanyImage;
import com.inspection.entity.CompanyTrusteeHistory;
import com.inspection.entity.Role;
import com.inspection.entity.User;
import com.inspection.repository.CompanyRepository;
import com.inspection.repository.CompanyTrusteeHistoryRepository;
import com.inspection.repository.UserRepository;
import com.inspection.util.EncryptionUtil;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.util.StringUtils;

import com.inspection.dto.TrusteeChangeRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.inspection.entity.Code;
import com.inspection.repository.CodeRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyService {
    
    private final CompanyRepository companyRepository;
    private final CompanyImageStorageService companyImageStorageService;
    private final UserRepository userRepository;
    private final EncryptionUtil encryptionUtil;
    private final CompanyTrusteeHistoryRepository trusteeHistoryRepository;
    private final ObjectMapper objectMapper;
    private final ContractService contractService;
    private final TrusteeService trusteeService;
    private final PasswordEncoder passwordEncoder;
    private final CodeRepository codeRepository;
    
    /**
     * 새로운 회사를 생성합니다.
     */
    @Transactional
    public CompanyDTO createCompany(CreateCompanyRequest request) {
        // storeNumber 유효성 검사 (3자리 숫자인지)
        if (request.getStoreNumber() == null || !request.getStoreNumber().matches("^\\d{3}$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "매장번호(storeNumber)는 3자리 숫자여야 합니다.");
        }

        // 중복 정보 체크 (storeNumber 포함) - 신규 생성이므로 existingCompanyId는 null
        checkDuplicateInfo(request, null); 
        
        Company company = request.toEntity();
        // storeNumber는 request에서 직접 받아서 설정 (자동생성 로직 제거)
        company.setStoreNumber(request.getStoreNumber()); 
        
        // trusteeCode 자동 생성 (연도+점번+순번 형식)
        String trusteeCode = generateTrusteeCode(company.getStoreNumber());
        company.setTrusteeCode(trusteeCode);
        
        log.info("회사 생성 - 입력된 정보: storeNumber={}, 자동생성된 trusteeCode={}", company.getStoreNumber(), trusteeCode);
        
        // 등록자 정보가 없는 경우 기본값 설정
        if (company.getCreatedBy() == null || company.getCreatedBy().isEmpty()) {
            company.setCreatedBy("시스템");
        }
        
        // 지부 그룹 설정
        if (request.getBranchGroupId() != null && !request.getBranchGroupId().isEmpty()) {
            Code branchGroup = codeRepository.findById(request.getBranchGroupId())
                .orElseThrow(() -> new RuntimeException("해당 지부 그룹을 찾을 수 없습니다. ID: " + request.getBranchGroupId()));
            company.setBranchGroup(branchGroup);
        }
        
        // 민감한 개인정보 암호화
        encryptCompanySensitiveData(company);
        
        Company savedCompany = companyRepository.save(company);
        
        // CompanyTrusteeHistory 생성 및 저장
        CompanyTrusteeHistory trusteeHistory = new CompanyTrusteeHistory();
        trusteeHistory.setCompany(savedCompany);
        trusteeHistory.setActive(true);
        trusteeHistory.setReason("신규 등록");
        trusteeHistory.setModifiedBy(savedCompany.getCreatedBy());
        
        // Company 정보를 CompanyTrusteeHistory에 복사
        trusteeHistory.copyFromCompany(savedCompany);
        
        // 수탁자 사용자 계정 자동 생성 및 연결
        if (savedCompany.getTrusteeCode() != null && !savedCompany.getTrusteeCode().isEmpty()) {
            User user = createTrusteeUser(savedCompany);
            trusteeHistory.setUser(user);
        }
        
        // 수탁자 이력 저장
        trusteeHistoryRepository.save(trusteeHistory);
        
        log.info("회사 생성 완료: {}, 매장번호: {}, 수탁코드: {}, 등록자: {}, 수탁자 이력 생성됨", 
                savedCompany.getStoreName(), 
                savedCompany.getStoreNumber(),
                savedCompany.getTrusteeCode(),
                savedCompany.getCreatedBy());
                
        CompanyDTO companyDTO = CompanyDTO.fromEntity(savedCompany);
        
        // DTO 반환 시 복호화 처리
        decryptCompanyDTOSensitiveData(companyDTO);
        
        return companyDTO;
    }
    
    /**
     * 수탁사업자를 위한 사용자 계정을 자동 생성합니다.
     */
    private User createTrusteeUser(Company company) {
        // 이미 동일한 userId를 가진 사용자가 있는지 확인
        String userId = company.getTrusteeCode();
        if (userRepository.existsByUserId(userId)) {
            log.warn("이미 존재하는 사용자 ID입니다: {}", userId);
            return userRepository.findByUserId(userId).orElse(null);
        }
        
        // 비밀번호 자동 생성: trusteeCode + @123
        String initialPassword = "tb" + userId + "!@";
        
        User user = new User();
        user.setUserId(userId);
        user.setPassword(passwordEncoder.encode(initialPassword));
        user.setRole(Role.USER);
        user.setUserName(company.getRepresentativeName());
        user.setActive(true);
        user.setCompany(company);
        
        // 개인정보는 이미 암호화된 상태로 복사
        if (company.getEmail() != null && !company.getEmail().isEmpty()) {
            user.setEmail(company.getEmail());
        }
        
        if (company.getPhoneNumber() != null && !company.getPhoneNumber().isEmpty()) {
            user.setPhoneNumber(company.getPhoneNumber());
        }
        
        User savedUser = userRepository.save(user);
        log.info("수탁사업자 사용자 계정 자동 생성 완료: {}, 회사: {}", userId, company.getCompanyName());
        
        return savedUser;
    }
    
    // 매장번호 자동생성 -> 수동 입력 방식으로 변경되어 주석 처리 또는 삭제
    /*
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
            throw new IllegalStateException("매장 번호가 최대값(999)을 초과했습니다.");
        }
        
        // 3자리 숫자로 포맷팅 (001, 002, ...)
        return String.format("%03d", nextNumber);
    }
    */
    
    /**
     * 수탁코드를 자동생성합니다.
     * 형식: {연도(4자리) + 점번(3자리) + 순번(3자리)}
     * 예: 2025123001, 2025123002, 2025321001, 2026123001
     * 
     * @param storeNumber 점번 (3자리, 예: "123")
     * @return 자동생성된 수탁코드
     */
    private String generateTrusteeCode(String storeNumber) {
        // 현재 연도 가져오기
        int currentYear = java.time.LocalDate.now().getYear();
        String yearStr = String.valueOf(currentYear);
        
        log.info("수탁코드 자동생성 시작: 연도={}, 점번={}", currentYear, storeNumber);
        
        // 1. Company 테이블에서 기존 수탁코드 조회
        List<String> companyTrusteeCodes = companyRepository.findTrusteeCodesByYearAndStoreNumber(yearStr, storeNumber);
        
        // 2. CompanyTrusteeHistory 테이블에서도 기존 수탁코드 조회 (수탁자 변경 이력 포함)
        List<String> historyTrusteeCodes = companyRepository.findTrusteeCodesFromHistoryByYearAndStoreNumber(yearStr, storeNumber);
        
        // 3. 두 목록을 합쳐서 최대 순번 찾기
        int maxSequence = 0;
        
        // Company에서 최대 순번 찾기
        for (String trusteeCode : companyTrusteeCodes) {
            try {
                // trusteeCode 형식: YYYY{storeNumber}XXX (예: 2025123001)
                // 마지막 3자리가 순번
                if (trusteeCode != null && trusteeCode.length() >= 10) {
                    String sequenceStr = trusteeCode.substring(trusteeCode.length() - 3);
                    int sequence = Integer.parseInt(sequenceStr);
                    maxSequence = Math.max(maxSequence, sequence);
                }
            } catch (NumberFormatException e) {
                log.warn("잘못된 수탁코드 형식 (Company): {}", trusteeCode);
            }
        }
        
        // CompanyTrusteeHistory에서 최대 순번 찾기
        for (String trusteeCode : historyTrusteeCodes) {
            try {
                // trusteeCode 형식: YYYY{storeNumber}XXX (예: 2025123001)
                // 마지막 3자리가 순번
                if (trusteeCode != null && trusteeCode.length() >= 10) {
                    String sequenceStr = trusteeCode.substring(trusteeCode.length() - 3);
                    int sequence = Integer.parseInt(sequenceStr);
                    maxSequence = Math.max(maxSequence, sequence);
                }
            } catch (NumberFormatException e) {
                log.warn("잘못된 수탁코드 형식 (CompanyTrusteeHistory): {}", trusteeCode);
            }
        }
        
        // 4. 다음 순번 계산
        int nextSequence = maxSequence + 1;
        
        // 5. 순번 범위 체크 (001-999)
        if (nextSequence > 999) {
            throw new IllegalStateException(
                String.format("해당 연도(%d)와 점번(%s)의 수탁코드 순번이 최대값(999)을 초과했습니다.", currentYear, storeNumber));
        }
        
        // 6. 수탁코드 생성: 연도(4자리) + 점번(3자리) + 순번(3자리)
        String trusteeCode = String.format("%s%s%03d", yearStr, storeNumber, nextSequence);
        
        log.info("수탁코드 자동생성 완료: {} (연도: {}, 점번: {}, 순번: {})", 
            trusteeCode, currentYear, storeNumber, nextSequence);
        
        return trusteeCode;
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
            .map(this::decryptCompanyDTOSensitiveDataSafe)
            .collect(Collectors.toList());
    }
    
    /**
     * 활성화된 회사 목록을 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<CompanyDTO> getActiveCompanies() {
        return companyRepository.findByActiveTrue().stream()
            .map(CompanyDTO::fromEntity)
            .map(this::decryptCompanyDTOSensitiveDataSafe)
            .collect(Collectors.toList());
    }
    
    /**
     * 회사 ID로 회사 정보를 조회합니다.
     */
    @Transactional(readOnly = true)
    public CompanyDTO getCompanyById(Long companyId) {
        Company company = companyRepository.findWithImageById(companyId)
            .orElseThrow(() -> new RuntimeException("회사를 찾을 수 없습니다. ID: " + companyId));
        
        CompanyDTO companyDTO = CompanyDTO.fromEntity(company);
        
        // 암호화된 정보 복호화
        decryptCompanyDTOSensitiveData(companyDTO);
        
        return companyDTO;
    }
    
    /**
     * 회사 정보를 수정합니다.
     */
    @Transactional
    public CompanyDTO updateCompany(Long companyId, CreateCompanyRequest request) {
        try {
            Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("회사를 찾을 수 없습니다. ID: " + companyId));
            
            // storeNumber 유효성 검사 (3자리 숫자인지)
            if (request.getStoreNumber() == null || !request.getStoreNumber().matches("^\\d{3}$")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "매장번호(storeNumber)는 3자리 숫자여야 합니다.");
            }

            // 중복 정보 체크 (storeNumber 포함) - 수정이므로 companyId 전달
            checkDuplicateInfo(request, companyId);
            
            // 기존 등록자 정보 저장 (수정 시에는 등록자 정보를 변경하지 않음)
            String originalCreatedBy = company.getCreatedBy();
            
            // 회사 정보 업데이트 (일반 필드)
            company.setStoreCode(request.getStoreCode());
            company.setStoreNumber(request.getStoreNumber());
            company.setStoreName(request.getStoreName());
            company.setTrustee(request.getTrustee());
            company.setTrusteeCode(request.getTrusteeCode());
            company.setCompanyName(request.getCompanyName());
            company.setRepresentativeName(request.getRepresentativeName());
            company.setActive(request.isActive());
            company.setStartDate(request.getStartDate());
            company.setEndDate(request.getEndDate());
            company.setInsuranceStartDate(request.getInsuranceStartDate());
            company.setInsuranceEndDate(request.getInsuranceEndDate());
            company.setManagerName(request.getManagerName());
            
            // 지부 그룹 업데이트
            if (request.getBranchGroupId() != null && !request.getBranchGroupId().isEmpty()) {
                Code branchGroup = codeRepository.findById(request.getBranchGroupId())
                    .orElseThrow(() -> new RuntimeException("해당 지부 그룹을 찾을 수 없습니다. ID: " + request.getBranchGroupId()));
                company.setBranchGroup(branchGroup);
            } else {
                company.setBranchGroup(null); // 지부 그룹 제거
            }
            
            // 민감 정보 필드 암호화하여 업데이트
            if (request.getEmail() != null && !request.getEmail().isEmpty()) {
                company.setEmail(encryptionUtil.encrypt(request.getEmail()));
            }
            
            if (request.getSubBusinessNumber() != null && !request.getSubBusinessNumber().isEmpty()) {
                company.setSubBusinessNumber(request.getSubBusinessNumber());
            }
            
            if (request.getPhoneNumber() != null && !request.getPhoneNumber().isEmpty()) {
                company.setPhoneNumber(encryptionUtil.encrypt(request.getPhoneNumber()));
            }
            
            if (request.getStoreTelNumber() != null && !request.getStoreTelNumber().isEmpty()) {
                company.setStoreTelNumber(encryptionUtil.encrypt(request.getStoreTelNumber()));
            }
            
            if (request.getAddress() != null && !request.getAddress().isEmpty()) {
                company.setAddress(encryptionUtil.encrypt(request.getAddress()));
            }
            
            if (request.getBusinessNumber() != null && !request.getBusinessNumber().isEmpty()) {
                company.setBusinessNumber(encryptionUtil.encrypt(request.getBusinessNumber()));
            }
            
            if (request.getBusinessType() != null && !request.getBusinessType().isEmpty()) {
                company.setBusinessType(request.getBusinessType());
            }
            
            if (request.getBusinessCategory() != null && !request.getBusinessCategory().isEmpty()) {
                company.setBusinessCategory(request.getBusinessCategory());
            }
            
            // 기존 등록자 정보 복원 (수정 시에는 등록자 정보를 변경하지 않음)
            company.setCreatedBy(originalCreatedBy);
            
            Company updatedCompany = companyRepository.save(company);
            log.info("회사 정보 수정 완료: {}", updatedCompany.getStoreName());
            
            // 활성화된 CompanyTrusteeHistory 조회 후 업데이트
            try {
                CompanyTrusteeHistory activeHistory = trusteeHistoryRepository.findByCompanyAndIsActiveTrue(company)
                    .orElse(null);
                
                if (activeHistory != null) {
                    // Company 엔티티의 수탁자 관련 정보를 CompanyTrusteeHistory에 반영
                    activeHistory.setTrustee(updatedCompany.getTrustee());
                    activeHistory.setTrusteeCode(updatedCompany.getTrusteeCode());
                    activeHistory.setRepresentativeName(updatedCompany.getRepresentativeName());
                    activeHistory.setManagerName(updatedCompany.getManagerName());
                    activeHistory.setEmail(updatedCompany.getEmail());
                    activeHistory.setPhoneNumber(updatedCompany.getPhoneNumber());
                    activeHistory.setBusinessNumber(updatedCompany.getBusinessNumber());
                    activeHistory.setSubBusinessNumber(updatedCompany.getSubBusinessNumber());
                    activeHistory.setCompanyName(updatedCompany.getCompanyName());
                    activeHistory.setStoreTelNumber(updatedCompany.getStoreTelNumber());
                    activeHistory.setBusinessType(updatedCompany.getBusinessType());
                    activeHistory.setBusinessCategory(updatedCompany.getBusinessCategory());
                    activeHistory.setStartDate(updatedCompany.getStartDate());
                    activeHistory.setEndDate(updatedCompany.getEndDate());
                    activeHistory.setInsuranceStartDate(updatedCompany.getInsuranceStartDate());
                    activeHistory.setInsuranceEndDate(updatedCompany.getInsuranceEndDate());
                    
                    // 수정자 정보 설정 (로그인한 사용자 또는 시스템)
                    activeHistory.setModifiedBy(request.getCreatedBy() != null ? request.getCreatedBy() : "시스템");
                    activeHistory.setReason("회사 정보 수정에 따른 업데이트");
                    
                    trusteeHistoryRepository.save(activeHistory);
                    log.info("활성화된 수탁자 이력 정보도 함께 업데이트 완료: historyId={}, trustee={}", 
                        activeHistory.getId(), activeHistory.getTrustee());
                } else {
                    log.info("활성화된 수탁자 이력이 없어 수탁자 이력 업데이트를 건너뜁니다. companyId={}", companyId);
                }
            } catch (Exception e) {
                // 수탁자 이력 업데이트 실패는 회사 정보 업데이트에 영향을 주지 않도록 함
                log.warn("활성화된 수탁자 이력 업데이트 중 오류 발생: {}", e.getMessage(), e);
            }
            
            CompanyDTO updatedDTO = CompanyDTO.fromEntity(updatedCompany);
            decryptCompanyDTOSensitiveData(updatedDTO);
            
            return updatedDTO;
        } catch (ResponseStatusException ex) {
            // 이미 ResponseStatusException인 경우 그대로 전달
            throw ex;
        } catch (IllegalArgumentException ex) {
            // IllegalArgumentException을 ResponseStatusException으로 변환
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        } catch (Exception ex) {
            // 기타 예외는 로깅 후 사용자 친화적인 오류로 변환
            log.error("회사 정보 수정 중 오류 발생: {}", ex.getMessage(), ex);
            
            // 메시지에서 오류 원인 분석
            String errorMessage = ex.getMessage();
            if (errorMessage != null) {
                if (errorMessage.contains("trustee_code") || errorMessage.contains("TrusteeCode")) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 사용 중인 수탁코드입니다.");
                } else if (errorMessage.contains("sub_business_number") || errorMessage.contains("SubBusinessNumber")) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 사용 중인 종사업장번호입니다.");
                } else if (errorMessage.contains("business_number") || errorMessage.contains("BusinessNumber")) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 사용 중인 사업자번호입니다.");
                } else if (errorMessage.contains("store_code") || errorMessage.contains("StoreCode")) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 사용 중인 매장코드입니다.");
                }
            }
            
            // 기본 오류 메시지
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "회사 정보 수정 중 오류가 발생했습니다.");
        }
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
            .map(this::decryptCompanyDTOSensitiveDataSafe)
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
        
        // 활성화된 CompanyTrusteeHistory 확인 및 업데이트 (필요시)
        try {
            CompanyTrusteeHistory activeHistory = trusteeHistoryRepository.findByCompanyAndIsActiveTrue(company)
                .orElse(null);
            
            if (activeHistory != null) {
                log.info("활성화된 수탁자 이력이 있지만, 이미지 수정은 수탁자 이력에 영향을 주지 않습니다: historyId={}", 
                    activeHistory.getId());
            }
        } catch (Exception e) {
            // 수탁자 이력 조회 실패는 기능에 영향을 주지 않도록 처리
            log.warn("활성화된 수탁자 이력 조회 중 오류 발생: {}", e.getMessage());
        }
        
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
    
    /**
     * 회사에 소속된 사용자 목록을 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<UserResponseDTO> getUsersByCompanyId(Long companyId) {
        // 회사가 존재하는지 확인
        if (!companyRepository.existsById(companyId)) {
            throw new RuntimeException("회사를 찾을 수 없습니다. ID: " + companyId);
        }
        
        // 회사에 소속된 사용자 목록 조회
        List<User> users = userRepository.findByCompanyId(companyId);
        
        // UserResponseDTO로 변환
        return users.stream()
            .map(user -> {
                UserResponseDTO dto = new UserResponseDTO(user);
                
                // 이메일과 전화번호 복호화
                try {
                    if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                        dto.setEmail(encryptionUtil.decrypt(user.getEmail()));
                    }
                    
                    if (user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty()) {
                        dto.setPhoneNumber(encryptionUtil.decrypt(user.getPhoneNumber()));
                    }
                } catch (Exception e) {
                    log.error("사용자 정보 복호화 중 오류: {}", e.getMessage());
                }
                
                return dto;
            })
            .collect(Collectors.toList());
    }

    private void checkDuplicateInfo(CreateCompanyRequest request, Long existingCompanyId) {
        boolean isCreating = (existingCompanyId == null);

        // 매장코드 중복 체크
        if (StringUtils.hasText(request.getStoreCode())) {
            companyRepository.findByStoreCode(request.getStoreCode())
                    .ifPresent(foundCompany -> {
                        if (isCreating || !foundCompany.getId().equals(existingCompanyId)) { 
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 사용 중인 매장코드입니다: " + request.getStoreCode());
                        }
                    });
        }

        // 매장번호(storeNumber) 중복 체크
        if (StringUtils.hasText(request.getStoreNumber())) {
            companyRepository.findByStoreNumber(request.getStoreNumber())
                .ifPresent(foundCompany -> {
                    if (isCreating || !foundCompany.getId().equals(existingCompanyId)) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 사용 중인 매장번호입니다: " + request.getStoreNumber());
                    }
                });
        }

        // 사업자번호 중복 체크
        if (StringUtils.hasText(request.getBusinessNumber())) {
            try {
                // 암호화된 사업자번호로 조회
                String encryptedBusinessNumber = encryptionUtil.encrypt(request.getBusinessNumber());
                companyRepository.findByBusinessNumber(encryptedBusinessNumber)
                        .ifPresent(foundCompany -> {
                            if (isCreating || !foundCompany.getId().equals(existingCompanyId)) {
                                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 사용 중인 사업자번호입니다: " + request.getBusinessNumber());
                            }
                        });
            } catch (Exception e) {
                if (e instanceof ResponseStatusException) {
                    throw e;
                }
                log.error("사업자번호 암호화 또는 중복 체크 중 오류 발생: {}", e.getMessage(), e);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "사업자번호 중복 체크 중 오류가 발생했습니다.");
            }
        }
            
        // 수탁코드 중복 체크 (값이 있는 경우에만)
        if (StringUtils.hasText(request.getTrusteeCode())) {
            try {
                companyRepository.findByTrusteeCode(request.getTrusteeCode())
                        .ifPresent(foundCompany -> {
                            if (isCreating || !foundCompany.getId().equals(existingCompanyId)) {
                                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 사용 중인 수탁코드입니다: " + request.getTrusteeCode());
                            }
                        });
            } catch (Exception e) {
                if (e instanceof ResponseStatusException) {
                    throw e;
                }
                log.error("수탁코드 중복 체크 중 오류 발생: {}", e.getMessage(), e);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 사용 중인 수탁코드입니다: " + request.getTrusteeCode());
            }
        }
        
        // 종사업장번호 중복 체크 (값이 있는 경우에만)
        if (StringUtils.hasText(request.getSubBusinessNumber())) {
            try {
                companyRepository.findBySubBusinessNumber(request.getSubBusinessNumber())
                        .ifPresent(foundCompany -> {
                            if (isCreating || !foundCompany.getId().equals(existingCompanyId)) {
                                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 사용 중인 종사업장번호입니다: " + request.getSubBusinessNumber());
                            }
                        });
            } catch (Exception e) {
                if (e instanceof ResponseStatusException) {
                    throw e;
                }
                log.error("종사업장번호 중복 체크 중 오류 발생: {}", e.getMessage(), e);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 사용 중인 종사업장번호입니다: " + request.getSubBusinessNumber());
            }
        }
    }

    /**
     * 수탁자 정보를 변경하고 이력을 관리합니다.
     * @deprecated 대신 {@link TrusteeService#changeTrustee}를 사용하세요
     */
    @Deprecated
    @Transactional
    public CompanyDTO changeTrustee(Long companyId, TrusteeChangeRequest request) {
        return trusteeService.changeTrustee(companyId, request);
    }
    
    /**
     * 기존 수탁자와 재계약을 진행합니다.
     * @deprecated 대신 {@link TrusteeService#renewContract}를 사용하세요
     */
    @Deprecated
    @Transactional
    public Map<String, Object> renewContract(Long companyId, Map<String, Object> request, String userId) {
        return trusteeService.renewContract(companyId, request, userId);
    }

    /**
     * 여러 회사 정보를 일괄 등록합니다. (배치 처리)
     */
    @Transactional
    public BatchResponseDTO createCompanyBatch(List<CreateCompanyRequest> requests) {
        BatchResponseDTO response = new BatchResponseDTO();
        
        for (CreateCompanyRequest request : requests) {
            try {
                // 회사 정보 생성 (내부적으로 암호화 처리됨)
                CompanyDTO createdCompany = createCompany(request);
                
                // 성공 정보 추가
                String identifier = request.getCompanyName();
                if (identifier == null || identifier.isEmpty()) {
                    identifier = request.getStoreName();
                    if (identifier == null || identifier.isEmpty()) {
                        identifier = "회사 정보";
                    }
                }
                
                response.addSuccess(identifier, createdCompany.getId());
                log.info("배치 처리 - 회사 생성 성공: {}", identifier);
                
            } catch (Exception e) {
                // 오류 정보 추가
                String identifier = request.getCompanyName();
                if (identifier == null || identifier.isEmpty()) {
                    identifier = request.getStoreName();
                    if (identifier == null || identifier.isEmpty()) {
                        identifier = "알 수 없는 회사";
                    }
                }
                
                String errorMessage = e.getMessage();
                if (e instanceof ResponseStatusException) {
                    ResponseStatusException rse = (ResponseStatusException) e;
                    errorMessage = rse.getReason();
                }
                
                response.addFailure(identifier, errorMessage);
                log.error("배치 처리 - 회사 생성 실패: {}, 오류: {}", identifier, errorMessage);
            }
        }
        
        log.info("회사 배치 처리 완료 - 전체: {}, 성공: {}, 실패: {}", 
            response.getTotalCount(), response.getSuccessCount(), response.getFailCount());
        
        return response;
    }

    /**
     * 모든 회사 목록을 페이징 처리하여 조회합니다.
     */
    @Transactional(readOnly = true)
    public Page<CompanyDTO> getAllCompaniesWithPaging(Pageable pageable) {
        return companyRepository.findAll(pageable)
            .map(CompanyDTO::fromEntity)
            .map(this::decryptCompanyDTOSensitiveDataSafe);
    }
    
    /**
     * 활성화된 회사 목록을 페이징 처리하여 조회합니다.
     */
    @Transactional(readOnly = true)
    public Page<CompanyDTO> getActiveCompaniesWithPaging(Pageable pageable) {
        return companyRepository.findByActiveTrue(pageable)
            .map(CompanyDTO::fromEntity)
            .map(this::decryptCompanyDTOSensitiveDataSafe);
    }
    
    /**
     * 매장명으로 회사를 검색하고 페이징 처리하여 조회합니다.
     */
    @Transactional(readOnly = true)
    public Page<CompanyDTO> searchCompaniesByNameWithPaging(String storeName, Pageable pageable) {
        return companyRepository.findByStoreNameContaining(storeName, pageable)
            .map(CompanyDTO::fromEntity)
            .map(this::decryptCompanyDTOSensitiveDataSafe);
    }
    
    /**
     * 키워드로 회사를 검색하고 페이징 처리하여 조회합니다.
     */
    @Transactional(readOnly = true)
    public Page<CompanyDTO> searchCompaniesByKeywordWithPaging(String keyword, Pageable pageable) {
        return companyRepository.findByStoreNameContainingOrBusinessNumberContainingOrStoreCodeContaining(
                keyword, keyword, keyword, pageable)
            .map(CompanyDTO::fromEntity)
            .map(this::decryptCompanyDTOSensitiveDataSafe);
    }

    /**
     * 회사의 민감한 개인정보를 암호화합니다.
     */
    private void encryptCompanySensitiveData(Company company) {
        try {
            // 1. 연락처 정보 암호화
            if (company.getPhoneNumber() != null && !company.getPhoneNumber().isEmpty()) {
                company.setPhoneNumber(encryptionUtil.encrypt(company.getPhoneNumber()));
            }
            
            if (company.getEmail() != null && !company.getEmail().isEmpty()) {
                company.setEmail(encryptionUtil.encrypt(company.getEmail()));
            }
            
            if (company.getStoreTelNumber() != null && !company.getStoreTelNumber().isEmpty()) {
                company.setStoreTelNumber(encryptionUtil.encrypt(company.getStoreTelNumber()));
            }
            
            // 2. 개인 식별 정보 암호화
            if (company.getAddress() != null && !company.getAddress().isEmpty()) {
                company.setAddress(encryptionUtil.encrypt(company.getAddress()));
            }
            
            if (company.getBusinessNumber() != null && !company.getBusinessNumber().isEmpty()) {
                company.setBusinessNumber(encryptionUtil.encrypt(company.getBusinessNumber()));
            }
            
            log.info("회사 개인정보 암호화 완료: {}", company.getStoreName());
        } catch (Exception e) {
            log.error("회사 개인정보 암호화 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("개인정보 암호화 중 오류가 발생했습니다.", e);
        }
    }
    
    /**
     * 회사 DTO의 민감한 개인정보를 복호화합니다.
     */
    private void decryptCompanyDTOSensitiveData(CompanyDTO companyDTO) {
        try {
            // 1. 연락처 정보 복호화
            if (companyDTO.getPhoneNumber() != null && !companyDTO.getPhoneNumber().isEmpty()) {
                companyDTO.setPhoneNumber(encryptionUtil.decrypt(companyDTO.getPhoneNumber()));
            }
            
            if (companyDTO.getEmail() != null && !companyDTO.getEmail().isEmpty()) {
                companyDTO.setEmail(encryptionUtil.decrypt(companyDTO.getEmail()));
            }
            
            if (companyDTO.getStoreTelNumber() != null && !companyDTO.getStoreTelNumber().isEmpty()) {
                companyDTO.setStoreTelNumber(encryptionUtil.decrypt(companyDTO.getStoreTelNumber()));
            }
            
            // 2. 개인 식별 정보 복호화
            if (companyDTO.getAddress() != null && !companyDTO.getAddress().isEmpty()) {
                companyDTO.setAddress(encryptionUtil.decrypt(companyDTO.getAddress()));
            }
            
            if (companyDTO.getBusinessNumber() != null && !companyDTO.getBusinessNumber().isEmpty()) {
                companyDTO.setBusinessNumber(encryptionUtil.decrypt(companyDTO.getBusinessNumber()));
            }
        } catch (Exception e) {
            log.error("회사 개인정보 복호화 중 오류 발생: {}", e.getMessage(), e);
            // 복호화 실패 시 원본 값 유지
        }
    }

    /**
     * 회사 DTO의 민감한 정보를 안전하게 복호화합니다.
     * 복호화 중 오류가 발생하면 원본 값을 유지합니다.
     */
    private CompanyDTO decryptCompanyDTOSensitiveDataSafe(CompanyDTO dto) {
        try {
            decryptCompanyDTOSensitiveData(dto);
        } catch (Exception e) {
            log.warn("회사 정보 복호화 중 오류 발생: {}, companyId: {}", e.getMessage(), dto.getId());
        }
        return dto;
    }
} 