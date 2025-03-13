package com.inspection.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import com.inspection.entity.Inquiry;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {
    @Query("SELECT i FROM Inquiry i JOIN FETCH i.writer w ORDER BY i.createdAt DESC")
    List<Inquiry> findAllWithWriter();
} 