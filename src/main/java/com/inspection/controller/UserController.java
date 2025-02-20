package com.inspection.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;
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

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponseDTO> getUser(@PathVariable Long userId) {
        User user = userService.getUserById(userId);
        UserResponseDTO userDTO = userService.getCurrentUserDTO(user.getUsername());
        return ResponseEntity.ok(userDTO);
    }

    @GetMapping
    public ResponseEntity<List<UserResponseDTO>> getAllUsers() {
        List<UserResponseDTO> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{userId}")
    public ResponseEntity<UserResponseDTO> updateUser(
        @PathVariable Long userId,
        @RequestBody UserUpdateDTO userUpdateDTO
    ) {
        User updatedUser = userService.updateUser(userId, userUpdateDTO);
        UserResponseDTO userDTO = userService.getCurrentUserDTO(updatedUser.getUsername());
        return ResponseEntity.ok(userDTO);
    }

    @PutMapping("/{userId}/password")
    public ResponseEntity<?> updatePassword(
        @PathVariable Long userId,
        @RequestBody Map<String, String> passwordRequest
    ) {
        try {
            log.info("비밀번호 변경 요청 - userId: {}", userId);
            String currentPassword = passwordRequest.get("currentPassword");
            String newPassword = passwordRequest.get("newPassword");

            if (currentPassword == null || newPassword == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("message", "현재 비밀번호와 새 비밀번호를 모두 입력해주세요."));
            }

            userService.updatePassword(userId, currentPassword, newPassword);
            return ResponseEntity.ok(Map.of("message", "비밀번호가 변경되었습니다."));
        } catch (RuntimeException e) {
            log.error("비밀번호 변경 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("message", e.getMessage()));
        }
    }


    

} 