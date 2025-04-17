package com.inspection.service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.inspection.dto.CorrectionFieldDTO;
import com.inspection.dto.CorrectionRequestDTO;
import com.inspection.dto.CorrectionStatusDTO;
import com.inspection.entity.Code;
import com.inspection.entity.Contract;
import com.inspection.entity.ContractParticipant;
import com.inspection.entity.ParticipantPdfField;
import com.inspection.entity.ParticipantTemplateMapping;
import com.inspection.repository.CodeRepository;
import com.inspection.repository.ContractParticipantRepository;
import com.inspection.repository.ContractRepository;
import com.inspection.repository.ParticipantPdfFieldRepository;
import com.inspection.util.EncryptionUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CorrectionRequestService {

    private final ParticipantPdfFieldRepository participantPdfFieldRepository;
    private final ContractParticipantRepository participantRepository;
    private final ContractRepository contractRepository;
    private final EmailService emailService;
    private final EncryptionUtil encryptionUtil;
    private final ParticipantTokenService participantTokenService;
    private final CodeRepository codeRepository;
    private final PdfProcessingService pdfProcessingService;
    private final PdfService pdfService;
    
    @Value("${frontend.base-url}")
    private String frontendBaseUrl;
    
    @Value("${file.upload.path}")
    private String uploadPath;
    
    // 참여자 상태 코드 상수
    private static final String PARTICIPANT_STATUS_RESIGN_IN_PROGRESS = "008001_0007"; // 재서명 진행중
    private static final String PARTICIPANT_STATUS_APPROVAL_WAITING = "008001_0001";   // 승인 대기

    /**
     * 재서명 요청 생성
     */
    @Transactional
    public CorrectionStatusDTO createCorrectionRequest(Long participantId, CorrectionRequestDTO request) {
        log.info("재서명 요청 생성 - 참여자 ID: {}, 필드 수: {}", participantId, request.getFieldIds().size());
        
        // 참여자 조회
        ContractParticipant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new RuntimeException("참여자 정보를 찾을 수 없습니다: " + participantId));
        
        // 참여자 상태를 재서명 진행중으로 변경
        try {
            Code resignInProgressStatus = codeRepository.findById(PARTICIPANT_STATUS_RESIGN_IN_PROGRESS)
                .orElseThrow(() -> new RuntimeException("재서명 진행중 상태 코드를 찾을 수 없습니다"));
            participant.setStatusCode(resignInProgressStatus);
            participantRepository.save(participant);
            log.info("참여자 상태를 재서명 진행중으로 변경 - 참여자 ID: {}", participantId);
        } catch (Exception e) {
            log.error("참여자 상태 변경 중 오류 발생: {}", e.getMessage(), e);
            // 상태 변경 실패는 프로세스를 중단하지 않음
        }
        
        // 계약 조회
        Contract contract = participant.getContract();
        
        // 필드별 수정 사유 맵 생성
        Map<Long, String> fieldCommentMap = request.getFieldCorrections() != null ? 
                request.getFieldCorrections().stream()
                        .collect(Collectors.toMap(CorrectionRequestDTO.FieldCorrectionDTO::getFieldId, 
                                                 CorrectionRequestDTO.FieldCorrectionDTO::getComment)) : 
                Map.of();
        
        // 요청된 필드 ID로 필드 목록 조회
        List<ParticipantPdfField> fields = participantPdfFieldRepository.findAllById(request.getFieldIds());
        
        if (fields.isEmpty()) {
            throw new RuntimeException("재서명 요청할 필드가 없습니다.");
        }
        
        LocalDateTime now = LocalDateTime.now();
        
        // 필드별 재서명 요청 상태 설정
        fields.forEach(field -> {
            field.setNeedsCorrection(true);
            field.setCorrectionRequestedAt(now);
            
            // 필드별 수정 사유 설정
            String comment = fieldCommentMap.containsKey(field.getId()) ? 
                    fieldCommentMap.get(field.getId()) : request.getCorrectionComment();
            field.setCorrectionComment(comment);
        });
        
        // 변경사항 저장
        List<ParticipantPdfField> updatedFields = participantPdfFieldRepository.saveAll(fields);
        log.info("재서명 요청 필드 업데이트 완료 - 필드 수: {}", updatedFields.size());
        
        // ParticipantTemplateMapping 업데이트
        // 필드들이 속한 모든 PDF에 대한 템플릿 매핑을 찾아 재서명 필요 상태로 설정
        Set<String> pdfIds = fields.stream()
                .map(ParticipantPdfField::getPdfId)
                .collect(Collectors.toSet());
        
        List<ParticipantTemplateMapping> mappings = participant.getTemplateMappings();
        for (ParticipantTemplateMapping mapping : mappings) {
            if (pdfIds.contains(mapping.getPdfId())) {
                mapping.setNeedsResign(true);
                mapping.setResignRequestedAt(now);
                log.info("템플릿 매핑 재서명 상태 업데이트 - PDF ID: {}", mapping.getPdfId());
            }
        }
        
        // 이메일 발송
        if (request.isSendEmail() && participant.getEmail() != null && !participant.getEmail().isEmpty()) {
            try {
                sendCorrectionRequestEmail(participant, fields);
                log.info("재서명 요청 이메일 발송 완료 - 참여자: {}", participant.getName());
            } catch (Exception e) {
                log.error("재서명 요청 이메일 발송 실패: {}", e.getMessage(), e);
            }
        }
        
        // 응답 생성
        return createCorrectionStatusDTO(participant, fields);
    }
    
    /**
     * 특정 참여자의 재서명 상태 조회
     */
    @Transactional(readOnly = true)
    public CorrectionStatusDTO getCorrectionStatus(Long participantId) {
        // 참여자 조회
        ContractParticipant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new RuntimeException("참여자 정보를 찾을 수 없습니다: " + participantId));
        
        // 재서명 필요한 필드 조회
        List<ParticipantPdfField> fields = participantPdfFieldRepository.findByNeedsCorrectionTrueAndParticipantId(participantId);
        
        return createCorrectionStatusDTO(participant, fields);
    }
    
    /**
     * 특정 참여자의 재서명 필요한 필드 목록 조회
     */
    @Transactional(readOnly = true)
    public List<CorrectionFieldDTO> getCorrectionFields(Long participantId) {
        List<ParticipantPdfField> fields = participantPdfFieldRepository.findByNeedsCorrectionTrueAndParticipantId(participantId);
        return fields.stream()
                .map(CorrectionFieldDTO::new)
                .collect(Collectors.toList());
    }
    
    /**
     * 재서명 요청 상태 DTO 생성
     */
    private CorrectionStatusDTO createCorrectionStatusDTO(ContractParticipant participant, List<ParticipantPdfField> fields) {
        // 모든 필드 조회하여 진행 상태 계산
        List<ParticipantPdfField> allFields = participantPdfFieldRepository.findByParticipantId(participant.getId());
        
        // 수정이 필요한 필드 수
        int needsCorrectionCount = (int) allFields.stream()
                .filter(ParticipantPdfField::getNeedsCorrection)
                .count();
        
        // 총 필드 수
        int totalFieldsCount = allFields.size();
        
        // 완료된 필드 수
        int completedCount = totalFieldsCount - needsCorrectionCount;
        
        // 상태 결정
        String status = "COMPLETED";
        if (needsCorrectionCount > 0) {
            status = completedCount > 0 ? "IN_PROGRESS" : "REQUESTED";
        }
        
        // 이메일 복호화
        String email = "";
        if (participant.getEmail() != null && !participant.getEmail().isEmpty()) {
            try {
                email = encryptionUtil.decrypt(participant.getEmail());
            } catch (Exception e) {
                log.error("이메일 복호화 실패: {}", e.getMessage());
            }
        }
        
        return CorrectionStatusDTO.builder()
                .participantId(participant.getId())
                .participantName(participant.getName())
                .participantEmail(email)
                .contractId(participant.getContract().getId())
                .contractTitle(participant.getContract().getTitle())
                .correctionRequestedAt(fields.isEmpty() ? null : 
                        fields.stream()
                             .map(ParticipantPdfField::getCorrectionRequestedAt)
                             .max(LocalDateTime::compareTo)
                             .orElse(null))
                .totalFields(totalFieldsCount)
                .completedFields(completedCount)
                .status(status)
                .fields(fields.stream()
                        .map(CorrectionFieldDTO::new)
                        .collect(Collectors.toList()))
                .build();
    }
    
    /**
     * 재서명 요청 이메일 전송
     */
    private void sendCorrectionRequestEmail(ContractParticipant participant, List<ParticipantPdfField> fields) throws Exception {
        // 이메일 복호화
        String decryptedEmail = encryptionUtil.decrypt(participant.getEmail());
        
        // 토큰 생성
        String token = participantTokenService.generateParticipantToken(participant.getId());
        
        // 재서명 페이지 URL 생성
        String correctionUrl = frontendBaseUrl + "/correction-request?token=" + token;
        
        // 이메일 전송
        emailService.sendCorrectionRequestEmail(
                decryptedEmail,
                participant.getName(),
                participant.getContract().getTitle(),
                fields.size(),
                correctionUrl
        );
    }

    /**
     * 재서명 필드 값 업데이트
     */
    @Transactional
    public CorrectionFieldDTO updateCorrectionField(Long participantId, Long fieldId, CorrectionFieldDTO fieldDTO) {
        log.info("재서명 필드 값 업데이트 - 참여자 ID: {}, 필드 ID: {}", participantId, fieldId);
        
        // 1. 필드 조회
        ParticipantPdfField field = participantPdfFieldRepository.findById(fieldId)
                .orElseThrow(() -> new RuntimeException("필드 정보를 찾을 수 없습니다: " + fieldId));
        
        // 2. 필드가 해당 참여자의 것인지 확인
        if (!field.getParticipant().getId().equals(participantId)) {
            throw new RuntimeException("해당 필드는 요청한 참여자의 것이 아닙니다.");
        }
        
        // 3. 필드가 재서명이 필요한 상태인지 확인
        if (!field.getNeedsCorrection()) {
            throw new RuntimeException("해당 필드는 재서명이 필요하지 않습니다.");
        }
        
        // 4. 필드 값 업데이트
        field.setValue(fieldDTO.getValue());
        field.setNeedsCorrection(false); // 재서명 완료 상태로 변경
        
        // 5. 변경사항 저장
        ParticipantPdfField updatedField = participantPdfFieldRepository.save(field);
        log.info("필드 값 업데이트 완료 - 필드 ID: {}", updatedField.getId());
        
        // 6. 모든 필드의 재서명이 완료되었는지 확인
        List<ParticipantPdfField> remainingFields = 
                participantPdfFieldRepository.findByNeedsCorrectionTrueAndParticipantId(participantId);
        
        // 7. 남은 필드가 없으면 완료 처리
        if (remainingFields.isEmpty()) {
            log.info("모든 필드 재서명 완료 - 참여자 ID: {}", participantId);
        } else {
            log.info("남은 재서명 필드 수: {}", remainingFields.size());
        }
        
        // 8. 응답 생성 및 반환
        return new CorrectionFieldDTO(updatedField);
    }
    
    /**
     * 참여자의 모든 재서명 완료 처리
     */
    @Transactional
    public CorrectionStatusDTO completeCorrections(Long participantId) {
        log.info("재서명 완료 처리 시작 - 참여자 ID: {}", participantId);
        
        // 1. 참여자 조회
        ContractParticipant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new RuntimeException("참여자 정보를 찾을 수 없습니다: " + participantId));
        
        // 2. 남은 재서명 필드 조회
        List<ParticipantPdfField> remainingFields = 
                participantPdfFieldRepository.findByNeedsCorrectionTrueAndParticipantId(participantId);
        
        // 3. 모든 필드 재서명 완료 처리
        if (!remainingFields.isEmpty()) {
            // 필드의 PDF ID 로깅
            Map<String, Integer> pdfFieldCounts = new HashMap<>();
            for (ParticipantPdfField field : remainingFields) {
                String pdfId = field.getPdfId();
                pdfFieldCounts.put(pdfId, pdfFieldCounts.getOrDefault(pdfId, 0) + 1);
            }
            pdfFieldCounts.forEach((pdfId, count) -> {
                log.info("PDF ID: {} - 필드 수: {}", pdfId, count);
            });
            
            remainingFields.forEach(field -> field.setNeedsCorrection(false));
            participantPdfFieldRepository.saveAll(remainingFields);
            log.info("남은 모든 필드 재서명 완료 처리 - 필드 수: {}", remainingFields.size());
        }
        
        // 4. 참여자의 재서명 상태 업데이트 및 재서명 PDF 생성
        LocalDateTime now = LocalDateTime.now();
        List<ParticipantTemplateMapping> mappings = participant.getTemplateMappings();
        
        for (ParticipantTemplateMapping mapping : mappings) {
            if (mapping.isNeedsResign()) {
                try {
                    // 원본 PDF ID
                    String pdfId = mapping.getPdfId();
                    
                    // 원본 PDF 파일 경로
                    Path originalPath = Paths.get(uploadPath, "participants", pdfId);
                    if (!Files.exists(originalPath)) {
                        throw new RuntimeException("원본 PDF를 찾을 수 없습니다: " + pdfId);
                    }
                    
                    // 현재 시간의 타임스탬프 생성
                    String timestamp = now.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                    
                    // 로그를 통해 원본 파일명 확인
                    log.info("원본 PDF 파일명 구조 분석: {}", pdfId);
                    
                    // 타임스탬프 패턴 정규식 (yyyyMMdd_HHmmss 형식)
                    String timestampPattern = "\\d{8}_\\d{6}";
                    
                    // 재서명 PDF ID 생성
                    String resignedPdfId;
                    
                    if (pdfId.matches(timestampPattern + ".*")) {
                        // 타임스탬프 부분 교체 - 파일명의 첫 번째 부분만 교체
                        int firstUnderscore = pdfId.indexOf("_", 9); // yyyyMMdd_ 이후의 첫 번째 언더스코어
                        if (firstUnderscore > 0) {
                            String afterTimestamp = pdfId.substring(firstUnderscore);
                            
                            // SIGNING을 RESIGNED로 교체
                            String afterSigningReplaced = afterTimestamp.replace("_SIGNING_", "_RESIGNED_");
                            
                            // 최종 파일명
                            resignedPdfId = timestamp + afterSigningReplaced;
                            
                            log.info("파일명 타임스탬프 교체 및 SIGNING->RESIGNED 교체 완료: {}", resignedPdfId);
                        } else {
                            // 예상치 못한 형식
                            resignedPdfId = timestamp + "_RESIGNED_" + pdfId.substring(15); // 타임스탬프 부분 제거
                            log.info("타임스탬프만 교체: {}", resignedPdfId);
                        }
                    } else {
                        // 타임스탬프 패턴이 없는 경우 간단하게 접두사 추가
                        resignedPdfId = timestamp + "_RESIGNED_" + pdfId;
                        log.info("타임스탬프와 접두사 추가: {}", resignedPdfId);
                    }
                    
                    log.info("재서명 PDF 파일명 생성 결과: 원본={}, 재서명={}", pdfId, resignedPdfId);
                    
                    // 원본 PDF 바이트 읽기
                    byte[] originalPdf = Files.readAllBytes(originalPath);
                    
                    // 해당 참여자의 특정 PDF에 해당하는 필드만 조회
                    List<ParticipantPdfField> fields = participantPdfFieldRepository.findByParticipantIdAndPdfId(participantId, pdfId);
                    log.info("해당 PDF의 필드 수: {} - PDF ID: {}", fields.size(), pdfId);
                    
                    // 필드가 있는 경우에만 PDF 처리
                    if (!fields.isEmpty()) {
                        // PDF에 필드 값 추가
                        byte[] processedPdf = pdfProcessingService.addValuesToFields(originalPdf, fields);
                        
                        // 서명 시간 정보
                        String timeInfo = "재서명 완료 시간: " + now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                        
                        // 시리얼 넘버 생성
                        String contractInfo = pdfId + "_" + participantId + "_" + now.toString();
                        String serialNumber = generateSerialNumber(contractInfo);
                        String serialInfo = "시리얼 넘버: " + serialNumber;
                        
                        // PDF 하단에 서명 시간과 시리얼 넘버 추가
                        processedPdf = pdfProcessingService.addSignatureTimeToPdf(
                            processedPdf, 
                            timeInfo + "    " + serialInfo
                        );
                        
                        // 원본 서명 PDF와 동일한 비밀번호 적용
                        // 저장된 암호화된 비밀번호 가져오기
                        String encryptedPassword = mapping.getDocumentPassword();
                        String password = null;
                        
                        try {
                            if (encryptedPassword != null && !encryptedPassword.isEmpty()) {
                                // 비밀번호 복호화
                                password = encryptionUtil.decrypt(encryptedPassword);
                                log.info("원본 PDF 비밀번호 재사용 - PDF ID: {}", pdfId);
                                
                                // PdfService를 사용하여 암호화
                                processedPdf = pdfService.encryptPdf(processedPdf, password);
                            } else {
                                log.warn("원본 PDF의 비밀번호를 찾을 수 없습니다. 암호화 없이 진행합니다.");
                            }
                        } catch (Exception e) {
                            log.error("PDF 비밀번호 적용 중 오류 발생: {}", e.getMessage(), e);
                            // 비밀번호 오류는 프로세스를 중단시키지 않음
                        }
                        
                        // 재서명 PDF 폴더 확인 및 생성
                        Path resignedDir = Paths.get(uploadPath, "resigned");
                        if (!Files.exists(resignedDir)) {
                            Files.createDirectories(resignedDir);
                        }
                        
                        // 재서명 PDF 저장
                        Path resignedPath = resignedDir.resolve(resignedPdfId);
                        Files.write(resignedPath, processedPdf);
                        
                        // 참여자 템플릿 매핑 정보 업데이트
                        mapping.setNeedsResign(false);
                        mapping.setResignedPdfId(resignedPdfId);
                        mapping.setResignedAt(now);
                        
                        log.info("재서명 PDF 생성 완료 - 참여자 ID: {}, PDF ID: {}", participantId, resignedPdfId);
                        
                        // 비밀번호가 있는 경우 이메일로 발송
                        try {
                            if (password != null) {
                                // 참여자 이메일 정보가 있는 경우에만 발송
                                if (participant.getEmail() != null && !participant.getEmail().trim().isEmpty()) {
                                    String decryptedEmail = encryptionUtil.decrypt(participant.getEmail());
                                    String contractTitle = participant.getContract().getTitle();
                                    
                                    // 재서명 완료 이메일 발송 (비밀번호 포함)
                                    emailService.sendPdfPasswordEmail(
                                        decryptedEmail,
                                        participant.getName(),
                                        password,
                                        resignedPdfId
                                    );
                                    
                                    log.info("재서명 PDF 비밀번호 이메일 발송 성공 - 참여자: {}, 이메일: {}", 
                                        participant.getName(), decryptedEmail.replaceAll("(?<=.{3}).(?=.*@)", "*"));
                                }
                            } else {
                                log.info("비밀번호가 없어 이메일 발송을 건너뜁니다.");
                            }
                        } catch (Exception e) {
                            log.error("재서명 PDF 비밀번호 이메일 발송 실패: {}", e.getMessage(), e);
                            // 이메일 발송 오류는 프로세스를 중단하지 않음
                        }
                    } else {
                        log.warn("해당 PDF에 재서명할 필드가 없습니다 - PDF ID: {}", pdfId);
                        // 필드가 없어도 재서명 처리는 완료
                        mapping.setNeedsResign(false);
                    }
                } catch (Exception e) {
                    log.error("재서명 PDF 생성 중 오류 발생: {}", e.getMessage(), e);
                    // PDF 생성 실패는 프로세스를 중단하지 않음
                }
            }
        }
        
        // 5. 참여자 상태를 승인 대기 상태로 변경
        try {
            Code waitingApprovalStatus = codeRepository.findById(PARTICIPANT_STATUS_APPROVAL_WAITING)
                .orElseThrow(() -> new RuntimeException("승인 대기 상태 코드를 찾을 수 없습니다"));
            participant.setStatusCode(waitingApprovalStatus);
            participantRepository.save(participant);
            log.info("참여자 상태를 승인 대기로 변경 - 참여자 ID: {}", participantId);
        } catch (Exception e) {
            log.error("참여자 상태 변경 중 오류 발생: {}", e.getMessage(), e);
            // 상태 변경 실패는 프로세스를 중단하지 않음
        }
        
        // 6. 상태 정보 반환
        return createCorrectionStatusDTO(participant, remainingFields);
    }
    
    /**
     * 계약 정보를 기반으로 시리얼 넘버를 생성합니다.
     */
    private String generateSerialNumber(String contractInfo) {
        try {
            // UUID 생성
            String uuid = java.util.UUID.randomUUID().toString().replace("-", "");
            
            // 계약 정보의 SHA-256 해시 생성
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(contractInfo.getBytes(StandardCharsets.UTF_8));
            
            // 해시를 16진수 문자열로 변환
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            
            // UUID와 해시 일부를 조합하여 시리얼 넘버 생성
            String serialNumber = uuid + hexString.toString().substring(0, 18);
            
            // 시리얼 넘버에 하이픈 추가 (가독성 향상)
            StringBuilder formattedSerial = new StringBuilder();
            for (int i = 0; i < serialNumber.length(); i++) {
                if (i > 0 && i % 5 == 0) {
                    formattedSerial.append('-');
                }
                formattedSerial.append(serialNumber.charAt(i));
            }
            
            return formattedSerial.toString();
        } catch (Exception e) {
            log.error("시리얼 넘버 생성 중 오류 발생", e);
            // 오류 발생 시 대체 시리얼 넘버 반환
            return "ERR-" + System.currentTimeMillis() + "-" + contractInfo.hashCode();
        }
    }

    /**
     * PDF ID에서 원본 파일명을 추출합니다.
     */
    private String extractOriginalFileName(String pdfId) {
        // 새 형식 (타임스탬프_SIGNING_계약번호_원본파일명_참여자이름.pdf)에서 추출
        // 앞에서부터 세 번째 언더스코어 이후부터 네 번째 언더스코어 이전까지가 원본 파일명
        
        String[] parts = pdfId.split("_");
        if (parts.length >= 5) { // 최소 5개 이상의 부분으로 나뉘어야 함
            // 원본 파일명 부분 (인덱스 3부터 마지막-1까지)
            StringBuilder fileName = new StringBuilder();
            for (int i = 3; i < parts.length - 1; i++) {
                fileName.append(parts[i]);
                if (i < parts.length - 2) {
                    fileName.append("_"); // 중간에 있던 언더스코어 복원
                }
            }
            return fileName.toString();
        }
        
        // 기존 형식 또는 파싱할 수 없는 경우 기본값 반환
        return "document";
    }
} 