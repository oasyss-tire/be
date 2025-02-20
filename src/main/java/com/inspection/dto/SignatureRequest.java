package com.inspection.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class SignatureRequest {
    private int pageNumber;
    private String signatureData;  // Base64 encoded canvas image
    private String signType;       // CONTRACTOR/CONTRACTEE
} 