package com.inspection.service;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
import com.inspection.util.EncryptionUtil;
import com.inspection.util.PasswordValidator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {
    
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final EncryptionUtil aesEncryption;
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    // 사용자 로드
    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        User user = userRepository.findByUserId(userId)
            .orElseThrow(() -> new UsernameNotFoundException("User not found with userId: " + userId));

        return org.springframework.security.core.userdetails.User.builder()
            .username(user.getUserId())
            .password(user.getPassword())
            .roles(user.getRole().name())
            .build();
    }


    // 사용자 생성
    @Transactional
    public User createUser(UserCreateDTO userCreateDTO) {
        // 비밀번호 정책 검증
        if (!PasswordValidator.isValid(userCreateDTO.getPassword())) {
            throw new RuntimeException(PasswordValidator.getPasswordPolicy());
        }
        
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

        if (userRepository.existsByUserId(user.getUserId())) {
            throw new RuntimeException("이미 존재하는 사용자 ID입니다.");
        }
        
        // Company 설정
        if (userCreateDTO.getCompanyId() != null) {
            Company company = companyRepository.findById(userCreateDTO.getCompanyId())
                .orElseThrow(() -> new RuntimeException("회사 정보를 찾을 수 없습니다. ID: " + userCreateDTO.getCompanyId()));
            user.setCompany(company);
        }

        return userRepository.save(user);
    }

    // 사용자 조회
    public User getCurrentUser(String userId) {
        return userRepository.findByUserId(userId)
            .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
    }

    // 사용자 존재 여부 확인
    public boolean existsByUserId(String userId) {
        return userRepository.existsByUserId(userId);
    }

    // 관리자 코드 검증
    public boolean validateAdminCode(String adminCode) {
        return "ADMIN123".equals(adminCode);
    }

    // 모든 사용자 조회
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


    // 사용자 조회
    public User getUserById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다. ID: " + id));
    }


    // 사용자 수정
    @Transactional
    public User updateUser(Long id, UserUpdateDTO userUpdateDTO) {
        User user = getUserById(id);
        
        user.setUserName(userUpdateDTO.getUserName());
        
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
        
        // Company 설정
        if (userUpdateDTO.getCompanyId() != null) {
            Company company = companyRepository.findById(userUpdateDTO.getCompanyId())
                .orElseThrow(() -> new RuntimeException("회사 정보를 찾을 수 없습니다. ID: " + userUpdateDTO.getCompanyId()));
            user.setCompany(company);
        } else {
            user.setCompany(null); // Company 연결 해제
        }
        
        return userRepository.save(user);
    }



    // 사용자 정보 조회 시 복호화
    public UserResponseDTO getCurrentUserDTO(String userId) {
        User user = getCurrentUser(userId);
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


    
    // 비밀번호 변경
    @Transactional
    public void updatePassword(Long id, String currentPassword, String newPassword) {
        log.info("비밀번호 변경 시도 - id: {}", id);
        
        // 비밀번호 정책 검증
        if (!PasswordValidator.isValid(newPassword)) {
            throw new RuntimeException(PasswordValidator.getPasswordPolicy());
        }
        
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다. ID: " + id));
        
        log.info("사용자 조회 성공 - userId: {}", user.getUserId());

        // 현재 비밀번호 검증
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            log.error("현재 비밀번호 불일치 - id: {}", id);
            throw new RuntimeException("현재 비밀번호가 일치하지 않습니다.");
        }

        log.info("현재 비밀번호 검증 성공");

        // 새 비밀번호 암호화 후 저장
        String encodedPassword = passwordEncoder.encode(newPassword);
        user.setPassword(encodedPassword);
        userRepository.save(user);
        
        log.info("비밀번호 변경 완료 - id: {}", id);
    }

    // 모든 사용자 조회 (페이징)
    @Transactional(readOnly = true)
    public Page<UserResponseDTO> getAllUsersWithPaging(Pageable pageable) {
        return userRepository.findAll(pageable)
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
            });
    }
    
    // 키워드로 사용자 검색 (페이징)
    @Transactional(readOnly = true)
    public Page<UserResponseDTO> searchUsersByKeywordWithPaging(String keyword, Pageable pageable) {
        return userRepository.findByUserIdContainingOrUserNameContaining(keyword, keyword, pageable)
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
            });
    }

    // 비밀번호 초기화 - 관리자용
    @Transactional
    public void resetPassword(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다. ID: " + userId));
        
        // 비밀번호 초기화 (고정된 초기화 비밀번호)
        String initialPassword = "tb0000!@"; // 기본 초기화 비밀번호
        
        // 비밀번호 정책 검증 (고정된 초기화 비밀번호도 정책을 만족하는지 확인)
        if (!PasswordValidator.isValid(initialPassword)) {
            throw new RuntimeException("초기화 비밀번호가 정책을 만족하지 않습니다. 개발자에게 문의하세요.");
        }
        
        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(initialPassword);
        user.setPassword(encodedPassword);
        
        // 사용자 상태를 활성화로 변경 (필요 시 제거)
        user.setActive(true);
        
        userRepository.save(user);
        log.info("사용자 비밀번호 초기화 완료 - userId: {}, userName: {}", user.getUserId(), user.getUserName());
    }
} 