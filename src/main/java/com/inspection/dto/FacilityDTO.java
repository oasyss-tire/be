package com.inspection.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import com.inspection.entity.FacilityStatus;
import lombok.Getter;
import lombok.Setter;



@Getter @Setter
public class FacilityDTO {
    private Long id;
    private Long companyId;
    private String name;
    private String code;
    private String location;
    private BigDecimal acquisitionCost;
    private LocalDate acquisitionDate;
    private FacilityStatus status;
    private String currentLocation;
    private String description;
    private String thumbnailUrl;
} 