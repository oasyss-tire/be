package com.inspection.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;

import com.inspection.dto.UserResponseDTO;
import com.inspection.dto.UserUpdateDTO;
import com.inspection.dto.PageResponseDTO;
import com.inspection.entity.User;
import com.inspection.service.UserService;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    
    // 사용자 조회
    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> getUser(@PathVariable Long id) {
        User user = userService.getUserById(id);
        UserResponseDTO userDTO = userService.getCurrentUserDTO(user.getUserId());
        return ResponseEntity.ok(userDTO);
    }

    // 사용자 목록 조회 API (페이징 지원)
    @GetMapping
    public ResponseEntity<?> getAllUsers(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestParam(value = "sort", required = false) String sort) {
        
        // 페이징 파라미터가 없는 경우 기존 로직 실행 (하위 호환성 유지)
        if (page == null && size == null && sort == null) {
            List<UserResponseDTO> users = userService.getAllUsers();
            return ResponseEntity.ok(users);
        }
        
        // 페이징 파라미터가 있는 경우 페이징 처리
        int pageNum = (page != null) ? page : 0;
        int sizeNum = (size != null) ? size : 10;
        
        // 정렬 설정
        Pageable pageable;
        if (sort != null && !sort.isEmpty()) {
            String[] sortParams = sort.split(",");
            String sortField = sortParams[0];
            Direction direction = (sortParams.length > 1 && sortParams[1].equalsIgnoreCase("asc")) 
                    ? Direction.ASC : Direction.DESC;
            pageable = PageRequest.of(pageNum, sizeNum, Sort.by(direction, sortField));
        } else {
            pageable = PageRequest.of(pageNum, sizeNum, Sort.by(Direction.DESC, "id"));
        }
        
        // 페이징된 데이터 조회
        Page<UserResponseDTO> userPage = userService.getAllUsersWithPaging(pageable);
        
        return ResponseEntity.ok(PageResponseDTO.fromPage(userPage));
    }
    
    // 키워드로 사용자 검색 API (통합 검색)
    @GetMapping("/search/keyword")
    public ResponseEntity<PageResponseDTO<UserResponseDTO>> searchUsersByKeyword(
            @RequestParam("keyword") String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "direction", required = false) String direction) {
        
        // 정렬 설정
        Pageable pageable;
        if (direction == null) {
            // sort 파라미터에 방향이 포함된 경우 (예: "id,desc")
            if (sort != null && sort.contains(",")) {
                String[] sortParams = sort.split(",");
                String sortField = sortParams[0];
                Direction sortDirection = (sortParams.length > 1 && sortParams[1].equalsIgnoreCase("asc")) 
                        ? Direction.ASC : Direction.DESC;
                pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortField));
            } else {
                // 기본 내림차순 정렬 (sort 파라미터가 없거나 방향이 없는 경우)
                String sortField = (sort != null && !sort.isEmpty()) ? sort : "id";
                pageable = PageRequest.of(page, size, Sort.by(Direction.DESC, sortField));
            }
        } else {
            // sort와 direction이 별도 파라미터로 제공된 경우
            Direction sortDirection = direction.equalsIgnoreCase("ASC") ? Direction.ASC : Direction.DESC;
            String sortField = (sort != null && !sort.isEmpty()) ? sort : "id";
            pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortField));
        }
        
        Page<UserResponseDTO> userPage = userService.searchUsersByKeywordWithPaging(keyword, pageable);
        
        return ResponseEntity.ok(PageResponseDTO.fromPage(userPage));
    }

    // 사용자 수정
    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDTO> updateUser(
        @PathVariable Long id,
        @RequestBody UserUpdateDTO userUpdateDTO
    ) {
        User updatedUser = userService.updateUser(id, userUpdateDTO);
        UserResponseDTO userDTO = userService.getCurrentUserDTO(updatedUser.getUserId());
        return ResponseEntity.ok(userDTO);
    }

    // 비밀번호 변경
    @PutMapping("/{id}/password")
    public ResponseEntity<?> updatePassword(
        @PathVariable Long id,
        @RequestBody Map<String, String> passwordRequest
    ) {
        try {
            log.info("비밀번호 변경 요청 - id: {}", id);
            String currentPassword = passwordRequest.get("currentPassword");
            String newPassword = passwordRequest.get("newPassword");

            if (currentPassword == null || newPassword == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("message", "현재 비밀번호와 새 비밀번호를 모두 입력해주세요."));
            }

            userService.updatePassword(id, currentPassword, newPassword);
            return ResponseEntity.ok(Map.of("message", "비밀번호가 변경되었습니다."));
        } catch (RuntimeException e) {
            log.error("비밀번호 변경 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("message", e.getMessage()));
        }
    }

    // 비밀번호 초기화 (관리자용)
    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('ROLE_ADMIN')") // Spring Security를 사용하여 관리자 권한 확인
    public ResponseEntity<?> resetPassword(
        @PathVariable Long id
    ) {
        try {
            log.info("관리자 권한 비밀번호 초기화 요청 - userId: {}", id);
            
            userService.resetPassword(id);
            
            // 보안상 초기화된 비밀번호 자체는 응답에 포함하지 않음
            return ResponseEntity.ok(Map.of(
                "message", "비밀번호가 초기화되었습니다. 사용자에게 기본 초기화 비밀번호를 알려주세요.",
                "userId", id
            ));
        } catch (Exception e) {
            log.error("비밀번호 초기화 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("message", e.getMessage()));
        }
    }
} 