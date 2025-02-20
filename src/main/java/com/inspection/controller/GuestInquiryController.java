package com.inspection.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.inspection.dto.GuestInquiryDTO;
import com.inspection.dto.GuestInquiryListDTO;
import com.inspection.repository.GuestInquiryRepository;
import com.inspection.service.GuestInquiryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/guest-inquiries")
@RequiredArgsConstructor
public class GuestInquiryController {
    
    private final GuestInquiryService guestInquiryService;
    private final GuestInquiryRepository guestInquiryRepository;

    // 문의 등록 (비회원용)
    @PostMapping
    public ResponseEntity<GuestInquiryDTO> create(@RequestBody GuestInquiryDTO inquiryDTO) {
        return ResponseEntity.ok(guestInquiryService.create(inquiryDTO));
    }

    // 문의 조회 (비회원용)
    @PostMapping("/check")
    public ResponseEntity<GuestInquiryDTO> checkInquiry(@RequestBody GuestInquiryDTO dto) {
        return ResponseEntity.ok(guestInquiryService.getByNameAndPassword(
            dto.getGuestName(), 
            dto.getPassword()
        ));
    }

    // 전체 문의 조회 (관리자/매니저용)
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<GuestInquiryDTO>> getAllInquiries() {
        return ResponseEntity.ok(guestInquiryService.getAllInquiries());
    }

    // 전체 문의 조회 (비공개)
    @GetMapping("/all")
    public ResponseEntity<List<GuestInquiryListDTO>> getAllInquiriesPublic() {
        List<GuestInquiryListDTO> inquiries = guestInquiryService.getAllInquiriesPublic();
        return ResponseEntity.ok(inquiries);
    }

    // 답변 등록 (관리자/매니저용)
    @PutMapping("/{id}/answer")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<GuestInquiryDTO> answer(
            @PathVariable Long id,
            @RequestBody String answer) {
        return ResponseEntity.ok(guestInquiryService.answer(id, answer));
    }

    // 관리자용 전체 문의 목록 조회 (상세 내용 포함)
    @GetMapping("/admin")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<GuestInquiryDTO>> getAdminInquiries() {
        return ResponseEntity.ok(guestInquiryService.getAllInquiriesForAdmin());
    }

    // 관리자용 단일 문의 상세 조회
    @GetMapping("/admin/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<GuestInquiryDTO> getAdminInquiry(@PathVariable Long id) {
        return ResponseEntity.ok(guestInquiryService.getInquiryForAdmin(id));
    }
} 