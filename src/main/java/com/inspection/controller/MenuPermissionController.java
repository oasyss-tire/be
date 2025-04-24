// src/main/java/com/inspection/controller/MenuPermissionController.java
package com.inspection.controller;

import com.inspection.dto.MenuDTO;
import com.inspection.entity.Role;
import com.inspection.entity.Code;
import com.inspection.service.MenuService;
import com.inspection.repository.CodeRepository;
import com.inspection.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/menu-permissions")
@RequiredArgsConstructor
public class MenuPermissionController {
    private final MenuService menuService;
    private final CodeRepository codeRepository;
    private final UserService userService;

    /**
     * 현재 로그인한 사용자의 접근 가능한 메뉴 목록 조회
     */
    @GetMapping("/user-menus")
    public ResponseEntity<List<MenuDTO>> getUserAccessibleMenus() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        Role role = findUserRole(username);
        
        List<MenuDTO> menus = menuService.getAccessibleMenusByRole(role);
        
        return ResponseEntity.ok(menus);
    }

    /**
     * 특정 경로에 대한 접근 권한 확인
     */
    @GetMapping("/check-access")
    public ResponseEntity<Map<String, Boolean>> checkPathAccess(@RequestParam String path) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        Role role = findUserRole(username);

        boolean hasAccess = menuService.hasMenuAccess(role, path);
        
        Map<String, Boolean> response = new HashMap<>();
        response.put("hasAccess", hasAccess);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 모든 메뉴 목록 조회 (관리자용)
     */
    @GetMapping("/all-menus")
    public ResponseEntity<?> getAllMenus() {
        if (!isAdmin()) {
            log.warn("관리자가 아닌 사용자가 전체 메뉴 목록 조회 시도: {}", 
                SecurityContextHolder.getContext().getAuthentication().getName());
            return ResponseEntity.status(403).body(Map.of("error", "관리자만 접근 가능합니다."));
        }

        List<MenuDTO> allMenus = menuService.getAllMenus();
        return ResponseEntity.ok(allMenus);
    }

    /**
     * 시스템에 등록된 모든 역할 목록 조회 (관리자용)
     */
    @GetMapping("/roles")
    public ResponseEntity<?> getAllRoles() {
        if (!isAdmin()) {
            log.warn("관리자가 아닌 사용자가 역할 목록 조회 시도: {}", 
                SecurityContextHolder.getContext().getAuthentication().getName());
            return ResponseEntity.status(403).body(Map.of("error", "관리자만 접근 가능합니다."));
        }

        return ResponseEntity.ok(List.of(Role.values()));
    }

    /**
     * 특정 역할의 메뉴 권한 목록 조회 (관리자용)
     */
    @GetMapping("/role/{roleId}")
    public ResponseEntity<?> getRolePermissions(@PathVariable String roleId) {
        if (!isAdmin()) {
            log.warn("관리자가 아닌 사용자가 권한 목록 조회 시도: {}", 
                SecurityContextHolder.getContext().getAuthentication().getName());
            return ResponseEntity.status(403).body(Map.of("error", "관리자만 접근 가능합니다."));
        }
        
        try {
            Map<String, Boolean> permissions = menuService.getRoleMenuPermissions(roleId);
            return ResponseEntity.ok(permissions);
        } catch (IllegalArgumentException e) {
            log.error("권한 조회 오류: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 특정 역할의 메뉴 권한 업데이트 (관리자용)
     */
    @PutMapping("/role/{roleId}")
    public ResponseEntity<?> updateRolePermissions(
            @PathVariable String roleId,
            @RequestBody Map<String, Boolean> permissions) {
        
        if (!isAdmin()) {
            log.warn("관리자가 아닌 사용자가 권한 업데이트 시도: {}", 
                SecurityContextHolder.getContext().getAuthentication().getName());
            return ResponseEntity.status(403).body(Map.of("error", "관리자만 접근 가능합니다."));
        }
        
        try {
            menuService.updateRoleMenuPermissions(roleId, permissions);
            return ResponseEntity.ok(Map.of("message", "권한이 성공적으로 업데이트되었습니다."));
        } catch (Exception e) {
            log.error("권한 업데이트 오류: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 현재 사용자가 관리자인지 확인
     */
    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        Role role = findUserRole(username);
        
        return Role.ADMIN.equals(role);
    }
    
    /**
     * 사용자명으로 역할 조회
     */
    private Role findUserRole(String username) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getAuthorities().stream()
            .filter(authority -> authority.getAuthority().startsWith("ROLE_"))
            .map(authority -> Role.valueOf(authority.getAuthority().substring(5))) // "ROLE_" 제거
            .findFirst()
            .orElse(Role.USER); // 기본값
    }
}