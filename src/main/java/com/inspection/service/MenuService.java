package com.inspection.service;

import com.inspection.dto.MenuDTO;
import com.inspection.entity.Code;
import com.inspection.entity.CodeAttribute;
import com.inspection.entity.Role;
import com.inspection.repository.CodeAttributeRepository;
import com.inspection.repository.CodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MenuService {
    private final CodeRepository codeRepository;
    private final CodeAttributeRepository codeAttributeRepository;

    private static final String MENU_GROUP_ID = "008001";
    private static final String ROLE_GROUP_ID = "009001";

    /**
     * 사용자 역할에 따라 접근 가능한 메뉴 목록을 조회합니다.
     */
    public List<MenuDTO> getAccessibleMenusByRole(Role role) {
        if (role == null) {
            log.warn("역할이 없는 사용자의 메뉴 접근 요청");
            return Collections.emptyList();
        }

        // 역할에 해당하는 코드 조회
        String roleCodeId = getRoleCodeId(role);
        Code roleCode = codeRepository.findById(roleCodeId)
                .orElseThrow(() -> new IllegalArgumentException("역할 코드를 찾을 수 없습니다: " + roleCodeId));

        // 해당 역할의 코드 속성에서 접근 가능한 메뉴 ID 목록 조회
        List<CodeAttribute> permissionAttrs = codeAttributeRepository.findByCode(roleCode);
        
        // 메뉴 ID와 권한 값(true/false) 맵 생성
        Map<String, Boolean> menuPermissions = new HashMap<>();
        permissionAttrs.forEach(attr -> {
            try {
                // 속성 키가 메뉴 ID, 값이 권한 여부(true/false)
                menuPermissions.put(attr.getAttributeKey(), Boolean.parseBoolean(attr.getAttributeValue()));
            } catch (Exception e) {
                log.warn("권한 값 파싱 오류: {}, {}", attr.getAttributeKey(), attr.getAttributeValue());
            }
        });
        
        // 권한이 있는 메뉴 ID 목록
        List<String> permittedMenuIds = menuPermissions.entrySet().stream()
                .filter(Map.Entry::getValue) // true인 항목만 필터링
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        log.debug("역할 '{}' 사용자의 접근 가능 메뉴 ID: {}", role, permittedMenuIds);

        if (permittedMenuIds.isEmpty()) {
            log.warn("역할 '{}' 에 할당된 메뉴 권한이 없습니다", role);
            return Collections.emptyList();
        }

        // 메뉴 정보 조회 및 DTO 변환
        List<MenuDTO> accessibleMenus = codeRepository.findByCodeIdIn(permittedMenuIds).stream()
                .map(this::convertToMenuDTO)
                .collect(Collectors.toList());

        log.debug("역할 '{}' 사용자의 접근 가능 메뉴 수: {}", role, accessibleMenus.size());
        return accessibleMenus;
    }

    /**
     * 역할에 해당하는 코드 ID 조회
     */
    private String getRoleCodeId(Role role) {
        return ROLE_GROUP_ID + "_" + String.format("%04d", role.ordinal() + 1);
    }

    /**
     * 코드 객체를 MenuDTO로 변환합니다.
     */
    private MenuDTO convertToMenuDTO(Code code) {
        MenuDTO dto = new MenuDTO();
        dto.setId(code.getCodeId());
        dto.setName(code.getCodeName());
        dto.setPath(code.getDescription()); // 메뉴 경로는 description에 저장됨
        
        // 메뉴 속성 설정 (아이콘 등)
        List<CodeAttribute> attributes = codeAttributeRepository.findByCode(code);
        for (CodeAttribute attr : attributes) {
            switch (attr.getAttributeKey()) {
                case "icon":
                    dto.setIcon(attr.getAttributeValue());
                    break;
                case "category":
                    dto.setCategory(attr.getAttributeValue());
                    break;
                case "displayOrder":
                    try {
                        dto.setDisplayOrder(Integer.parseInt(attr.getAttributeValue()));
                    } catch (NumberFormatException e) {
                        log.warn("메뉴 '{}' 표시 순서 값 오류: {}", code.getCodeId(), attr.getAttributeValue());
                        dto.setDisplayOrder(Integer.parseInt(code.getCodeId().split("_")[1])); // 코드ID에서 순서 추출
                    }
                    break;
                case "visible":
                    dto.setVisible(Boolean.parseBoolean(attr.getAttributeValue()));
                    break;
            }
        }
        
        // 표시 순서 기본값 설정 (필요한 경우)
        if (dto.getDisplayOrder() == 0) {
            try {
                // 코드ID에서 순서 정보 추출 (예: 008001001_0001 -> 1)
                String orderPart = code.getCodeId().split("_")[1];
                dto.setDisplayOrder(Integer.parseInt(orderPart));
            } catch (Exception e) {
                dto.setDisplayOrder(999); // 기본값
            }
        }
        
        // 카테고리 설정 (그룹 ID에서 추출)
        if (dto.getCategory() == null && code.getCodeGroup() != null) {
            dto.setCategory(code.getCodeGroup().getGroupName());
        }
        
        return dto;
    }

    /**
     * 특정 역할이 주어진 경로에 접근할 수 있는지 확인합니다.
     */
    public boolean hasMenuAccess(Role role, String path) {
        if (role == null || path == null) {
            return false;
        }

        // 관리자는 모든 경로에 접근 가능
        if (Role.ADMIN.equals(role)) {
            return true;
        }

        // 해당 역할에 할당된 메뉴 목록 조회
        List<MenuDTO> accessibleMenus = getAccessibleMenusByRole(role);
        
        // 경로가 정확히 일치하거나 하위 경로인지 확인
        return accessibleMenus.stream()
                .anyMatch(menu -> {
                    String menuPath = menu.getPath();
                    return path.equals(menuPath) || 
                           (menuPath != null && !menuPath.equals("/") && path.startsWith(menuPath + "/"));
                });
    }

    /**
     * 모든 메뉴 목록을 조회합니다. (관리자용)
     */
    public List<MenuDTO> getAllMenus() {
        // 모든 메뉴 카테고리에서 메뉴 코드 조회
        List<Code> allMenus = new ArrayList<>();
        codeRepository.findByCodeGroupGroupId(MENU_GROUP_ID + "001").forEach(allMenus::add); // 계약 메뉴
        codeRepository.findByCodeGroupGroupId(MENU_GROUP_ID + "002").forEach(allMenus::add); // 시설물 메뉴
        codeRepository.findByCodeGroupGroupId(MENU_GROUP_ID + "003").forEach(allMenus::add); // 관리 메뉴
        codeRepository.findByCodeGroupGroupId(MENU_GROUP_ID + "004").forEach(allMenus::add); // 커뮤니티 메뉴
        
        return allMenus.stream()
                .map(this::convertToMenuDTO)
                .sorted(Comparator.comparingInt(MenuDTO::getDisplayOrder))
                .collect(Collectors.toList());
    }

    /**
     * 특정 역할의 메뉴 권한 목록을 조회합니다. (관리자용)
     */
    public Map<String, Boolean> getRoleMenuPermissions(String roleId) {
        // 역할 ID 유효성 검사
        Role role;
        try {
            role = Role.valueOf(roleId.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.error("유효하지 않은 역할 ID: {}", roleId);
            throw new IllegalArgumentException("유효하지 않은 역할 ID: " + roleId);
        }

        // 역할 코드 조회
        String roleCodeId = getRoleCodeId(role);
        Code roleCode = codeRepository.findById(roleCodeId)
                .orElseThrow(() -> new IllegalArgumentException("역할 코드를 찾을 수 없습니다: " + roleCodeId));
        
        // 해당 역할의 코드 속성에서 권한 정보 조회
        List<CodeAttribute> permissionAttrs = codeAttributeRepository.findByCode(roleCode);
        
        // 메뉴 ID와 권한 값(true/false) 맵 생성
        Map<String, Boolean> permissions = new HashMap<>();
        permissionAttrs.forEach(attr -> {
            try {
                permissions.put(attr.getAttributeKey(), Boolean.parseBoolean(attr.getAttributeValue()));
            } catch (Exception e) {
                log.warn("권한 값 파싱 오류: {}, {}", attr.getAttributeKey(), attr.getAttributeValue());
            }
        });
        
        // 모든 메뉴에 대한 권한 맵 완성 (권한 설정이 없는 메뉴는 false로 기본 설정)
        List<String> allMenuIds = getAllMenus().stream()
                .map(MenuDTO::getId)
                .collect(Collectors.toList());
        
        allMenuIds.forEach(menuId -> {
            if (!permissions.containsKey(menuId)) {
                permissions.put(menuId, false);
            }
        });

        return permissions;
    }

    /**
     * 특정 역할의 메뉴 권한을 업데이트합니다. (관리자용)
     * 전달된 권한 맵에 포함된 메뉴에 대해서만 권한을 변경하고, 나머지는 기존 권한을 유지합니다.
     */
    @Transactional
    public void updateRoleMenuPermissions(String roleId, Map<String, Boolean> permissions) {
        // 역할 ID 유효성 검사
        Role role;
        try {
            role = Role.valueOf(roleId.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.error("유효하지 않은 역할 ID: {}", roleId);
            throw new IllegalArgumentException("유효하지 않은 역할 ID: " + roleId);
        }

        // ADMIN 역할의 권한은 변경 불가
        if (Role.ADMIN.equals(role)) {
            log.warn("관리자 역할의 권한은 변경할 수 없습니다");
            throw new IllegalArgumentException("관리자 역할의 권한은 변경할 수 없습니다");
        }

        // 역할 코드 조회
        String roleCodeId = getRoleCodeId(role);
        Code roleCode = codeRepository.findById(roleCodeId)
                .orElseThrow(() -> new IllegalArgumentException("역할 코드를 찾을 수 없습니다: " + roleCodeId));
        
        // 기존 권한 정보 조회 (현재 설정된 모든 메뉴 권한)
        List<CodeAttribute> existingAttrs = codeAttributeRepository.findByCode(roleCode);
        Map<String, CodeAttribute> existingMenuPermissions = new HashMap<>();
        
        // 기존 메뉴 권한 속성 맵 구성
        existingAttrs.forEach(attr -> {
            // 메뉴 ID 패턴을 가진 속성만 처리
            if (attr.getAttributeKey().matches("^\\d+_\\d+$")) {
                existingMenuPermissions.put(attr.getAttributeKey(), attr);
            }
        });
        
        // 업데이트할 권한 처리
        permissions.forEach((menuId, hasAccess) -> {
            CodeAttribute attr = existingMenuPermissions.get(menuId);
            
            if (attr == null) {
                // 기존에 없던 메뉴 권한인 경우 새로 생성
                attr = new CodeAttribute();
                attr.setCode(roleCode);
                attr.setAttributeKey(menuId);
                attr.setCreatedBy("system");
            }
            
            // 권한 값 업데이트
            attr.setAttributeValue(hasAccess.toString());
            codeAttributeRepository.save(attr);
            
            // 처리된 항목은 맵에서 제거
            existingMenuPermissions.remove(menuId);
        });

        log.info("역할 '{}' 의 메뉴 권한이 업데이트되었습니다", roleId);
    }
}

