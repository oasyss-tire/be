package com.inspection.service;

import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.inspection.dto.ChatbotRequest;
import com.inspection.dto.ChatbotResponse;
import com.inspection.dto.QAPairDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatbotService {
    private final WebClient webClient;
    
    public ChatbotResponse chat(String question, String userId) {
        ChatbotRequest request = new ChatbotRequest(question, userId);
        
        try {
            log.info("Sending request to FastAPI: {}", request);
            
            ChatbotResponse response = webClient
                .post()
                .uri("/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(request))
                .retrieve()
                .onStatus(
                    status -> status.is5xxServerError(),
                    clientResponse -> {
                        log.error("Server error: {}", clientResponse.statusCode());
                        return Mono.error(new RuntimeException("FastAPI 서버 오류"));
                    }
                )
                .bodyToMono(ChatbotResponse.class)
                .block();
                
            log.info("Received response: {}", response);
            return response;
            
        } catch (Exception e) {
            log.error("AI 서비스 호출 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("AI 서비스 연동 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    // QA 목록 조회
    public List<QAPairDTO> getQAList() {
        try {
            log.info("Requesting QA list from FastAPI");
            List<QAPairDTO> result = webClient
                .get()
                .uri("/qa/list")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<QAPairDTO>>() {})
                .block();
            log.info("Received QA list: {}", result);
            return result;
        } catch (Exception e) {
            log.error("QA 목록 조회 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("QA 목록 조회 중 오류가 발생했습니다.");
        }
    }
    
    // QA 추가
    public void addQAPair(QAPairDTO qaPair) {
        try {
            webClient
                .post()
                .uri("/qa/add")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(qaPair))
                .retrieve()
                .bodyToMono(Void.class)
                .block();
        } catch (Exception e) {
            log.error("QA 추가 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("QA 추가 중 오류가 발생했습니다.");
        }
    }
    
    // QA 삭제
    public void deleteQAPair(int index) {
        try {
            webClient
                .delete()
                .uri("/qa/{index}", index)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
        } catch (Exception e) {
            log.error("QA 삭제 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("QA 삭제 중 오류가 발생했습니다.");
        }
    }
}