package com.inspection.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.inspection.entity.Notice;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    List<Notice> findAllByOrderByCreatedAtDesc();
    
    @Query("SELECT n FROM Notice n WHERE n.popup = true " +
           "AND n.popupStartDate <= :now " +
           "AND n.popupEndDate >= :now")
    List<Notice> findActivePopups(LocalDateTime now);
} 