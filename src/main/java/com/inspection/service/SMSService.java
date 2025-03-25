package com.inspection.service;

import java.util.Hashtable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.*;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.inspection.entity.ContractParticipant;

@Service
@RequiredArgsConstructor
@Slf4j
public class SMSService {
    
    @Value("${eon.sender-phone}")
    private String senderPhone;

    @Value("${eon.license-key}")
    private String licenseKey;
    
    @Value("${frontend.base-url}")
    private String frontendBaseUrl;
    
    private final ParticipantTokenService participantTokenService;

    public String sendSMS(String name, String phone, String content, String link) {
        try {
            Hashtable<String, String> param = new Hashtable<>();
            
            param.put("STYPE", "1");
            param.put("RESERVETIME", "");
            param.put("SENDPHONE", senderPhone);
            param.put("DESTPHONE", phone);
            
            String baseUrl = frontendBaseUrl.replace("http://", "").replace("https://", "");
            String domainUrl = String.format("%s/contract-sign/%s", baseUrl, link);
            
            String message = String.format(
                "[Web발신]\n" +
                "[타이어 뱅크]\n" +
                "안녕하세요, %s님\n" +
                "계약서 서명이 요청되었습니다.\n\n" +
                "본 링크는 24시간 동안 유효합니다.\n" +
                "문의사항: 1599-7181\n\n" +
                "▣ 계약서 서명하기: http://%s",
                name,
                domainUrl
            );
            param.put("MSG", new String(message.getBytes("KSC5601"), "8859_1"));

            String response = sendEonRequest(
                "http://blue3.eonmail.co.kr:8081/weom/servlet/api.EONASP6", 
                param
            );
            
            log.info("SMS 발송 결과: {}", response);
            return response;

        } catch (Exception e) {
            log.error("SMS 발송 실패: ", e);
            throw new RuntimeException("SMS 발송에 실패했습니다.", e);
        }
    }

    private String sendEonRequest(String urlStr, Hashtable<String, String> params) throws Exception {
        StringBuilder postData = new StringBuilder();
        for (String key : params.keySet()) {
            if (postData.length() != 0) postData.append('&');
            postData.append(key).append('=').append(params.get(key));
        }

        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        try {
            byte[] postDataBytes = postData.toString().getBytes("8859_1");

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
            conn.setRequestProperty("eon_licencekey", licenseKey);
            conn.setDoOutput(true);
            conn.getOutputStream().write(postDataBytes);

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "KSC5601"));
            StringBuilder response = new StringBuilder();
            String line;
            
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            
            in.close();
            return response.toString();
            
        } finally {
            conn.disconnect();
        }
    }

    /**
     * 계약 참여자에게 토큰이 포함된 서명 요청 SMS를 발송합니다.
     * 
     * @param participant 서명 참여자
     * @param baseUrl 서명 페이지 기본 URL (예: http://example.com)
     * @param contractTitle 계약 제목 (선택적)
     * @return 생성된 토큰
     */
    public String sendSignatureSMS(ContractParticipant participant, String baseUrl, String contractTitle) {
        try {
            // 1. 참여자 ID로 토큰 생성
            String token = participantTokenService.generateParticipantToken(participant.getId());
            
            // 2. 토큰을 포함한 서명 링크 생성
            String signatureLink = baseUrl + "/contract-sign?token=" + token;
            
            Hashtable<String, String> param = new Hashtable<>();
            
            param.put("STYPE", "1");
            param.put("RESERVETIME", "");
            param.put("SENDPHONE", senderPhone);
            param.put("DESTPHONE", participant.getDecryptedPhoneNumber());
            
            // 3. SMS 메시지 생성 (토큰 포함 링크)
            String message = String.format(
                "[Web발신]\n" +
                "[타이어 뱅크]\n" +
                "안녕하세요, %s님\n" +
                "%s 계약서 서명이 요청되었습니다.\n\n" +
                "본 링크는 24시간 동안 유효합니다.\n" +
                "문의사항: 1599-7181\n\n" +
                "▣ 계약서 서명하기: %s",
                participant.getName(),
                contractTitle != null ? contractTitle : "",
                signatureLink
            );
            
            param.put("MSG", new String(message.getBytes("KSC5601"), "8859_1"));

            // 4. SMS 발송
            String response = sendEonRequest(
                "http://blue3.eonmail.co.kr:8081/weom/servlet/api.EONASP6", 
                param
            );
            
            log.info("서명 요청 SMS 발송 성공 - 참여자: {}, 계약: {}", participant.getName(), contractTitle);
            return token;

        } catch (Exception e) {
            log.error("서명 요청 SMS 발송 실패: ", e);
            throw new RuntimeException("서명 요청 SMS 발송에 실패했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 서명 완료 후 계약 참여자에게 장기 보관용 토큰이 포함된 결과 페이지 링크를 SMS로 발송합니다.
     * 
     * @param participant 계약 참여자 객체
     * @param signedContractUrl 서명된 계약 조회 URL (토큰 포함)
     * @param contractTitle 계약 제목
     */
    public void sendContractCompletionSMS(ContractParticipant participant, String signedContractUrl, String contractTitle) {
        try {
            // 휴대폰 번호가 없는 경우 처리 중단
            if (participant.getDecryptedPhoneNumber() == null || participant.getDecryptedPhoneNumber().trim().isEmpty()) {
                log.warn("참여자 휴대폰 번호 없음 (ID: {}) - 계약 완료 SMS 전송 건너뜀", participant.getId());
                return;
            }
            
            Hashtable<String, String> param = new Hashtable<>();
            
            param.put("STYPE", "1");
            param.put("RESERVETIME", "");
            param.put("SENDPHONE", senderPhone);
            param.put("DESTPHONE", participant.getDecryptedPhoneNumber());
            
            // SMS 메시지 생성 (장기 토큰 포함 링크)
            String message = String.format(
                "[Web발신]\n" +
                "[타이어 뱅크]\n" +
                "안녕하세요, %s님\n" +
                "%s 계약서 서명이 완료되었습니다.\n\n" +
                "아래 링크로 계약서를 확인하실 수 있습니다.\n" +
                "※ 본 링크는 2년간 유효합니다.\n" +
                "문의사항: 1599-7181\n\n" +
                "▣ 서명 완료된 계약서: %s",
                participant.getName(),
                contractTitle != null ? contractTitle : "",
                signedContractUrl
            );
            
            param.put("MSG", new String(message.getBytes("KSC5601"), "8859_1"));

            // SMS 발송
            String response = sendEonRequest(
                "http://blue3.eonmail.co.kr:8081/weom/servlet/api.EONASP6", 
                param
            );
            
            log.info("계약 완료 SMS 발송 성공 - 참여자: {}, 계약: {}", participant.getName(), contractTitle);
        } catch (Exception e) {
            log.error("계약 완료 SMS 발송 실패: {}", e.getMessage(), e);
            // SMS 실패는 Critical 오류가 아니므로 예외를 던지지 않고 로그만 기록
        }
    }
} 