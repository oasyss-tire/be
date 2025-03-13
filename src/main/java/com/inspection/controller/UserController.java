package com.inspection.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.inspection.dto.UserResponseDTO;
import com.inspection.dto.UserUpdateDTO;
import com.inspection.entity.User;
import com.inspection.service.UserService;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    // 모든 사용자 조회
    @GetMapping
    public ResponseEntity<List<UserResponseDTO>> getAllUsers() {
        List<UserResponseDTO> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
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
} 