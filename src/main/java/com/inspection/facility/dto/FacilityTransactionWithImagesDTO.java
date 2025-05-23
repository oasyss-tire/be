package com.inspection.facility.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacilityTransactionWithImagesDTO {
    private FacilityTransactionDTO transaction;
    private List<FacilityTransactionImageDTO> images;
} 