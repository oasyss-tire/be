package com.inspection.dto;

import lombok.Getter;
import lombok.Setter;
import com.inspection.entity.FacilityStatus;
import java.time.LocalDateTime;

@Getter @Setter
public class FacilityStatusHistoryDTO {
    private Long id;
    private FacilityStatus status;
    private String currentLocation;
    private LocalDateTime statusChangeDate;
} 