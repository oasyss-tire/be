package com.inspection.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class FacilityContractDTO {
    private Long id;
    private Long facilityId;
    private String contractNumber;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal contractAmount;
    private Boolean isPaid;
    private LocalDate paidDate;
    private String vendorName;
    private String vendorContact;
    private String description;
} 