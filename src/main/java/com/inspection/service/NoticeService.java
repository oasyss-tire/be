package com.inspection.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
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

import com.inspection.dto.NoticeDTO;
import com.inspection.entity.Notice;
import com.inspection.entity.User;
import com.inspection.repository.NoticeRepository;
import com.inspection.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NoticeService {
    private final NoticeRepository noticeRepository;
    private final UserRepository userRepository;
    private final String uploadDir = "uploads/images/";
    private static final Logger log = LoggerFactory.getLogger(NoticeService.class);

    @Transactional
    public NoticeDTO createNotice(String title, String content, Long userId, 
                                boolean popup, LocalDateTime popupStartDate, 
                                LocalDateTime popupEndDate, List<MultipartFile> images) {
        User writer = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
            
        Notice notice = new Notice();
        notice.setTitle(title);
        notice.setContent(content);
        notice.setWriter(writer);
        notice.setPopup(popup);
        notice.setPopupStartDate(popupStartDate);
        notice.setPopupEndDate(popupEndDate);
        
        if (images != null && !images.isEmpty()) {
            List<String> imageUrls = new ArrayList<>();
            for (MultipartFile image : images) {
                if (!image.isEmpty()) {
                    String imageUrl = saveImage(image);
                    imageUrls.add(imageUrl);
                }
            }
            notice.setImageUrls(imageUrls);
        }
        
        Notice savedNotice = noticeRepository.save(notice);
        return convertToDTO(savedNotice);
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
        } catch (SecurityException e) {
            throw new RuntimeException("파일 접근 권한 없음: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public List<NoticeDTO> getAllNotices() {
        return noticeRepository.findAllByOrderByCreatedAtDesc().stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<NoticeDTO> getActivePopups() {
        return noticeRepository.findActivePopups(LocalDateTime.now()).stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    @Transactional
    public void deleteNotice(Long noticeId) {
        Notice notice = noticeRepository.findById(noticeId)
            .orElseThrow(() -> new RuntimeException("공지사항을 찾을 수 없습니다."));
            
        // 권한 체크 추가
        Long currentUserId = getCurrentUserId();
        validateWriter(notice, currentUserId);
        
        // 이미지가 있다면 파일 시스템에서도 삭제
        if (notice.getImageUrls() != null && !notice.getImageUrls().isEmpty()) {
            for (String imageUrl : notice.getImageUrls()) {
                deleteImage(imageUrl);
            }
        }
        
        noticeRepository.deleteById(noticeId);
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

    @Transactional(readOnly = true)
    public NoticeDTO getNotice(Long noticeId) {
        Notice notice = noticeRepository.findById(noticeId)
            .orElseThrow(() -> new RuntimeException("공지사항을 찾을 수 없습니다."));
        return convertToDTO(notice);
    }

    @Transactional
    public NoticeDTO updateNotice(Long noticeId, String title, String content, 
                                boolean popup, LocalDateTime popupStartDate, 
                                LocalDateTime popupEndDate, 
                                List<String> existingImages,
                                List<MultipartFile> newImages) {
        Notice notice = noticeRepository.findById(noticeId)
            .orElseThrow(() -> new RuntimeException("공지사항을 찾을 수 없습니다."));
        
        // 권한 체크
        Long currentUserId = getCurrentUserId();
        validateWriter(notice, currentUserId);
        
        // 기본 정보 업데이트
        notice.setTitle(title);
        notice.setContent(content);
        notice.setPopup(popup);
        notice.setPopupStartDate(popupStartDate);
        notice.setPopupEndDate(popupEndDate);
        
        // 현재 저장된 이미지 중 유지되지 않는 이미지 삭제
        List<String> currentImages = notice.getImageUrls();
        if (currentImages != null) {
            for (String oldImage : currentImages) {
                if (!existingImages.contains(oldImage)) {
                    deleteImage(oldImage);
                }
            }
        }
        
        // 새로운 이미지 저장
        List<String> updatedImageUrls = new ArrayList<>(existingImages);
        for (MultipartFile newImage : newImages) {
            if (!newImage.isEmpty()) {
                String imageUrl = saveImage(newImage);
                updatedImageUrls.add(imageUrl);
            }
        }
        
        notice.setImageUrls(updatedImageUrls);
        
        Notice updatedNotice = noticeRepository.save(notice);
        return convertToDTO(updatedNotice);
    }

    private void validateWriter(Notice notice, Long userId) {
        if (!notice.getWriter().getId().equals(userId)) {
            throw new RuntimeException("공지사항 작성자만 수정/삭제할 수 있습니다.");
        }
    }

    private NoticeDTO convertToDTO(Notice notice) {
        NoticeDTO dto = new NoticeDTO();
        dto.setNoticeId(notice.getNoticeId());
        dto.setTitle(notice.getTitle());
        dto.setContent(notice.getContent());
        dto.setWriterName(notice.getWriter().getUserName());
        dto.setWriterId(notice.getWriter().getId());
        dto.setCreatedAt(notice.getCreatedAt());
        dto.setPopup(notice.isPopup());
        dto.setPopupStartDate(notice.getPopupStartDate());
        dto.setPopupEndDate(notice.getPopupEndDate());
        dto.setImageUrls(notice.getImageUrls());
        return dto;
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new RuntimeException("인증 정보를 찾을 수 없습니다.");
        }
        
        String userId = authentication.getName();
        User user = userRepository.findByUserId(userId)
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        return user.getId();
    }
} 