package com.inspection.service;

import org.springframework.stereotype.Service;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.util.Hashtable;
import java.util.Enumeration;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import java.io.DataOutputStream;
import java.io.BufferedOutputStream;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.inspection.entity.KakaoAlert;
import com.inspection.repository.KakaoAlertRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import com.inspection.dto.KakaoAlertDTO;
import com.inspection.repository.UserRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class KakaoAlertService {
    
    private final KakaoAlertRepository kakaoAlertRepository;
    private final UserRepository userRepository;
    
    @Value("${eon.license-key}")
    private String licenseKey;
    
    @Value("${eon.sender-phone}")
    private String senderPhone;
    
    public void sendAlert(String phoneNumber, String message, Long userId) {
        try {
            Hashtable<String, String> param = new Hashtable<>();
            param.put("STYPE", "4");                // 알림톡
            param.put("RESERVETIME", "");           
            param.put("SENDPHONE", senderPhone);    
            param.put("DESTPHONE", phoneNumber);    
            param.put("SUBJECT", "점검 결과 안내 보고서");   
            
            // 템플릿 내용과 정확히 일치
            param.put("MSG", 
                "[점검 결과 보고서]\n" +
                "안녕하세요. 고객님,\n" +
                "고객님의 사업장에\n" +
                "안전 점검이 완료되었습니다.\n" +
                "\n" +
                "▣ 점검결과 상세보기"
            );              
            
            // 알림톡 템플릿 코드
            param.put("TALK_TEMPLAT", "OAAS_REP_002");
            
            // 버튼 설정
            param.put("TALK_BTN1_NAME", "점검보고서");    
            param.put("TALK_BTN1_URL", String.format("https://%s", message));
            
            // multipart/form-data 대신 일반 POST 요청 사용
            String response = sendEonRequest("http://blue3.eonmail.co.kr:8081/weom/servlet/api.EONASP6", param);
            log.info("EON API Response: {}", response);
            
            // CPID 추출 및 DB 저장
            if (response.contains("RESULTCODE")) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(response);
                if (root.get("RESULTCODE").asText().equals("200")) {
                    String cpId = root.get("CPID").asText();
                    
                    // 알림톡 이력 저장
                    KakaoAlert alert = new KakaoAlert();
                    alert.setUserId(userId);
                    alert.setReceiverPhone(phoneNumber);
                    alert.setMessage(message);
                    alert.setCpId(cpId);
                    alert.setSentAt(LocalDateTime.now());
                    
                    kakaoAlertRepository.save(alert);
                }
            }
        } catch (Exception e) {
            log.error("Failed to send alert: ", e);
            throw new RuntimeException("알림톡 전송 실패: " + e.getMessage(), e);
        }
    }

    private String sendEonRequest(String webUrl, Hashtable<String, String> param) throws Exception {
        HttpURLConnection con = null;
        InputStream urlin = null;
        ByteArrayOutputStream tmpVector = null;

        try {
            URL url = new URL(webUrl);
            con = (HttpURLConnection) url.openConnection();
            con.setReadTimeout(60000);
            con.setUseCaches(false);
            con.setRequestMethod("POST");
            con.setRequestProperty("eon_licencekey", licenseKey);
            con.setDoOutput(true);

            // 파라미터 인코딩
            StringBuilder paramQuery = new StringBuilder();
            Enumeration<String> paramKeys = param.keys();
            while (paramKeys.hasMoreElements()) {
                String key = paramKeys.nextElement();
                String value = param.get(key);
                if (paramQuery.length() > 0) {
                    paramQuery.append("&");
                }
                paramQuery.append(key).append("=").append(URLEncoder.encode(value, "EUC-KR"));
            }

            // 요청 데이터 전송
            try (PrintWriter writer = new PrintWriter(con.getOutputStream())) {
                writer.print(paramQuery.toString());
            }

            // 응답 확인
            int responseCode = con.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new RuntimeException("API 호출 실패: " + responseCode);
            }

            // 응답 데이터 읽기
            urlin = con.getInputStream();
            tmpVector = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = urlin.read(buffer)) != -1) {
                tmpVector.write(buffer, 0, bytesRead);
            }

            return tmpVector.toString("EUC-KR");

        } finally {
            if (urlin != null) try { urlin.close(); } catch (Exception ignored) {}
            if (con != null) try { con.disconnect(); } catch (Exception ignored) {}
        }
    }

    // multipart/form-data로 요청하는 메서드 추가
    private String sendEonRequestMultipart(String webUrl, Hashtable<String, String> param) throws Exception {
        HttpURLConnection con = null;
        InputStream urlin = null;
        ByteArrayOutputStream tmpVector = null;
        DataOutputStream out = null;

        try {
            String boundary = "_nextpart_" + System.currentTimeMillis();
            
            URL url = new URL(webUrl);
            con = (HttpURLConnection) url.openConnection();
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("POST");
            con.setRequestProperty("Connection", "Keep-Alive");
            con.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
            con.setRequestProperty("eon_licencekey", licenseKey);

            // 데이터 전송
            out = new DataOutputStream(new BufferedOutputStream(con.getOutputStream()));
            
            // 파라미터 전송
            for (Enumeration<String> keys = param.keys(); keys.hasMoreElements();) {
                String key = keys.nextElement();
                String value = param.get(key);
                
                out.writeBytes("--" + boundary + "\r\n");
                out.writeBytes("Content-Disposition: form-data; name=\"" + key + "\"\r\n\r\n");
                out.writeBytes(value + "\r\n");
            }
            out.writeBytes("--" + boundary + "--\r\n");
            out.flush();

            // 응답 읽기
            urlin = con.getInputStream();
            tmpVector = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = urlin.read(buffer)) != -1) {
                tmpVector.write(buffer, 0, bytesRead);
            }

            return tmpVector.toString("EUC-KR");
        } finally {
            if (out != null) try { out.close(); } catch (Exception ignored) {}
            if (urlin != null) try { urlin.close(); } catch (Exception ignored) {}
            if (con != null) try { con.disconnect(); } catch (Exception ignored) {}
        }
    }

    public List<KakaoAlertDTO> getAllAlerts() {
        return kakaoAlertRepository.findAll().stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    public KakaoAlertDTO getAlertById(Long alertId) {
        KakaoAlert alert = kakaoAlertRepository.findById(alertId)
            .orElseThrow(() -> new RuntimeException("알림톡 이력을 찾을 수 없습니다. ID: " + alertId));
        return convertToDTO(alert);
    }

    private KakaoAlertDTO convertToDTO(KakaoAlert alert) {
        KakaoAlertDTO dto = new KakaoAlertDTO();
        dto.setId(alert.getId());
        dto.setUserId(alert.getUserId());
        
        // userId로 사용자 정보 조회
        userRepository.findById(alert.getUserId())
            .ifPresent(user -> dto.setUsername(user.getUsername()));
            
        dto.setReceiverPhone(alert.getReceiverPhone());
        dto.setMessage(alert.getMessage());
        dto.setCpId(alert.getCpId());
        dto.setSentAt(alert.getSentAt());
        return dto;
    }
} 