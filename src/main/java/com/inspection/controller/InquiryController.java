package com.inspection.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import com.inspection.dto.InquiryDTO;
import com.inspection.service.InquiryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/inquiries")
@RequiredArgsConstructor
@Slf4j
public class InquiryController {
    private final InquiryService inquiryService;
    
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<InquiryDTO> createInquiry(
            @RequestParam String inquiryTitle,
            @RequestParam String inquiryContent,
            @RequestParam Long userId,
            @RequestParam String contactNumber,
            @RequestParam(required = false) List<MultipartFile> images) {
        
        try {
            log.info("Creating inquiry - Title: {}, UserId: {}", inquiryTitle, userId);
            InquiryDTO createdInquiry = inquiryService.createInquiry(
                inquiryTitle, inquiryContent, userId, contactNumber, images);
            log.info("Successfully created inquiry with id: {}", createdInquiry.getInquiryId());
            return ResponseEntity.ok(createdInquiry);
        } catch (Exception e) {
            log.error("Error creating inquiry: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(null);
        }
    }

    @GetMapping
    public ResponseEntity<List<InquiryDTO>> getAllInquiries() {
        List<InquiryDTO> inquiries = inquiryService.getAllInquiries();
        return ResponseEntity.ok(inquiries);
    }

    @GetMapping("/{inquiryId}")
    public ResponseEntity<InquiryDTO> getInquiry(@PathVariable Long inquiryId) {
        InquiryDTO inquiry = inquiryService.getInquiry(inquiryId);
        return ResponseEntity.ok(inquiry);
    }

    @PutMapping(value = "/{inquiryId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateInquiry(
            @PathVariable Long inquiryId,
            @RequestParam(required = false) String inquiryTitle,
            @RequestParam(required = false) String inquiryContent,
            @RequestParam(required = false) String contactNumber,
            @RequestParam(required = false) List<MultipartFile> images,
            @RequestParam(required = false) List<String> existingImages,
            @RequestParam(required = false) Boolean processed,
            @RequestParam(required = false) String processContent,
            @RequestParam(required = false) String memo) {
        
        try {
            InquiryDTO updatedInquiry = inquiryService.updateInquiry(
                inquiryId, inquiryTitle, inquiryContent, contactNumber,
                images, existingImages, processed, processContent, memo);
            return ResponseEntity.ok(updatedInquiry);
        } catch (RuntimeException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @DeleteMapping("/{inquiryId}")
    public ResponseEntity<Void> deleteInquiry(@PathVariable Long inquiryId) {
        try {
            inquiryService.deleteInquiry(inquiryId);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
} 