package com.inspection.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class SaveContractPdfFieldsRequest {
    private String pdfId;       // PDF 문서 식별자
    private List<ContractPdfFieldDTO> fields;
} 