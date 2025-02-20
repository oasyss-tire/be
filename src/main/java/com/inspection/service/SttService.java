package com.inspection.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Slf4j
@Service
public class SttService {

    // application.yml에 정의된 값을 읽어옴
    @Value("${clova.speech.secret}")
    private String secretKey;

    @Value("${clova.speech.invoke-url}")
    private String apiUrl;

    private final OkHttpClient client = new OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build();
        
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String convertSpeechToText(MultipartFile audioFile) throws IOException {
        log.info("STT 변환 시작. 파일 크기: {}, ContentType: {}", 
            audioFile.getSize(), 
            audioFile.getContentType());

        // 헤더 정보 로깅 추가
        log.info("API Key: {}", secretKey);
        log.info("API URL: {}", apiUrl);

        // Clova Speech API 요청 파라미터 설정
        Map<String, Object> params = new HashMap<>();
        params.put("language", "ko-KR");
        params.put("completion", "sync");
        params.put("sampleRate", 16000);
        params.put("encoding", "wav");

        Map<String, Object> audioConfig = new HashMap<>();
        audioConfig.put("channel", 1);
        audioConfig.put("sampleRate", 16000);
        audioConfig.put("format", "wav");
        audioConfig.put("bitDepth", 16);
        params.put("audio", audioConfig);

        String jsonParams = objectMapper.writeValueAsString(params);
        log.info("API 요청 파라미터: {}", jsonParams);

        // multipart/form-data 요청 생성
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("params", jsonParams)
                .addFormDataPart("media", audioFile.getOriginalFilename(),
                        RequestBody.create(MediaType.parse("audio/wav"), audioFile.getBytes()))
                .build();

        String fullUrl = apiUrl + "/recognizer/upload";
        log.info("요청 URL: {}", fullUrl);

        Request request = new Request.Builder()
                .url(fullUrl)
                .addHeader("X-CLOVASPEECH-API-KEY", secretKey)
                .post(requestBody)
                .build();

        // 요청 헤더 로깅
        log.info("Request Headers: {}", request.headers());

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            log.info("Response Code: {}", response.code());
            log.info("Response Headers: {}", response.headers());
            log.info("Clova STT 응답: {}", responseBody);
            
            if (!response.isSuccessful()) {
                throw new IOException("API 호출 실패: " + responseBody);
            }

            return objectMapper.readTree(responseBody)
                    .path("text")
                    .asText();
        }
    }
} 