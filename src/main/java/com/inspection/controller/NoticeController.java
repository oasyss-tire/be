package com.inspection.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.inspection.dto.NoticeDTO;
import com.inspection.service.NoticeService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {
    private final NoticeService noticeService;
    private static final Logger log = LoggerFactory.getLogger(NoticeController.class);
    
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<NoticeDTO> createNotice(
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam Long userId,
            @RequestParam boolean popup,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime popupStartDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime popupEndDate,
            @RequestParam(required = false) List<MultipartFile> images) {
        
        // log.info("공지사항 등록 요청");
        // log.info("제목: {}", title);
        // log.info("내용: {}", content);
        // log.info("작성자 ID: {}", userId);
        // log.info("팝업 여부: {}", popup);
        // log.info("팝업 시작일: {}", popupStartDate);
        // log.info("팝업 종료일: {}", popupEndDate);
        // log.info("이미지 첨부 여부: {}", (images != null && !images.isEmpty()));
        
        try {
            NoticeDTO notice = noticeService.createNotice(
                title, content, userId, popup, popupStartDate, popupEndDate, images);
            return ResponseEntity.ok(notice);
        } catch (Exception e) {
            log.error("공지사항 등록 실패: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    @GetMapping
    public ResponseEntity<List<NoticeDTO>> getAllNotices() {
        return ResponseEntity.ok(noticeService.getAllNotices());
    }
    
    @GetMapping("/popups")
    public ResponseEntity<List<NoticeDTO>> getActivePopups() {
        return ResponseEntity.ok(noticeService.getActivePopups());
    }
    
    @GetMapping("/{noticeId}")
    public ResponseEntity<NoticeDTO> getNotice(@PathVariable Long noticeId) {
        return ResponseEntity.ok(noticeService.getNotice(noticeId));
    }
    
    @DeleteMapping("/{noticeId}")
    public ResponseEntity<Void> deleteNotice(@PathVariable Long noticeId) {
        noticeService.deleteNotice(noticeId);
        return ResponseEntity.ok().build();
    }
    
    @PutMapping(value = "/{noticeId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<NoticeDTO> updateNotice(
            @PathVariable Long noticeId,
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam boolean popup,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime popupStartDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime popupEndDate,
            @RequestParam(required = false) List<String> existingImages,
            @RequestParam(required = false) List<MultipartFile> images) {
        
        NoticeDTO updatedNotice = noticeService.updateNotice(
            noticeId, title, content, popup, popupStartDate, popupEndDate, 
            existingImages != null ? existingImages : new ArrayList<>(), 
            images != null ? images : new ArrayList<>());
        return ResponseEntity.ok(updatedNotice);
    }
} 