package com.inspection.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.inspection.entity.Code;
import com.inspection.entity.CodeAttribute;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CodeAttributeDTO {
    private Long id;                 // 속성 ID
    private String codeId;           // 코드 ID
    private String attributeKey;     // 속성 키
    private String attributeValue;   // 속성 값
    private String createdBy;        // 등록자
    private LocalDateTime createdAt; // 등록일
    private String updatedBy;        // 수정자
    private LocalDateTime updatedAt; // 수정일
    
    /**
     * CodeAttribute 엔티티를 DTO로 변환
     */
    public static CodeAttributeDTO fromEntity(CodeAttribute attribute) {
        if (attribute == null) return null;
        
        CodeAttributeDTO dto = new CodeAttributeDTO();
        dto.setId(attribute.getId());
        
        if (attribute.getCode() != null) {
            dto.setCodeId(attribute.getCode().getCodeId());
        }
        
        dto.setAttributeKey(attribute.getAttributeKey());
        dto.setAttributeValue(attribute.getAttributeValue());
        dto.setCreatedBy(attribute.getCreatedBy());
        dto.setCreatedAt(attribute.getCreatedAt());
        dto.setUpdatedBy(attribute.getUpdatedBy());
        dto.setUpdatedAt(attribute.getUpdatedAt());
        
        return dto;
    }
    
    /**
     * CodeAttribute 엔티티 목록을 DTO 목록으로 변환
     */
    public static List<CodeAttributeDTO> fromEntities(List<CodeAttribute> attributes) {
        if (attributes == null) return null;
        
        List<CodeAttributeDTO> dtos = new ArrayList<>();
        for (CodeAttribute attribute : attributes) {
            dtos.add(fromEntity(attribute));
        }
        return dtos;
    }
    
    /**
     * DTO를 CodeAttribute 엔티티로 변환
     */
    public CodeAttribute toEntity() {
        CodeAttribute attribute = new CodeAttribute();
        
        if (this.id != null) {
            attribute.setId(this.id);
        }
        
        if (this.codeId != null) {
            Code code = new Code();
            code.setCodeId(this.codeId);
            attribute.setCode(code);
        }
        
        attribute.setAttributeKey(this.attributeKey);
        attribute.setAttributeValue(this.attributeValue);
        
        return attribute;
    }
} 