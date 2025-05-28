package com.inspection.service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inspection.dto.CompanyDTO;
import com.inspection.dto.CreateContractRequest;
import com.inspection.dto.CreateParticipantRequest;
import com.inspection.dto.TrusteeChangeRequest;
import com.inspection.entity.Company;
import com.inspection.entity.CompanyTrusteeHistory;
import com.inspection.entity.Contract;
import com.inspection.entity.Role;
import com.inspection.entity.User;
import com.inspection.repository.CompanyRepository;
import com.inspection.repository.CompanyTrusteeHistoryRepository;
import com.inspection.repository.UserRepository;
import com.inspection.util.EncryptionUtil;

import com.inspection.dto.TrusteeHistoryDTO;
import com.inspection.entity.ContractTemplateMapping;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 수탁자 정보 관련 서비스
 * 수탁자 변경, 재계약 등의 기능을 담당
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TrusteeService {
    
    private final CompanyRepository companyRepository;
    private final CompanyTrusteeHistoryRepository trusteeHistoryRepository;
    private final EncryptionUtil encryptionUtil;
    private final ObjectMapper objectMapper;
    private final ContractService contractService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
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
        
        log.info("수탁코드 자동생성 시작 (수탁자 변경): 연도={}, 점번={}", currentYear, storeNumber);
        
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
        
        log.info("수탁코드 자동생성 완료 (수탁자 변경): {} (연도: {}, 점번: {}, 순번: {})", 
            trusteeCode, currentYear, storeNumber, nextSequence);
        
        return trusteeCode;
    }

    /**
     * 수탁자 변경 요청 정보로 사용자 계정을 자동 생성합니다.
     * @param request 수탁자 변경 요청 정보
     * @param company 연결할 회사 객체
     * @param trusteeCode 자동생성된 수탁코드
     * @return 생성된 사용자 객체
     */
    private User createTrusteeUser(TrusteeChangeRequest request, Company company, String trusteeCode) {
        // 이미 동일한 userId를 가진 사용자가 있는지 확인
        String userId = trusteeCode; // 자동생성된 trusteeCode 사용
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
        user.setUserName(request.getRepresentativeName());
        user.setActive(true);
        // Company 연결 설정
        user.setCompany(company);
        
        // 개인정보 암호화
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            user.setEmail(encryptionUtil.encrypt(request.getEmail()));
        }
        
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isEmpty()) {
            user.setPhoneNumber(encryptionUtil.encrypt(request.getPhoneNumber()));
        }
        
        User savedUser = userRepository.save(user);
        log.info("수탁자 변경에 대한 사용자 계정 자동 생성 완료: {}, 수탁자: {}, 회사: {}", 
                userId, request.getTrustee(), company.getStoreName());
        
        return savedUser;
    }

    /**
     * 수탁자 정보를 변경하고 이력을 관리합니다.
     * 새 수탁자 정보는 CompanyTrusteeHistory에만 저장하고 시작일이 되면 스케줄러가 활성화합니다.
     */
    @Transactional
    public CompanyDTO changeTrustee(Long companyId, TrusteeChangeRequest request) {
        // 회사 조회
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회사를 찾을 수 없습니다. ID: " + companyId));
        
        // 기존 활성 수탁자 이력 조회
        CompanyTrusteeHistory activeHistory = trusteeHistoryRepository.findByCompanyAndIsActiveTrue(company)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "현재 활성화된 수탁자 정보를 찾을 수 없습니다."));
        
        // 시작일 유효성 검사
        if (request.getStartDate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "새 수탁자의 시작일은 필수입니다.");
        }
        
        // 기존 활성 수탁자 이력은 종료일까지 활성 상태 유지 (즉시 비활성화하지 않음)
        log.info("기존 수탁자 이력 유지 (종료일까지 활성 상태): historyId={}, trustee={}, 종료일={}", 
                activeHistory.getId(), activeHistory.getTrustee(), activeHistory.getEndDate());
        
        // 새 수탁자 이력 생성 (처음에는 비활성 상태)
        CompanyTrusteeHistory newHistory = new CompanyTrusteeHistory();
        newHistory.setCompany(company);
        newHistory.setActive(false); // 스케줄러가 시작일에 활성화할 때까지 비활성 상태 유지
        
        // 수탁코드 자동생성 (연도+점번+순번 형식)
        String autoGeneratedTrusteeCode = generateTrusteeCode(company.getStoreNumber());
        
        // 새 수탁자 정보 설정 (요청으로 받은 데이터 사용, 단 trusteeCode는 자동생성)
        newHistory.setTrustee(request.getTrustee());
        newHistory.setTrusteeCode(autoGeneratedTrusteeCode); // 자동생성된 수탁코드 사용
        newHistory.setRepresentativeName(request.getRepresentativeName());
        newHistory.setCompanyName(request.getCompanyName());
        newHistory.setManagerName(request.getManagerName());
        newHistory.setSubBusinessNumber(request.getSubBusinessNumber());
        newHistory.setBusinessType(request.getBusinessType());
        newHistory.setBusinessCategory(request.getBusinessCategory());
        
        log.info("수탁자 변경 - 자동생성된 수탁코드: {}, 회사: {}, 점번: {}", 
            autoGeneratedTrusteeCode, company.getStoreName(), company.getStoreNumber());
        
        // 암호화가 필요한 필드 처리
        try {
            // 사업자번호 암호화
            if (request.getBusinessNumber() != null && !request.getBusinessNumber().isEmpty()) {
                newHistory.setBusinessNumber(encryptionUtil.encrypt(request.getBusinessNumber()));
            } else {
                newHistory.setBusinessNumber(request.getBusinessNumber());
            }
            
            // 이메일 암호화
            if (request.getEmail() != null && !request.getEmail().isEmpty()) {
                newHistory.setEmail(encryptionUtil.encrypt(request.getEmail()));
            } else {
                newHistory.setEmail(request.getEmail());
            }
            
            // 휴대폰번호 암호화
            if (request.getPhoneNumber() != null && !request.getPhoneNumber().isEmpty()) {
                newHistory.setPhoneNumber(encryptionUtil.encrypt(request.getPhoneNumber()));
            } else {
                newHistory.setPhoneNumber(request.getPhoneNumber());
            }
            
            // 매장 전화번호 암호화
            if (request.getStoreTelNumber() != null && !request.getStoreTelNumber().isEmpty()) {
                newHistory.setStoreTelNumber(encryptionUtil.encrypt(request.getStoreTelNumber()));
            } else {
                newHistory.setStoreTelNumber(request.getStoreTelNumber());
            }
        } catch (Exception e) {
            log.error("수탁자 정보 암호화 중 오류 발생: {}", e.getMessage(), e);
            // 암호화 실패 시 원본 값 사용
            newHistory.setBusinessNumber(request.getBusinessNumber());
            newHistory.setEmail(request.getEmail());
            newHistory.setPhoneNumber(request.getPhoneNumber());
            newHistory.setStoreTelNumber(request.getStoreTelNumber());
        }
        
        newHistory.setStartDate(request.getStartDate());
        newHistory.setEndDate(request.getEndDate());
        newHistory.setInsuranceStartDate(request.getInsuranceStartDate());
        newHistory.setInsuranceEndDate(request.getInsuranceEndDate());
        
        // 변경 사유 및 수정자 정보 설정
        newHistory.setReason(request.getReason() != null ? request.getReason() : "수탁자 변경");
        newHistory.setModifiedBy(request.getModifiedBy());
        
        // 새 수탁자를 위한 User 계정 생성 (자동 생성)
        if (autoGeneratedTrusteeCode != null && !autoGeneratedTrusteeCode.isEmpty()) {
            try {
                // TrusteeChangeRequest와 자동생성된 trusteeCode로 User 생성
                User user = createTrusteeUser(request, company, autoGeneratedTrusteeCode);
                
                // 생성된 User를 CompanyTrusteeHistory에 연결
                if (user != null) {
                    newHistory.setUser(user);
                    log.info("새 수탁자를 위한 사용자 계정 생성 완료: userId={}, userName={}", 
                            user.getUserId(), user.getUserName());
                }
            } catch (Exception e) {
                // 사용자 계정 생성 중 오류가 발생해도 수탁자 정보 변경은 계속 진행
                log.error("새 수탁자를 위한 사용자 계정 생성 중 오류 발생: {}", e.getMessage(), e);
            }
        }
        
        // 새 수탁자 이력 저장
        CompanyTrusteeHistory savedHistory = trusteeHistoryRepository.save(newHistory);
        
        // Company 엔티티 정보는 업데이트하지 않음 (스케줄러가 시작일에 업데이트)
        
        log.info("새 수탁자 정보 등록 완료 (시작일 {} 부터 활성화 예정): 회사={}, 이전 수탁자={}, 새 수탁자={}", 
                request.getStartDate(), company.getStoreName(), activeHistory.getTrustee(), newHistory.getTrustee());
        
        // 현재 활성화된 수탁자 정보로 DTO 반환 (기존 활성 수탁자 정보 유지)
        return CompanyDTO.fromEntity(company);
    }
    
    /**
     * 기존 수탁자와 재계약을 진행합니다.
     * 1. 기존 CompanyTrusteeHistory 레코드의 isActive를 false로 변경
     * 2. 새 CompanyTrusteeHistory 레코드 생성 (isActive=true, reason="재계약")
     * 3. 새 Contract 생성 (계약 구분 코드에 재계약 회차 표시)
     * 4. 새 CompanyTrusteeHistory 레코드에 새 contract 연결
     * 
     * 입력값은 최소화하여 계약 날짜만 필수로 받고, 나머지는 이전 계약에서 자동으로 가져옵니다.
     */
    @Transactional
    public Map<String, Object> renewContract(Long companyId, Map<String, Object> request, String userId) {
        // 회사 조회
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회사를 찾을 수 없습니다. ID: " + companyId));
        
        // 기존 활성 수탁자 이력 조회
        CompanyTrusteeHistory activeHistory = trusteeHistoryRepository.findByCompanyAndIsActiveTrue(company)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "현재 활성화된 수탁자 정보를 찾을 수 없습니다."));
        
        // 이전 계약 정보 조회 (필수)
        Contract previousContract = activeHistory.getContract();
        if (previousContract == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "이전 계약 정보가 없습니다. 신규 계약으로 진행해 주세요.");
        }
        
        // 1. 기존 활성 수탁자 이력은 종료일까지 활성 상태 유지
        log.info("기존 수탁자 이력 유지 (종료일까지 활성 상태): historyId={}, trustee={}, 종료일={}", 
                activeHistory.getId(), activeHistory.getTrustee(), activeHistory.getEndDate());
        
        // 2. 새 수탁자 이력 생성 (시작일까지 비활성 상태로 유지)
        CompanyTrusteeHistory newHistory = new CompanyTrusteeHistory();
        newHistory.setCompany(company);
        newHistory.setActive(false); // 스케줄러가 시작일에 활성화할 때까지 비활성 상태 유지
        
        // 기존 정보 복사
        newHistory.setTrustee(activeHistory.getTrustee());
        newHistory.setTrusteeCode(activeHistory.getTrusteeCode());
        newHistory.setRepresentativeName(activeHistory.getRepresentativeName());
        newHistory.setBusinessNumber(activeHistory.getBusinessNumber());
        newHistory.setCompanyName(activeHistory.getCompanyName());
        newHistory.setManagerName(activeHistory.getManagerName());
        newHistory.setEmail(activeHistory.getEmail());
        newHistory.setPhoneNumber(activeHistory.getPhoneNumber());
        newHistory.setSubBusinessNumber(activeHistory.getSubBusinessNumber());
        newHistory.setStoreTelNumber(activeHistory.getStoreTelNumber());
        newHistory.setBusinessType(activeHistory.getBusinessType());
        newHistory.setBusinessCategory(activeHistory.getBusinessCategory());
        
        // 재계약 시에는 기존 User를 그대로 사용
        newHistory.setUser(activeHistory.getUser());
        
        // 필수 입력값 확인 - 계약 시작일과 종료일
        LocalDate startDate = null;
        LocalDate expiryDate = null;
        LocalDate insuranceStartDate = null;
        LocalDate insuranceEndDate = null;
        
        try {
            // startDate 파싱 (필수)
            if (request.containsKey("startDate")) {
                String startDateStr = (String) request.get("startDate");
                startDate = LocalDate.parse(startDateStr);
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "계약 시작일(startDate)은 필수 입력값입니다.");
            }
            
            // expiryDate 파싱 (필수)
            if (request.containsKey("expiryDate")) {
                String expiryDateStr = (String) request.get("expiryDate");
                expiryDate = LocalDate.parse(expiryDateStr);
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "계약 종료일(expiryDate)은 필수 입력값입니다.");
            }
            
            // insuranceStartDate 파싱 (선택적 - 없으면 계약 시작일과 동일하게 설정)
            if (request.containsKey("insuranceStartDate")) {
                String insuranceStartDateStr = (String) request.get("insuranceStartDate");
                insuranceStartDate = LocalDate.parse(insuranceStartDateStr);
            } else {
                insuranceStartDate = startDate; // 계약 시작일과 동일하게 설정
            }
            
            // insuranceEndDate 파싱 (선택적 - 없으면 계약 종료일과 동일하게 설정)
            if (request.containsKey("insuranceEndDate")) {
                String insuranceEndDateStr = (String) request.get("insuranceEndDate");
                insuranceEndDate = LocalDate.parse(insuranceEndDateStr);
            } else {
                insuranceEndDate = expiryDate; // 계약 종료일과 동일하게 설정
            }
        } catch (Exception e) {
            log.error("날짜 파싱 중 오류 발생: {}", e.getMessage(), e);
            if (e instanceof ResponseStatusException) {
                throw e;
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "유효하지 않은 날짜 형식입니다. YYYY-MM-DD 형식으로 입력해주세요.");
        }
        
        // 새 이력에 날짜 정보 설정
        newHistory.setStartDate(startDate);
        newHistory.setEndDate(expiryDate);
        newHistory.setInsuranceStartDate(insuranceStartDate);
        newHistory.setInsuranceEndDate(insuranceEndDate);
        
        // 회사 엔티티에도 날짜 정보 업데이트
        company.setStartDate(startDate);
        company.setEndDate(expiryDate);
        company.setInsuranceStartDate(insuranceStartDate);
        company.setInsuranceEndDate(insuranceEndDate);
        
        // 변경 사유 및 수정자 정보 설정
        newHistory.setReason("재계약");
        newHistory.setModifiedBy(userId != null ? userId : "시스템");
        
        // 새 수탁자 이력 저장
        CompanyTrusteeHistory savedHistory = trusteeHistoryRepository.save(newHistory);
        log.info("새 수탁자 이력 생성 완료: historyId={}, trustee={}", savedHistory.getId(), savedHistory.getTrustee());
        
        // Company 엔티티 정보 업데이트
        Company updatedCompany = companyRepository.save(company);
        
        // 3. 계약 생성 요청 준비 (이전 계약 정보 기반)
        CreateContractRequest contractRequest = new CreateContractRequest();
        
        // 기본 정보 설정 
        contractRequest.setCompanyId(companyId);
        contractRequest.setTitle(company.getCompanyName() + " - 재계약");
        contractRequest.setDescription("재계약: " + company.getCompanyName());
        
        // 이전 계약에서 부서 정보 설정 (만약 있다면)
        if (previousContract.getDepartment() != null) {
            contractRequest.setDepartment(previousContract.getDepartment());
        }
        
        // 날짜 정보 설정
        contractRequest.setStartDate(startDate);
        contractRequest.setExpiryDate(expiryDate);
        contractRequest.setInsuranceStartDate(insuranceStartDate);
        contractRequest.setInsuranceEndDate(insuranceEndDate);
        
        // 이전 계약에서 템플릿 정보 가져오기
        if (previousContract.getTemplateMappings() != null && !previousContract.getTemplateMappings().isEmpty()) {
            List<Long> templateIds = previousContract.getTemplateMappings().stream()
                .map(mapping -> mapping.getTemplate().getId())
                .collect(Collectors.toList());
            contractRequest.setTemplateIds(templateIds);
            log.info("이전 계약의 템플릿 정보 사용: templateCount={}", templateIds.size());
        } else {
            // 템플릿 정보가 없는 경우 예외 발생
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "이전 계약에 템플릿 정보가 없습니다. 템플릿 정보를 직접 입력해 주세요.");
        }
        
        // 계약 구분 코드 설정 (재계약)
        contractRequest.setContractTypeCodeId("001001_0002"); // 재계약 코드
        log.info("재계약 코드 설정: companyId={}, 계약 구분 코드=001001_0002", companyId);
        
        // 이전 계약에서 참여자 정보 가져오기
        if (previousContract.getParticipants() != null && !previousContract.getParticipants().isEmpty()) {
            List<CreateParticipantRequest> participants = previousContract.getParticipants().stream()
                .map(participant -> {
                    CreateParticipantRequest participantRequest = new CreateParticipantRequest();
                    participantRequest.setName(participant.getName());
                    
                    // 암호화된 이메일과 전화번호 복호화
                    try {
                        String decryptedEmail = encryptionUtil.decrypt(participant.getEmail());
                        String decryptedPhone = encryptionUtil.decrypt(participant.getPhoneNumber());
                        participantRequest.setEmail(decryptedEmail);
                        participantRequest.setPhoneNumber(decryptedPhone);
                    } catch (Exception e) {
                        log.error("참여자 정복화 중 오류 발생: {}", e.getMessage(), e);
                        // 복호화에 실패한 경우 원본 값 사용
                        participantRequest.setEmail(participant.getEmail());
                        participantRequest.setPhoneNumber(participant.getPhoneNumber());
                    }
                    
                    participantRequest.setNotifyType(participant.getNotifyType());
                    
                    // 사용자 ID가 있는 경우 설정
                    if (participant.getUser() != null) {
                        participantRequest.setUserId(participant.getUser().getId());
                    }
                    
                    return participantRequest;
                })
                .collect(Collectors.toList());
            
            contractRequest.setParticipants(participants);
            log.info("이전 계약의 참여자 정보 사용: participantCount={}", participants.size());
        } else {
            // 참여자 정보가 없는 경우 예외 발생
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "이전 계약에 참여자 정보가 없습니다. 참여자 정보를 직접 입력해 주세요.");
        }
        
        // 작성자 정보 설정
        contractRequest.setCreatedBy(userId != null ? userId : "시스템");
        
        // userId로 사용자 조회하여 User ID 설정
        if (userId != null) {
            try {
                // 로그인 아이디(userId)로 User 조회
                User user = userRepository.findByUserId(userId)
                    .orElse(null);
                
                if (user != null) {
                    // User의 id 값을 설정 (contractRequest.userId)
                    contractRequest.setUserId(user.getId());
                    log.info("계약 작성자 User ID 설정: userId={}, userName={}, id={}", 
                        userId, user.getUserName(), user.getId());
                } else {
                    log.warn("userId로 사용자를 찾을 수 없습니다: {}", userId);
                }
            } catch (Exception e) {
                log.warn("사용자 정보 조회 중 오류 발생: {}", e.getMessage());
            }
        }
        
        // 이전 계약의 문서 코드 IDs 가져오기 (있는 경우)
        try {
            List<String> documentCodeIds = contractService.getContractDocumentCodeIds(previousContract.getId());
            if (!documentCodeIds.isEmpty()) {
                contractRequest.setDocumentCodeIds(documentCodeIds);
                log.info("이전 계약의 문서 코드 정보 사용: documentCodeCount={}", documentCodeIds.size());
            }
        } catch (Exception e) {
            log.warn("이전 계약의 문서 코드 정보를 가져오는 중 오류 발생: {}", e.getMessage());
            // 문서 코드는 필수가 아니므로 오류가 발생해도 계속 진행
        }
        
        // 추가: 수탁자 이력 ID 설정 (신규 수탁자 이력 ID)
        contractRequest.setTrusteeHistoryId(savedHistory.getId());
        log.info("새 계약에 수탁자 이력 ID 설정: trusteeHistoryId={}", savedHistory.getId());
        
        // 4. ContractService를 통해 새 계약 생성
        Contract newContract = contractService.createContract(contractRequest);
        log.info("재계약 계약 생성 완료: contractId={}, title={}", newContract.getId(), newContract.getTitle());
        
        // 5. 새 CompanyTrusteeHistory 레코드에 새 contract 연결 (기존 레코드는 변경하지 않음)
        // 새 수탁자 이력에만 계약 연결, 기존 수탁자 이력은 그대로 유지
        savedHistory.setContract(newContract);
        trusteeHistoryRepository.save(savedHistory);
        log.info("새 수탁자 이력과 계약 연결 완료: historyId={}, contractId={}", savedHistory.getId(), newContract.getId());
        
        // 결과 반환
        Map<String, Object> result = new HashMap<>();
        result.put("companyId", updatedCompany.getId());
        result.put("companyName", updatedCompany.getStoreName());
        result.put("historyId", savedHistory.getId());
        result.put("contractId", newContract.getId());
        result.put("contractNumber", newContract.getContractNumber());
        result.put("message", "재계약이 성공적으로 처리되었습니다.");
        
        // 계약 기본 정보 추가
        result.put("id", newContract.getId()); // 프론트엔드 호환성을 위해 id도 추가
        result.put("title", newContract.getTitle());
        result.put("createdBy", newContract.getCreatedBy());
        result.put("createdAt", newContract.getCreatedAt());
        result.put("startDate", newContract.getStartDate());
        result.put("expiryDate", newContract.getExpiryDate());
        
        // 참여자 정보 추가 (이메일/알림톡 발송을 위해 필수)
        if (newContract.getParticipants() != null && !newContract.getParticipants().isEmpty()) {
            List<Map<String, Object>> participantsList = newContract.getParticipants().stream()
                .map(participant -> {
                    Map<String, Object> participantMap = new HashMap<>();
                    participantMap.put("id", participant.getId());
                    participantMap.put("name", participant.getName());
                    
                    // 암호화된 이메일과 전화번호 복호화
                    try {
                        String decryptedEmail = encryptionUtil.decrypt(participant.getEmail());
                        String decryptedPhone = encryptionUtil.decrypt(participant.getPhoneNumber());
                        participantMap.put("email", decryptedEmail);
                        participantMap.put("phoneNumber", decryptedPhone);
                    } catch (Exception e) {
                        log.error("참여자 정복화 중 오류 발생: {}", e.getMessage(), e);
                        // 복호화에 실패한 경우 원본 값 사용
                        participantMap.put("email", participant.getEmail());
                        participantMap.put("phoneNumber", participant.getPhoneNumber());
                    }
                    
                    participantMap.put("notifyType", participant.getNotifyType().name());
                    
                    // 계약 정보 맵 생성
                    Map<String, Object> contractInfo = new HashMap<>();
                    contractInfo.put("title", newContract.getTitle());
                    participantMap.put("contract", contractInfo);
                    
                    return participantMap;
                })
                .collect(Collectors.toList());
            
            result.put("participants", participantsList);
            log.info("재계약 응답에 참여자 정보 포함: 참여자 수={}", participantsList.size());
        } else {
            log.warn("재계약 생성은 성공했으나 참여자 정보가 없습니다. contractId={}", newContract.getId());
            result.put("participants", List.of());
        }
        
        return result;
    }

    /**
     * 회사의 수탁자 이력 목록 조회 (계약 생성 시 선택용)
     * 활성 이력과 미래 이력(시작일이 미래인 비활성 이력) 모두 조회
     */
    public List<TrusteeHistoryDTO> getTrusteeHistoriesForContract(Long companyId) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회사를 찾을 수 없습니다. ID: " + companyId));
        
        // 회사의 모든 수탁자 이력 조회 (시작일 내림차순), 계약 및 템플릿 정보 포함
        List<CompanyTrusteeHistory> histories = trusteeHistoryRepository.findByCompanyWithContractAndTemplatesOrderByStartDateDesc(company);
        
        LocalDate today = LocalDate.now();
        
        // DTO 변환 (UI 표시용 정보 추가)
        return histories.stream()
            .map(history -> {
                TrusteeHistoryDTO dto = new TrusteeHistoryDTO();
                dto.setId(history.getId());
                dto.setTrustee(history.getTrustee());
                dto.setTrusteeCode(history.getTrusteeCode());
                dto.setRepresentativeName(history.getRepresentativeName());
                
                // 암호화된 필드 복호화
                try {
                    // 사업자번호 복호화
                    if (history.getBusinessNumber() != null && !history.getBusinessNumber().isEmpty()) {
                        dto.setBusinessNumber(encryptionUtil.decrypt(history.getBusinessNumber()));
                    } else {
                        dto.setBusinessNumber(history.getBusinessNumber());
                    }
                    
                    // 휴대폰번호 복호화
                    if (history.getPhoneNumber() != null && !history.getPhoneNumber().isEmpty()) {
                        dto.setPhoneNumber(encryptionUtil.decrypt(history.getPhoneNumber()));
                    } else {
                        dto.setPhoneNumber(history.getPhoneNumber());
                    }
                    
                    // 이메일 복호화
                    if (history.getEmail() != null && !history.getEmail().isEmpty()) {
                        dto.setEmail(encryptionUtil.decrypt(history.getEmail()));
                    } else {
                        dto.setEmail(history.getEmail());
                    }
                } catch (Exception e) {
                    log.error("수탁자 이력 데이터 복호화 중 오류 발생: historyId={}, error={}", history.getId(), e.getMessage(), e);
                    // 복호화 실패 시 원본 값 사용
                    dto.setBusinessNumber(history.getBusinessNumber());
                    dto.setPhoneNumber(history.getPhoneNumber());
                    dto.setEmail(history.getEmail());
                }
                
                dto.setStartDate(history.getStartDate());
                dto.setEndDate(history.getEndDate());
                dto.setInsuranceStartDate(history.getInsuranceStartDate());
                dto.setInsuranceEndDate(history.getInsuranceEndDate());
                dto.setActive(history.isActive());
                
                // 연결된 사용자 ID 설정
                if (history.getUser() != null) {
                    dto.setUserId(history.getUser().getId());
                }
                
                // 계약 및 템플릿 정보 설정
                if (history.getContract() != null) {
                    Contract contract = history.getContract();
                    dto.setContractId(contract.getId());
                    dto.setContractNumber(contract.getContractNumber());
                    
                    // 템플릿 정보 설정
                    if (contract.getTemplateMappings() != null && !contract.getTemplateMappings().isEmpty()) {
                        for (ContractTemplateMapping mapping : contract.getTemplateMappings()) {
                            if (mapping.getTemplate() != null) {
                                TrusteeHistoryDTO.TemplateInfo templateInfo = new TrusteeHistoryDTO.TemplateInfo(
                                    mapping.getTemplate().getId(),
                                    mapping.getTemplate().getTemplateName(),
                                    mapping.getProcessedPdfId(),
                                    mapping.getSortOrder()
                                );
                                dto.getTemplates().add(templateInfo);
                            }
                        }
                    }
                }
                
                // 활성 상태 표시 추가
                if (history.isActive()) {
                    // 오늘로부터 7일 이내에 종료되는 계약인지 확인
                    if (history.getEndDate() != null && 
                        !history.getEndDate().isBefore(today) && 
                        history.getEndDate().isBefore(today.plusDays(8))) {
                        dto.setStatusLabel("만료 예정 계약 (" + history.getEndDate() + " 종료)");
                        dto.setStatusType("expiring");
                    } else {
                        dto.setStatusLabel("현재 계약 중");
                        dto.setStatusType("active");
                    }
                } else {
                    // 시작일이 오늘인 경우 (오늘 시작 예정)
                    if (history.getStartDate() != null && history.getStartDate().isEqual(today)) {
                        dto.setStatusLabel("오늘 시작 예정");
                        dto.setStatusType("starting_today");
                    }
                    // 시작일이 미래인 경우 (예정된 계약)
                    else if (history.getStartDate() != null && history.getStartDate().isAfter(today)) {
                        dto.setStatusLabel("예정된 계약 (" + history.getStartDate() + " 시작)");
                        dto.setStatusType("pending");
                    } 
                    // 종료일이 오늘 이전인 경우 (종료된 계약)
                    else if (history.getEndDate() != null && history.getEndDate().isBefore(today)) {
                        dto.setStatusLabel("종료된 계약");
                        dto.setStatusType("expired");
                    }
                    // 그 외의 경우 (비활성 상태지만 날짜 조건이 명확하지 않은 경우)
                    else {
                        dto.setStatusLabel("비활성 계약");
                        dto.setStatusType("inactive");
                    }
                }
                
                return dto;
            })
            .collect(Collectors.toList());
    }
} 