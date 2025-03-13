package com.inspection.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.inspection.dto.InquiryDTO;
import com.inspection.entity.Inquiry;
import com.inspection.entity.Role;
import com.inspection.entity.User;
import com.inspection.repository.InquiryRepository;
import com.inspection.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class InquiryService {
    private final InquiryRepository inquiryRepository;
    private final UserRepository userRepository;
    private final String uploadDir = "uploads/inquiry_images/";

    @Transactional
    public InquiryDTO createInquiry(String inquiryTitle, String inquiryContent, 
                                   Long userId, String contactNumber, 
                                   List<MultipartFile> images) {
        try {
            log.info("Starting to create inquiry for user: {}", userId);
            User writer = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
            
            // 문의사항 엔티티 생성
            Inquiry inquiry = new Inquiry();
            inquiry.setInquiryTitle(inquiryTitle);
            inquiry.setInquiryContent(inquiryContent);
            inquiry.setWriter(writer);
            inquiry.setContactNumber(contactNumber);
            inquiry.setProcessed(false);  // 초기 처리상태는 false
            
            // 이미지 처리
            if (images != null && !images.isEmpty()) {
                List<String> imageUrls = new ArrayList<>();
                for (MultipartFile image : images) {
                    if (!image.isEmpty()) {
                        String imageUrl = saveImage(image);
                        imageUrls.add(imageUrl);
                    }
                }
                inquiry.setImageUrls(imageUrls);
            }
            
            // 저장
            Inquiry savedInquiry = inquiryRepository.save(inquiry);
            log.info("Successfully saved inquiry to database");
            return convertToDTO(savedInquiry);
        } catch (Exception e) {
            log.error("Error in createInquiry: ", e);
            throw e;
        }
    }

    @Transactional
    public InquiryDTO updateInquiry(Long inquiryId, String inquiryTitle, 
                                String inquiryContent, String contactNumber,
                                List<MultipartFile> newImages, 
                                List<String> existingImages,
                                Boolean processed, String processContent, 
                                String memo) {
        
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
            .orElseThrow(() -> new RuntimeException("문의사항을 찾을 수 없습니다."));
            
        User currentUser = getCurrentUser();
        
        // ✅ MANAGER도 ADMIN과 동일한 권한을 갖도록 수정
        boolean isAdminUser = isAdminOrManager(currentUser);

        // 권한 체크 - ADMIN, MANAGER이거나 작성자 본인이어야 함
        if (!isAdminUser && !isWriter(currentUser, inquiry)) {
            throw new RuntimeException("수정 권한이 없습니다.");
        }
        
        // ADMIN, MANAGER가 아닌 경우, 처리 관련 필드 수정 불가
        if (!isAdminUser) {
            if (processed != null || processContent != null || memo != null) {
                throw new RuntimeException("처리 상태는 관리자만 수정할 수 있습니다.");
            }
            
            // 기본 정보만 수정 가능
            if (inquiryTitle != null) inquiry.setInquiryTitle(inquiryTitle);
            if (inquiryContent != null) inquiry.setInquiryContent(inquiryContent);
            if (contactNumber != null) inquiry.setContactNumber(contactNumber);
            
            // 이미지 처리
            if (newImages != null || existingImages != null) {
                handleImages(inquiry, newImages, existingImages);
            }
        } else {
            // ✅ ADMIN, MANAGER는 모든 필드 수정 가능
            if (inquiryTitle != null) inquiry.setInquiryTitle(inquiryTitle);
            if (inquiryContent != null) inquiry.setInquiryContent(inquiryContent);
            if (contactNumber != null) inquiry.setContactNumber(contactNumber);
            if (processed != null) inquiry.setProcessed(processed);
            if (processContent != null) inquiry.setProcessContent(processContent);
            if (memo != null) inquiry.setMemo(memo);
            
            // 이미지 처리
            if (newImages != null || existingImages != null) {
                handleImages(inquiry, newImages, existingImages);
            }
        }
        
        Inquiry updatedInquiry = inquiryRepository.save(inquiry);
        return convertToDTO(updatedInquiry);
    }

    /**
     * ✅ ADMIN과 MANAGER를 한 번에 체크하는 함수 추가
     */
    private boolean isAdminOrManager(User user) {
        return user.getRole().name().equalsIgnoreCase("ADMIN") || user.getRole().name().equalsIgnoreCase("MANAGER");
    }

    @Transactional
    public void deleteInquiry(Long inquiryId) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
            .orElseThrow(() -> new RuntimeException("문의사항을 찾을 수 없습니다."));
            
        // 현재 로그인한 사용자 정보 가져오기
        User currentUser = getCurrentUser();
        
        // 권한 체크 - ADMIN이거나 작성자 본인이어야 함
        if (!isAdmin(currentUser) && !isWriter(currentUser, inquiry)) {
            throw new RuntimeException("삭제 권한이 없습니다.");
        }
        
        // 이미지 파일 삭제
        if (inquiry.getImageUrls() != null && !inquiry.getImageUrls().isEmpty()) {
            for (String imageUrl : inquiry.getImageUrls()) {
                deleteImage(imageUrl);
            }
        }
        
        inquiryRepository.delete(inquiry);
    }

    private boolean isAdmin(User user) {
        Role role = user.getRole();
        // log.info("Checking admin role. User role: {}", role);
        return "ADMIN".equals(role.name());  // enum의 name() 사용
    }

    private boolean isWriter(User user, Inquiry inquiry) {
        return inquiry.getWriter().getUserId().equals(user.getUserId());
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        return userRepository.findByUserId(userId)
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
    }

    private void handleImages(Inquiry inquiry, List<MultipartFile> newImages, 
                            List<String> existingImages) {
        List<String> currentImages = inquiry.getImageUrls();
        
        // existingImages가 null이거나 비어있으면 모든 기존 이미지 삭제
        if (currentImages != null && (existingImages == null || existingImages.isEmpty())) {
            for (String oldImage : currentImages) {
                deleteImage(oldImage);
            }
            inquiry.setImageUrls(new ArrayList<>());
        }
        // 기존 이미지 중 삭제된 이미지 처리
        else if (currentImages != null && existingImages != null) {
            for (String oldImage : currentImages) {
                if (!existingImages.contains(oldImage)) {
                    deleteImage(oldImage);
                }
            }
        }
        
        // 새로운 이미지 목록 생성
        List<String> updatedImageUrls = new ArrayList<>();
        if (existingImages != null) {
            updatedImageUrls.addAll(existingImages);
        }
        
        // 새 이미지 추가
        if (newImages != null) {
            for (MultipartFile image : newImages) {
                if (!image.isEmpty()) {
                    String imageUrl = saveImage(image);
                    updatedImageUrls.add(imageUrl);
                }
            }
        }
        
        inquiry.setImageUrls(updatedImageUrls);
    }

    private String saveImage(MultipartFile image) {
        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String filename = UUID.randomUUID().toString() + "_" + image.getOriginalFilename();
            Path filePath = uploadPath.resolve(filename);
            Files.copy(image.getInputStream(), filePath);

            return filename;
        } catch (IOException e) {
            throw new RuntimeException("이미지 파일 저장 실패: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public List<InquiryDTO> getAllInquiries() {
        return inquiryRepository.findAllWithWriter().stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InquiryDTO getInquiry(Long inquiryId) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
            .orElseThrow(() -> new RuntimeException("문의사항을 찾을 수 없습니다."));
        return convertToDTO(inquiry);
    }

    private InquiryDTO convertToDTO(Inquiry inquiry) {
        InquiryDTO dto = new InquiryDTO();
        dto.setInquiryId(inquiry.getInquiryId());
        dto.setInquiryTitle(inquiry.getInquiryTitle());
        dto.setInquiryContent(inquiry.getInquiryContent());
        dto.setCreatedAt(inquiry.getCreatedAt());
        dto.setImageUrls(inquiry.getImageUrls());
        dto.setWriterName(inquiry.getWriter().getUserName());
        dto.setWriterId(inquiry.getWriter().getId());
        dto.setContactNumber(inquiry.getContactNumber());
        dto.setProcessed(inquiry.isProcessed());
        dto.setProcessContent(inquiry.getProcessContent());
        dto.setMemo(inquiry.getMemo());
        return dto;
    }

    private void deleteImage(String imageUrl) {
        try {
            Path imagePath = Paths.get(uploadDir).resolve(imageUrl);
            Files.deleteIfExists(imagePath);
        } catch (IOException e) {
            // 파일 삭제 실패는 로그만 남기고 계속 진행
            log.error("이미지 파일 삭제 실패: {}", e.getMessage());
        }
    }
} 