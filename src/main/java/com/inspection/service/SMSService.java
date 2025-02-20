package com.inspection.service;

import java.util.Hashtable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.*;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SMSService {
    
    private static final String ADMIN_PHONE = "01093574445";
    
    @Value("${eon.sender-phone}")
    private String senderPhone;

    @Value("${eon.license-key}")
    private String licenseKey;

    public String sendSMS(String name, String phone, String content) {
        try {
            Hashtable<String, String> param = new Hashtable<>();
            
            param.put("STYPE", "1");
            param.put("RESERVETIME", "");
            param.put("SENDPHONE", senderPhone);
            param.put("DESTPHONE", ADMIN_PHONE);
            
            String message = String.format(
                "[새로운 문의]\n이름: %s\n연락처: %s\n내용: %s",
                name, phone, content
            );
            param.put("MSG", new String(message.getBytes("KSC5601"), "8859_1"));  // 한글 인코딩 수정

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
} 