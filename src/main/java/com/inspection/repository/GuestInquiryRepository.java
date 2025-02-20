package com.inspection.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.inspection.entity.GuestInquiry;

public interface GuestInquiryRepository extends JpaRepository<GuestInquiry, Long> {
    // 이름으로만 조회
    GuestInquiry findByGuestName(String guestName);
} 