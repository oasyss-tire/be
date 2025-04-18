package com.inspection.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.inspection.dto.ContractPdfFieldDTO;
import com.inspection.dto.ParticipantPdfFieldDTO;
import com.inspection.dto.SaveContractPdfFieldsRequest;
import com.inspection.entity.Code;
import com.inspection.entity.ContractPdfField;
import com.inspection.entity.ParticipantPdfField;
import com.inspection.repository.CodeRepository;
import com.inspection.repository.ContractPdfFieldRepository;
import com.inspection.repository.ParticipantPdfFieldRepository;
import com.inspection.util.EncryptionUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ContractPdfService {
    private final ContractPdfFieldRepository contractPdfFieldRepository;
    private final ParticipantPdfFieldRepository participantPdfFieldRepository;
    private final CodeRepository codeRepository;
    private final EncryptionUtil encryptionUtil;

    public void saveFields(SaveContractPdfFieldsRequest request) {
        log.info("Deleting existing fields for PDF: {}", request.getPdfId());
        contractPdfFieldRepository.deleteByPdfId(request.getPdfId());

        List<ContractPdfField> fields = request.getFields().stream()
            .map(dto -> {
                ContractPdfField field = new ContractPdfField();
                field.setPdfId(request.getPdfId());
                field.setFieldId(dto.getId());
                field.setFieldName(dto.getFieldName());
                field.setType(dto.getType());
                field.setRelativeX(dto.getRelativeX());
                field.setRelativeY(dto.getRelativeY());
                field.setRelativeWidth(dto.getRelativeWidth());
                field.setRelativeHeight(dto.getRelativeHeight());
                field.setPage(dto.getPage());
                field.setValue(dto.getValue());
                field.setConfirmText(dto.getConfirmText());
                field.setDescription(dto.getDescription());
                
                // 형식 코드 설정 (있는 경우에만)
                if (dto.getFormatCodeId() != null && !dto.getFormatCodeId().isEmpty()) {
                    try {
                        Code formatCode = codeRepository.findById(dto.getFormatCodeId())
                            .orElse(null);
                        field.setFormat(formatCode);
                    } catch (Exception e) {
                        log.warn("형식 코드를 찾을 수 없습니다: {}", dto.getFormatCodeId());
                    }
                }
                
                log.debug("Created field entity: {}", field);
                return field;
            })
            .collect(Collectors.toList());

        log.info("Saving {} new fields", fields.size());
        List<ContractPdfField> savedFields = contractPdfFieldRepository.saveAll(fields);
        log.info("Successfully saved {} fields", savedFields.size());
    }

    public List<ContractPdfFieldDTO> getFieldsByPdfId(String pdfId) {
        return contractPdfFieldRepository.findByPdfId(pdfId).stream()
            .map(field -> {
                // 민감 정보 필드인 경우 복호화된 DTO 반환
                if (field.getFormat() != null) {
                    String formatCode = field.getFormat().getCodeId();
                    if ("001004_0001".equals(formatCode) || "001004_0002".equals(formatCode)) {
                        return new ContractPdfFieldDTO(field, encryptionUtil);
                    }
                }
                // 일반 필드는 그대로 반환
                return new ContractPdfFieldDTO(field);
            })
            .collect(Collectors.toList());
    }

    public List<ParticipantPdfFieldDTO> getParticipantFieldsByPdfId(String pdfId) {
        return participantPdfFieldRepository.findByPdfId(pdfId).stream()
            .map(field -> {
                // 민감 정보 필드인 경우 복호화된 DTO 반환
                if (field.getFormat() != null) {
                    String formatCode = field.getFormat().getCodeId();
                    if ("001004_0001".equals(formatCode) || "001004_0002".equals(formatCode)) {
                        return new ParticipantPdfFieldDTO(field, encryptionUtil);
                    }
                }
                // 일반 필드는 그대로 반환
                return new ParticipantPdfFieldDTO(field);
            })
            .collect(Collectors.toList());
    }
    
    public Optional<ParticipantPdfField> getParticipantFieldByPdfIdAndFieldName(String pdfId, String fieldName) {
        return participantPdfFieldRepository.findByPdfIdAndFieldName(pdfId, fieldName);
    }
} 