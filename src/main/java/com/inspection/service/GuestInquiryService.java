package com.inspection.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import com.inspection.entity.GuestInquiry;
import com.inspection.dto.GuestInquiryDTO;
import com.inspection.dto.GuestInquiryListDTO;
import com.inspection.repository.GuestInquiryRepository;

@Service
@RequiredArgsConstructor
public class GuestInquiryService {
    private final GuestInquiryRepository guestInquiryRepository;
    private final PasswordEncoder passwordEncoder;

    // 문의 등록
    public GuestInquiryDTO create(GuestInquiryDTO dto) {
        GuestInquiry inquiry = new GuestInquiry();
        inquiry.setGuestName(dto.getGuestName());
        inquiry.setPhoneNumber(dto.getPhoneNumber());
        inquiry.setPassword(passwordEncoder.encode(dto.getPassword()));  // 비밀번호 암호화
        inquiry.setContent(dto.getContent());
        
        return convertToDTO(guestInquiryRepository.save(inquiry));
    }

    // 문의 조회 (비밀번호 확인)
    public GuestInquiryDTO getByNameAndPassword(String name, String password) {
        GuestInquiry inquiry = guestInquiryRepository.findByGuestName(name);
        if (inquiry == null || !passwordEncoder.matches(password, inquiry.getPassword())) {
            throw new RuntimeException("문의를 찾을 수 없거나 비밀번호가 일치하지 않습니다.");
        }
        return convertToDTO(inquiry);
    }

    // 관리자용 답변 등록
    public GuestInquiryDTO answer(Long id, String answer) {
        GuestInquiry inquiry = guestInquiryRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("문의를 찾을 수 없습니다."));
        
        inquiry.setAnswer(answer);
        inquiry.setAnswered(true);
        inquiry.setAnswerTime(LocalDateTime.now());
        
        return convertToDTO(guestInquiryRepository.save(inquiry));
    }

    // 관리자용 전체 조회
    public List<GuestInquiryDTO> getAllInquiries() {
        return guestInquiryRepository.findAll().stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    public List<GuestInquiryListDTO> getAllInquiriesPublic() {
        return guestInquiryRepository.findAll().stream()
            .map(this::convertToListDTO)
            .collect(Collectors.toList());
    }

    // 관리자용 전체 문의 조회 (모든 정보 포함)
    public List<GuestInquiryDTO> getAllInquiriesForAdmin() {
        return guestInquiryRepository.findAll().stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    // 관리자용 단일 문의 조회
    public GuestInquiryDTO getInquiryForAdmin(Long id) {
        GuestInquiry inquiry = guestInquiryRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("문의를 찾을 수 없습니다."));
        return convertToDTO(inquiry);
    }

    private GuestInquiryDTO convertToDTO(GuestInquiry inquiry) {
        GuestInquiryDTO dto = new GuestInquiryDTO();
        dto.setId(inquiry.getId());
        dto.setGuestName(inquiry.getGuestName());
        dto.setPhoneNumber(inquiry.getPhoneNumber());
        dto.setContent(inquiry.getContent());
        dto.setAnswer(inquiry.getAnswer());
        dto.setAnswerTime(inquiry.getAnswerTime());
        dto.setAnswered(inquiry.getAnswered());
        dto.setCreatedAt(inquiry.getCreatedAt());
        return dto;
    }

    private GuestInquiryListDTO convertToListDTO(GuestInquiry inquiry) {
        GuestInquiryListDTO dto = new GuestInquiryListDTO();
        dto.setId(inquiry.getId());
        dto.setGuestName(inquiry.getGuestName());
        dto.setContent(inquiry.getContent().length() > 30 
            ? inquiry.getContent().substring(0, 30) + "..." 
            : inquiry.getContent());
        dto.setAnswered(inquiry.getAnswered());
        dto.setCreatedAt(inquiry.getCreatedAt());
        return dto;
    }
} 