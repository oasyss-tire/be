package com.inspection.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.inspection.dto.ChatbotRequest;
import com.inspection.dto.ChatbotResponse;
import com.inspection.dto.QAPairDTO;
import com.inspection.service.ChatbotService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatbotController {
    private final ChatbotService chatbotService;
    private static final Logger log = LoggerFactory.getLogger(ChatbotController.class);
    
    @PostMapping
    public ChatbotResponse chat(
        @RequestBody ChatbotRequest request,
        @AuthenticationPrincipal UserDetails userDetails  // 현재 인증된 사용자 정보
    ) {
        // 사용자 ID 설정 (로그인한 경우 해당 ID, 아닌 경우 'guest')
        String userId = (userDetails != null) ? 
            userDetails.getUsername() : "guest";
            
        log.info("Chat request from user: {}", userId);
        return chatbotService.chat(request.getQuestion(), userId);
    }
    
    @GetMapping("/qa")
    public ResponseEntity<List<QAPairDTO>> getQAList() {
        return ResponseEntity.ok(chatbotService.getQAList());
    }
    
    @PostMapping("/qa")
    public ResponseEntity<Void> addQAPair(@RequestBody QAPairDTO qaPair) {
        chatbotService.addQAPair(qaPair);
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/qa/{index}")
    public ResponseEntity<Void> deleteQAPair(@PathVariable int index) {
        chatbotService.deleteQAPair(index);
        return ResponseEntity.ok().build();
    }
} 