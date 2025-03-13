package com.inspection.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

import com.inspection.dto.LoginRequest;
import com.inspection.dto.LoginResponse;
import com.inspection.dto.UserCreateDTO;
import com.inspection.dto.UserResponseDTO;
import com.inspection.entity.User;
import com.inspection.security.JwtTokenProvider;
import com.inspection.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;


    /* 회원가입 */
    @PostMapping("/signup")
    public ResponseEntity<UserResponseDTO> signup(@RequestBody UserCreateDTO userCreateDTO) {
        User savedUser = userService.createUser(userCreateDTO);
        UserResponseDTO responseDTO = userService.getCurrentUserDTO(savedUser.getUserId());
        return ResponseEntity.ok(responseDTO);
    }

    /* 현재 사용자 정보 조회 */
    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        UserResponseDTO userDTO = userService.getCurrentUserDTO(userDetails.getUsername());
        return ResponseEntity.ok(userDTO);
    }

    /* 아이디 중복 체크 */
    @GetMapping("/check-username")
    public ResponseEntity<?> checkUsername(@RequestParam String userId) {
        if (userService.existsByUserId(userId)) {
            return ResponseEntity
                .status(HttpStatus.CONFLICT)  
                .body("이미 사용중인 아이디입니다.");
        }
        return ResponseEntity.ok("사용 가능한 아이디입니다.");
    }


    /* 로그인 */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            // 아이디 존재 여부 확인 
            if (!userService.existsByUserId(loginRequest.getUserId())) {
                return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "존재하지 않는 사용자입니다."));
            }

            // 인증 시도 
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getUserId(), 
                    loginRequest.getPassword()
                )
            );

            // JWT 토큰 발급 
            String token = tokenProvider.generateToken(authentication);
            UserResponseDTO userResponse = userService.getCurrentUserDTO(loginRequest.getUserId());

            return ResponseEntity.ok(new LoginResponse(token, userResponse));

        } catch (Exception e) {
            // 비밀번호가 틀린 경우 
            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("success", false, "message", "비밀번호가 틀렸습니다."));
        }
    }


    /* 로그아웃 */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                tokenProvider.invalidateToken(token);
                log.info("사용자 로그아웃 성공");
                return ResponseEntity.ok().body(Map.of("message", "로그아웃 되었습니다."));
            }
            return ResponseEntity.badRequest().body(Map.of("message", "유효하지 않은 토큰입니다."));
        } catch (Exception e) {
            log.error("로그아웃 처리 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "로그아웃 처리 중 오류가 발생했습니다."));
        }
    }
} 