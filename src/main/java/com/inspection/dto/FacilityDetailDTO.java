package com.inspection.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import com.inspection.entity.FacilityStatus;

@Getter @Setter
public class FacilityDetailDTO extends FacilityDTO {
    private String companyName;
    private LocalDate statusChangeDate;
    private List<FacilityContractDTO> contracts;
    private List<FacilityImageDTO> images;
} 