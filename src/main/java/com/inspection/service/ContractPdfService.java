package com.inspection.service;

import com.inspection.dto.ContractPdfFieldDTO;
import com.inspection.dto.SaveContractPdfFieldsRequest;
import com.inspection.entity.ContractPdfField;
import com.inspection.repository.ContractPdfFieldRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import com.inspection.entity.ParticipantPdfField;
import com.inspection.repository.ParticipantPdfFieldRepository;
import com.inspection.dto.ParticipantPdfFieldDTO;
import java.util.Optional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ContractPdfService {
    private final ContractPdfFieldRepository contractPdfFieldRepository;
    private final ParticipantPdfFieldRepository participantPdfFieldRepository;

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
            .map(ContractPdfFieldDTO::new)
            .collect(Collectors.toList());
    }

    public List<ParticipantPdfFieldDTO> getParticipantFieldsByPdfId(String pdfId) {
        return participantPdfFieldRepository.findByPdfId(pdfId).stream()
            .map(ParticipantPdfFieldDTO::new)
            .collect(Collectors.toList());
    }
    
    public Optional<ParticipantPdfField> getParticipantFieldByPdfIdAndFieldName(String pdfId, String fieldName) {
        return participantPdfFieldRepository.findByPdfIdAndFieldName(pdfId, fieldName);
    }
} 