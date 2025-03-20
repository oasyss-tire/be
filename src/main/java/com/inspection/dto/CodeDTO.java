package com.inspection.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.inspection.entity.Code;
import com.inspection.entity.CodeGroup;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CodeDTO {
    private String codeId;           // 코드 ID
    private String codeName;         // 코드 이름
    private String groupId;          // 소속 그룹 ID
    private Integer sortOrder;       // 정렬 순서
    private boolean active;          // 활성화 여부
    private String description;      // 설명
    private List<CodeAttributeDTO> attributes;  // 코드 속성 목록
    private String createdBy;        // 등록자
    private LocalDateTime createdAt; // 등록일
    private String updatedBy;        // 수정자
    private LocalDateTime updatedAt; // 수정일
    
    /**
     * Code 엔티티를 DTO로 변환
     */
    public static CodeDTO fromEntity(Code code) {
        if (code == null) return null;
        
        CodeDTO dto = new CodeDTO();
        dto.setCodeId(code.getCodeId());
        dto.setCodeName(code.getCodeName());
        
        if (code.getCodeGroup() != null) {
            dto.setGroupId(code.getCodeGroup().getGroupId());
        }
        
        dto.setSortOrder(code.getSortOrder());
        dto.setActive(code.isActive());
        dto.setDescription(code.getDescription());
        dto.setCreatedBy(code.getCreatedBy());
        dto.setCreatedAt(code.getCreatedAt());
        dto.setUpdatedBy(code.getUpdatedBy());
        dto.setUpdatedAt(code.getUpdatedAt());
        
        return dto;
    }
    
    /**
     * Code 엔티티 목록을 DTO 목록으로 변환
     */
    public static List<CodeDTO> fromEntities(List<Code> codes) {
        if (codes == null) return null;
        
        List<CodeDTO> dtos = new ArrayList<>();
        for (Code code : codes) {
            dtos.add(fromEntity(code));
        }
        return dtos;
    }
    
    /**
     * DTO를 Code 엔티티로 변환
     */
    public Code toEntity() {
        Code code = new Code();
        code.setCodeId(this.codeId);
        code.setCodeName(this.codeName);
        
        if (this.groupId != null) {
            CodeGroup codeGroup = new CodeGroup();
            codeGroup.setGroupId(this.groupId);
            code.setCodeGroup(codeGroup);
        }
        
        code.setSortOrder(this.sortOrder);
        code.setActive(this.active);
        code.setDescription(this.description);
        
        return code;
    }
} 