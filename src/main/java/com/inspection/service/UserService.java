package com.inspection.service;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.inspection.dto.UserCreateDTO;
import com.inspection.dto.UserResponseDTO;
import com.inspection.dto.UserUpdateDTO;
import com.inspection.entity.Company;
import com.inspection.entity.User;
import com.inspection.repository.CompanyRepository;
import com.inspection.repository.UserRepository;
import com.inspection.util.AESEncryption;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CompanyRepository companyRepository;
    private final AESEncryption aesEncryption;
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        return org.springframework.security.core.userdetails.User.builder()
            .username(user.getUsername())
            .password(user.getPassword())
            .roles(user.getRole().name())
            .build();
    }

    @Transactional
    public User createUser(UserCreateDTO userCreateDTO) {
        User user = userCreateDTO.toEntity();
        
        // 비밀번호 암호화
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        
        // 이메일 암호화
        if (user.getEmail() != null) {
            user.setEmail(aesEncryption.encrypt(user.getEmail()));
        }
        
        // 전화번호 암호화
        if (user.getPhoneNumber() != null) {
            user.setPhoneNumber(aesEncryption.encrypt(user.getPhoneNumber()));
        }
        
        if (userCreateDTO.getCompanyId() != null) {
            Company company = companyRepository.findById(userCreateDTO.getCompanyId())
                .orElseThrow(() -> new RuntimeException("존재하지 않는 회사입니다."));
            user.setCompany(company);
        }

        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("이미 존재하는 사용자명입니다.");
        }

        return userRepository.save(user);
    }

    public User getCurrentUser(String username) {
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean validateAdminCode(String adminCode) {
        return "ADMIN123".equals(adminCode);
    }

    @Transactional(readOnly = true)
    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll().stream()
            .map(user -> {
                UserResponseDTO dto = new UserResponseDTO(user);
                try {
                    // 이메일 복호화
                    if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                        try {
                            dto.setEmail(aesEncryption.decrypt(user.getEmail()));
                        } catch (Exception e) {
                            log.warn("이메일 복호화 실패: {}", user.getEmail());
                            dto.setEmail(user.getEmail());  // 실패시 원본값 사용
                        }
                    }
                    
                    // 전화번호 복호화
                    if (user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty()) {
                        try {
                            dto.setPhoneNumber(aesEncryption.decrypt(user.getPhoneNumber()));
                        } catch (Exception e) {
                            log.warn("전화번호 복호화 실패: {}", user.getPhoneNumber());
                            dto.setPhoneNumber(user.getPhoneNumber());  // 실패시 원본값 사용
                        }
                    }
                } catch (Exception e) {
                    log.error("사용자 정보 복호화 중 오류: {}", e.getMessage());
                }
                return dto;
            })
            .collect(Collectors.toList());
    }

    public User getUserById(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다. ID: " + userId));
    }

    @Transactional
    public User updateUser(Long userId, UserUpdateDTO userUpdateDTO) {
        User user = getUserById(userId);
        
        user.setFullName(userUpdateDTO.getFullName());
        
        // 이메일 암호화
        if (userUpdateDTO.getEmail() != null) {
            user.setEmail(aesEncryption.encrypt(userUpdateDTO.getEmail()));
        }
        
        // 전화번호 암호화
        if (userUpdateDTO.getPhoneNumber() != null) {
            user.setPhoneNumber(aesEncryption.encrypt(userUpdateDTO.getPhoneNumber()));
        }
        
        user.setRole(userUpdateDTO.getRole());
        user.setActive(userUpdateDTO.isActive());
        
        if (userUpdateDTO.getCompanyId() != null) {
            Company company = companyRepository.findById(userUpdateDTO.getCompanyId())
                .orElseThrow(() -> new RuntimeException("존재하지 않는 회사입니다."));
            user.setCompany(company);
        }
        
        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다. ID: " + userId));
        userRepository.delete(user);
    }

    // 사용자 정보 조회 시 복호화
    public UserResponseDTO getCurrentUserDTO(String username) {
        User user = getCurrentUser(username);
        UserResponseDTO dto = new UserResponseDTO(user);
        
        // 이메일과 전화번호 복호화
        if (user.getEmail() != null) {
            dto.setEmail(aesEncryption.decrypt(user.getEmail()));
        }
        if (user.getPhoneNumber() != null) {
            dto.setPhoneNumber(aesEncryption.decrypt(user.getPhoneNumber()));
        }
        
        return dto;
    }


    @Transactional
    public void updatePassword(Long userId, String currentPassword, String newPassword) {
        log.info("비밀번호 변경 시도 - userId: {}", userId);
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다. ID: " + userId));
        
        log.info("사용자 조회 성공 - username: {}", user.getUsername());

        // 현재 비밀번호 검증
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            log.error("현재 비밀번호 불일치 - userId: {}", userId);
            throw new RuntimeException("현재 비밀번호가 일치하지 않습니다.");
        }

        log.info("현재 비밀번호 검증 성공");

        // 새 비밀번호 암호화 후 저장
        String encodedPassword = passwordEncoder.encode(newPassword);
        user.setPassword(encodedPassword);
        userRepository.save(user);
        
        log.info("비밀번호 변경 완료 - userId: {}", userId);
    }


} 