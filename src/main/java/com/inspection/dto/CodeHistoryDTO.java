package com.inspection.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.inspection.entity.CodeHistory;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter @Setter
@Slf4j
public class CodeHistoryDTO {
    private Long id;                 // 이력 ID
    private String codeId;           // 코드 ID
    private String codeName;         // 코드 이름
    private String groupId;          // 소속 그룹 ID
    private String actionType;       // 작업 유형 (CREATE, UPDATE, DELETE)
    private String changedBy;        // 변경자
    private LocalDateTime changedAt; // 변경일
    private Map<String, Object> changeDetails;  // 변경 상세 내용
    
    /**
     * CodeHistory 엔티티를 DTO로 변환
     */
    public static CodeHistoryDTO fromEntity(CodeHistory history) {
        if (history == null) return null;
        
        CodeHistoryDTO dto = new CodeHistoryDTO();
        dto.setId(history.getId());
        dto.setCodeId(history.getCodeId());
        dto.setCodeName(history.getCodeName());
        dto.setGroupId(history.getGroupId());
        dto.setActionType(history.getActionType().name());
        dto.setChangedBy(history.getChangedBy());
        dto.setChangedAt(history.getChangedAt());
        
        // 변경 상세 내용 처리 (JSON 문자열을 Map으로 변환)
        if (history.getChangeDetails() != null) {
            try {
                Map<String, Object> changeDetails = new HashMap<>();
                changeDetails.put("details", history.getChangeDetails());
                dto.setChangeDetails(changeDetails);
            } catch (Exception e) {
                log.error("이력 변경 내용 변환 중 오류 발생", e);
            }
        }
        
        return dto;
    }
    
    /**
     * CodeHistory 엔티티 목록을 DTO 목록으로 변환
     */
    public static List<CodeHistoryDTO> fromEntities(List<CodeHistory> histories) {
        if (histories == null) return null;
        
        List<CodeHistoryDTO> dtos = new ArrayList<>();
        for (CodeHistory history : histories) {
            dtos.add(fromEntity(history));
        }
        return dtos;
    }
} 