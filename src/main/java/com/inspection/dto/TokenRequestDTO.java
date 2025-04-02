package com.inspection.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 토큰 생성 요청을 위한 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TokenRequestDTO {
    private Long participantId;
} 