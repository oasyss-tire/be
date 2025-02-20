package com.inspection.controller;

import org.springframework.web.bind.annotation.*;
import com.inspection.service.FireCheckInspectionService;
import com.inspection.dto.FireCheckInspectionDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import java.util.List;

@RestController
@RequestMapping("/api/fire-check")
@RequiredArgsConstructor
public class FireCheckInspectionController {

    private final FireCheckInspectionService fireCheckInspectionService;
    
    // Create
    @PostMapping
    public ResponseEntity<FireCheckInspectionDTO> createInspection(@RequestBody FireCheckInspectionDTO dto) {
        FireCheckInspectionDTO created = fireCheckInspectionService.createInspection(dto);
        return ResponseEntity.ok(created);
    }
    
    // Read
    @GetMapping("/{id}")
    public ResponseEntity<FireCheckInspectionDTO> getInspection(@PathVariable Long id) {
        FireCheckInspectionDTO dto = fireCheckInspectionService.getInspection(id);
        return ResponseEntity.ok(dto);
    }
    
    // Read All
    @GetMapping
    public ResponseEntity<List<FireCheckInspectionDTO>> getAllInspections() {
        List<FireCheckInspectionDTO> inspections = fireCheckInspectionService.getAllInspections();
        return ResponseEntity.ok(inspections);
    }
    
    // Update
    @PutMapping("/{id}")
    public ResponseEntity<FireCheckInspectionDTO> updateInspection(
            @PathVariable Long id,
            @RequestBody FireCheckInspectionDTO dto) {
        FireCheckInspectionDTO updated = fireCheckInspectionService.updateInspection(id, dto);
        return ResponseEntity.ok(updated);
    }
    
    // Delete
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInspection(@PathVariable Long id) {
        fireCheckInspectionService.deleteInspection(id);
        return ResponseEntity.ok().build();
    }
} 