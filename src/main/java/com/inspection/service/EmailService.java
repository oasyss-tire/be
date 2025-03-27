package com.inspection.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.web.multipart.MultipartFile;

import com.inspection.entity.ContractParticipant;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {
    
    private final JavaMailSender emailSender;
    private final ParticipantTokenService participantTokenService;
    
    @Value("${spring.mail.username}")
    private String fromEmail;

    // 이메일 공통 푸터 HTML
    private String getEmailFooter() {
        return "<hr style='margin-top: 30px; border: 0; border-top: 1px solid #eee;'>" +
               "<footer style='margin-top: 20px; color: #777; font-size: 12px;'>" +
               "<p style='margin: 5px 0;'>타이어뱅크 (본점)</p>" +
               "<p style='margin: 5px 0;'>세종 한누리대로 350 8층 | 30121</p>" +
               "<p style='margin: 5px 0;'>문의전화: 1599-7181</p>" +
               "</footer>";
    }

    public void sendSimpleMessage(String to, String subject, String text) {
        try {
            log.info("이메일 전송 시도 - 수신자: {}, 제목: {}", to, subject);
            
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, false);
            
            emailSender.send(message);
            log.info("이메일 전송 성공");
        } catch (Exception e) {
            log.error("이메일 전송 실패: {}", e.getMessage(), e);
            throw new RuntimeException("이메일 전송 실패: " + e.getMessage(), e);
        }
    }

    public void sendHtmlMessage(String to, String subject, String htmlContent) {
        try {
            log.info("HTML 이메일 전송 시도 - 수신자: {}, 제목: {}", to, subject);
            
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            
            emailSender.send(message);
            log.info("HTML 이메일 전송 성공");
        } catch (Exception e) {
            log.error("HTML 이메일 전송 실패: {}", e.getMessage(), e);
            throw new RuntimeException("이메일 전송 실패: " + e.getMessage(), e);
        }
    }

    public void sendMessageWithAttachment(
            String to, 
            String subject, 
            String content,
            MultipartFile attachment) {
        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);  // HTML 지원
            
            if (attachment != null && !attachment.isEmpty()) {
                helper.addAttachment(
                    attachment.getOriginalFilename(),
                    new ByteArrayResource(attachment.getBytes())
                );
            }
            
            emailSender.send(message);
            log.info("첨부파일 이메일 전송 성공 - 파일명: {}", attachment.getOriginalFilename());
        } catch (Exception e) {
            log.error("첨부파일 이메일 전송 실패", e);
            throw new RuntimeException("이메일 전송 실패: " + e.getMessage(), e);
        }
    }

    
    /**
     * 계약 참여자 ID, 이메일, 이름, 계약 제목을 받아 토큰이 포함된 서명 요청 이메일을 발송합니다.
     * 프론트엔드에서 직접 호출할 수 있는 형태의 메서드입니다.
     * 
     * @param participantId 참여자 ID
     * @param email 참여자 이메일
     * @param name 참여자 이름
     * @param contractTitle 계약 제목
     * @param baseUrl 서명 페이지 기본 URL
     * @return 생성된 토큰
     */
    public String sendSignatureRequestWithToken(
            Long participantId, 
            String email, 
            String name,
            String contractTitle, 
            String baseUrl) {
        try {
            // 1. 참여자 ID로 토큰 생성
            String token = participantTokenService.generateParticipantToken(participantId);
            
            // 2. 토큰을 포함한 서명 링크 생성
            String signatureLink = baseUrl + "/contract-sign?token=" + token;
            
            // 3. 이메일 제목
            String subject = "[타이어뱅크] " + contractTitle + " - 계약서 서명 요청";
            
            // 4. HTML 이메일 본문 생성
            String htmlContent = "<html><body style='font-family: Arial, sans-serif;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd;'>" +
                "<h2 style='color: #333;'>계약서 서명 요청</h2>" +
                "<p>안녕하세요, <strong>" + name + "</strong>님</p>" +
                "<p>" + contractTitle + " 계약서의 서명이 요청되었습니다.</p>" +
                "<p>아래 링크를 클릭하여 서명을 진행해주세요.</p>" +
                "<p><strong>※ 본 링크는 24시간 동안 유효합니다.</strong></p>" +
                "<div style='margin: 30px 0;'>" +
                "<a href='" + signatureLink + "' style='background-color: #3182F6; color: white; padding: 12px 20px; " +
                "text-decoration: none; border-radius: 4px; font-weight: bold;'>계약서 서명하기</a>" +
                "</div>" +
                "<p>문의사항은 1599-7181로 연락주시기 바랍니다.</p>" +
                "<p>감사합니다.<br>타이어뱅크 드림</p>" +
                getEmailFooter() +
                "</div></body></html>";
            
            // 5. 이메일 전송
            sendHtmlMessage(email, subject, htmlContent);
            log.info("서명 요청 이메일 발송 성공 - 참여자ID: {}, 수신자: {}, 계약: {}", participantId, email, contractTitle);
            
            return token;
        } catch (Exception e) {
            log.error("서명 요청 이메일 발송 실패: {}", e.getMessage(), e);
            throw new RuntimeException("서명 요청 이메일 발송 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 재서명 완료 후 계약 참여자에게 장기 보관용 토큰이 포함된 결과 페이지 링크를 이메일로 발송합니다.
     * 이전 토큰이 무효화되었다는 안내 문구가 포함됩니다.
     * 
     * @param participant 계약 참여자 객체
     * @param signedContractUrl 서명된 계약 조회 URL (토큰 포함)
     * @param contractTitle 계약 제목
     */
    public void sendResignCompletionEmail(ContractParticipant participant, String signedContractUrl, String contractTitle) {
        try {
            // 이메일 주소가 없는 경우 처리 중단
            if (participant.getEmail() == null || participant.getEmail().trim().isEmpty()) {
                log.warn("참여자 이메일 없음 (ID: {}) - 재서명 완료 이메일 전송 건너뜀", participant.getId());
                return;
            }
            
            // 이메일 제목
            String subject = "[타이어뱅크] " + contractTitle + " - 계약서 재서명이 완료되었습니다";
            
            // HTML 이메일 본문 생성
            String htmlContent = "<html><body style='font-family: Arial, sans-serif;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd;'>" +
                "<h2 style='color: #333;'>계약서 재서명 완료</h2>" +
                "<p>안녕하세요, <strong>" + participant.getName() + "</strong>님</p>" +
                "<p><strong>" + contractTitle + "</strong> 계약서의 재서명이 완료되었습니다.</p>" +
                "<p>아래 링크를 통해 언제든지 계약 내용을 확인하실 수 있습니다.</p>" +
                "<p><strong style='color: #ff6600;'>※ 이전에 받은 계약서 열람 링크는 더 이상 유효하지 않으니 아래 새 링크를 사용해주세요.</strong></p>" +
                "<p><strong>※ 본 링크는 2년간 유효합니다. 필요시 저장해두세요.</strong></p>" +
                "<div style='margin: 30px 0;'>" +
                "<a href='" + signedContractUrl + "' style='background-color: #3182F6; color: white; padding: 12px 20px; " +
                "text-decoration: none; border-radius: 4px; font-weight: bold;'>재서명 완료된 계약서 보기</a>" +
                "</div>" +
                "<p>문의사항은 1599-7181로 연락주시기 바랍니다.</p>" +
                "<p>감사합니다.<br>타이어뱅크 드림</p>" +
                getEmailFooter() +
                "</div></body></html>";
            
            // 이메일 전송
            sendHtmlMessage(participant.getEmail(), subject, htmlContent);
            log.info("재서명 완료 이메일 발송 성공 - 참여자ID: {}, 수신자: {}, 계약: {}", 
                    participant.getId(), participant.getEmail(), contractTitle);
            
        } catch (Exception e) {
            log.error("재서명 완료 이메일 발송 실패: {}", e.getMessage(), e);
            // 이메일 실패는 Critical 오류가 아니므로 예외를 던지지 않고 로그만 기록
        }
    }
    
    /**
     * 서명 완료 후 계약 참여자에게 장기 보관용 토큰이 포함된 결과 페이지 링크를 이메일로 발송합니다.
     * 
     * @param participant 계약 참여자 객체
     * @param signedContractUrl 서명된 계약 조회 URL (토큰 포함)
     * @param contractTitle 계약 제목
     */
    public void sendContractCompletionEmail(ContractParticipant participant, String signedContractUrl, String contractTitle) {
        try {
            // 이메일 주소가 없는 경우 처리 중단
            if (participant.getEmail() == null || participant.getEmail().trim().isEmpty()) {
                log.warn("참여자 이메일 없음 (ID: {}) - 계약 완료 이메일 전송 건너뜀", participant.getId());
                return;
            }
            
            // 이메일 제목
            String subject = "[타이어뱅크] " + contractTitle + " - 계약서 서명이 완료되었습니다";
            
            // HTML 이메일 본문 생성
            String htmlContent = "<html><body style='font-family: Arial, sans-serif;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd;'>" +
                "<h2 style='color: #333;'>계약서 서명 완료</h2>" +
                "<p>안녕하세요, <strong>" + participant.getName() + "</strong>님</p>" +
                "<p><strong>" + contractTitle + "</strong> 계약서의 서명이 완료되었습니다.</p>" +
                "<p>아래 링크를 통해 언제든지 계약 내용을 확인하실 수 있습니다.</p>" +
                "<p><strong>※ 이전에 받은 계약서 열람 링크는 더 이상 유효하지 않으니 이 링크를 사용해주세요.</strong></p>" +
                "<p><strong>※ 본 링크는 2년간 유효합니다. 필요시 저장해두세요.</strong></p>" +
                "<div style='margin: 30px 0;'>" +
                "<a href='" + signedContractUrl + "' style='background-color: #3182F6; color: white; padding: 12px 20px; " +
                "text-decoration: none; border-radius: 4px; font-weight: bold;'>서명 완료된 계약서 보기</a>" +
                "</div>" +
                "<p>문의사항은 1599-7181로 연락주시기 바랍니다.</p>" +
                "<p>감사합니다.<br>타이어뱅크 드림</p>" +
                getEmailFooter() +
                "</div></body></html>";
            
            // 이메일 전송
            sendHtmlMessage(participant.getEmail(), subject, htmlContent);
            log.info("계약 완료 이메일 발송 성공 - 참여자ID: {}, 수신자: {}, 계약: {}", 
                    participant.getId(), participant.getEmail(), contractTitle);
            
        } catch (Exception e) {
            log.error("계약 완료 이메일 발송 실패: {}", e.getMessage(), e);
            // 이메일 실패는 Critical 오류가 아니므로 예외를 던지지 않고 로그만 기록
        }
    }

    /**
     * 재서명 요청을 위한 이메일을 발송합니다.
     * 이 메서드는 참여자가 재서명 요청을 승인받았을 때 호출됩니다.
     * 
     * @param participantId 참여자 ID
     * @param email 참여자 이메일
     * @param name 참여자 이름
     * @param contractTitle 계약 제목
     * @param baseUrl 서명 페이지 기본 URL
     * @return 생성된 토큰
     */
    public String sendResignRequestEmail(
            Long participantId, 
            String email, 
            String name,
            String contractTitle, 
            String baseUrl) {
        try {
            // 1. 참여자 ID로 토큰 생성
            String token = participantTokenService.generateParticipantToken(participantId);
            
            // 2. 토큰을 포함한 서명 링크 생성
            String signatureLink = baseUrl + "/contract-sign?token=" + token;
            
            // 3. 이메일 제목
            String subject = "[타이어뱅크] " + contractTitle + " - 계약서 재서명 요청";
            
            // 4. HTML 이메일 본문 생성
            String htmlContent = "<html><body style='font-family: Arial, sans-serif;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd;'>" +
                "<h2 style='color: #333;'>계약서 재서명 요청</h2>" +
                "<p>안녕하세요, <strong>" + name + "</strong>님</p>" +
                "<p><strong>귀하의 재서명 요청이 승인되었습니다.</strong></p>" +
                "<p>" + contractTitle + " 계약서의 재서명을 진행해주시기 바랍니다.</p>" +
                "<p>아래 링크를 클릭하여 서명을 진행해주세요.</p>" +
                "<p><strong>※ 본 링크는 24시간 동안 유효합니다.</strong></p>" +
                "<div style='margin: 30px 0;'>" +
                "<a href='" + signatureLink + "' style='background-color: #3182F6; color: white; padding: 12px 20px; " +
                "text-decoration: none; border-radius: 4px; font-weight: bold;'>계약서 재서명하기</a>" +
                "</div>" +
                "<p>문의사항은 1599-7181로 연락주시기 바랍니다.</p>" +
                "<p>감사합니다.<br>타이어뱅크 드림</p>" +
                getEmailFooter() +
                "</div></body></html>";
            
            // 5. 이메일 전송
            sendHtmlMessage(email, subject, htmlContent);
            log.info("재서명 요청 이메일 발송 성공 - 참여자ID: {}, 수신자: {}, 계약: {}", participantId, email, contractTitle);
            
            return token;
        } catch (Exception e) {
            log.error("재서명 요청 이메일 발송 실패: {}", e.getMessage(), e);
            throw new RuntimeException("재서명 요청 이메일 발송 실패: " + e.getMessage(), e);
        }
    }

    /**
     * PDF 암호를 포함한 이메일을 발송합니다.
     * 
     * @param email 수신자 이메일
     * @param name 수신자 이름
     * @param password PDF 암호
     * @param pdfId PDF ID
     */
    public void sendPdfPasswordEmail(String email, String name, String password, String pdfId) {
        try {
            // 이메일 주소가 없는 경우 처리 중단
            if (email == null || email.trim().isEmpty()) {
                log.warn("이메일 주소 없음 - PDF 암호 이메일 전송 건너뜀");
                return;
            }
            
            // 이메일 제목
            String subject = "[타이어뱅크] 서명 완료 문서 암호 안내";
            
            // HTML 이메일 본문 생성
            String htmlContent = "<html><body style='font-family: Arial, sans-serif;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd;'>" +
                "<h2 style='color: #333;'>서명 완료 문서 암호 안내</h2>" +
                "<p>안녕하세요, <strong>" + name + "</strong>님</p>" +
                "<p>서명이 완료된 계약서의 PDF 열람 암호를 안내해 드립니다.</p>" +
                "<div style='margin: 20px 0; padding: 15px; background-color: #f5f5f5; border-left: 4px solid #4CAF50;'>" +
                "<p style='font-size: 18px; margin: 0;'><strong>PDF 열람 암호: " + password + "</strong></p>" +
                "<p style='font-size: 12px; margin: 5px 0 0 0; color: #666;'>다운로드한 PDF 파일을 열 때 위 암호를 입력해주세요.</p>" +
                "</div>" +
                "<p style='color: #666; font-size: 12px;'>※ 보안을 위해 이 암호는 타인과 공유하지 마세요.</p>" +
                "<p style='color: #666; font-size: 12px;'>※ 문서는 암호를 사용해 내용을 보호하고 있습니다.</p>" +
                "<p>문의사항은 1599-7181로 연락주시기 바랍니다.</p>" +
                "<p>감사합니다.<br>타이어뱅크 드림</p>" +
                getEmailFooter() +
                "</div></body></html>";
            
            // 이메일 전송
            sendHtmlMessage(email, subject, htmlContent);
            log.info("PDF 암호 이메일 발송 성공 - 수신자: {}, PDF ID: {}", 
                    email.replaceAll("(?<=.{3}).(?=.*@)", "*"), pdfId);
            
        } catch (Exception e) {
            log.error("PDF 암호 이메일 발송 실패: {}", e.getMessage(), e);
            // 이메일 실패는 Critical 오류가 아니므로 예외를 던지지 않고 로그만 기록
        }
    }
    
    /**
     * 서명 완료 이메일에 PDF 암호를 포함하여 발송합니다.
     * 
     * @param participant 계약 참여자 객체
     * @param signedContractUrl 서명된 계약 조회 URL (토큰 포함)
     * @param contractTitle 계약 제목
     * @param pdfPassword PDF 암호
     */
    public void sendContractCompletionEmailWithPassword(
            ContractParticipant participant, 
            String signedContractUrl, 
            String contractTitle,
            String pdfPassword) {
        try {
            // 이메일 주소가 없는 경우 처리 중단
            if (participant.getEmail() == null || participant.getEmail().trim().isEmpty()) {
                log.warn("참여자 이메일 없음 (ID: {}) - 계약 완료 이메일 전송 건너뜀", participant.getId());
                return;
            }
            
            // 이메일 제목
            String subject = "[타이어뱅크] " + contractTitle + " - 계약서 서명이 완료되었습니다";
            
            // 암호 안내 섹션 추가
            String passwordSection = 
                "<div style='margin: 20px 0; padding: 15px; background-color: #f5f5f5; border-left: 4px solid #4CAF50;'>" +
                "<p style='margin: 0;'><strong>PDF 열람 암호: " + pdfPassword + "</strong></p>" +
                "<p style='font-size: 12px; margin: 5px 0 0 0; color: #666;'>다운로드한 PDF 파일을 열 때 위 암호를 입력해주세요.</p>" +
                "</div>";
            
            // HTML 이메일 본문 생성
            String htmlContent = "<html><body style='font-family: Arial, sans-serif;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd;'>" +
                "<h2 style='color: #333;'>계약서 서명 완료</h2>" +
                "<p>안녕하세요, <strong>" + participant.getName() + "</strong>님</p>" +
                "<p><strong>" + contractTitle + "</strong> 계약서의 서명이 완료되었습니다.</p>" +
                "<p>아래 링크를 통해 언제든지 계약 내용을 확인하실 수 있습니다.</p>" +
                "<p><strong>※ 이전에 받은 계약서 열람 링크는 더 이상 유효하지 않으니 이 링크를 사용해주세요.</strong></p>" +
                "<p><strong>※ 본 링크는 2년간 유효합니다. 필요시 저장해두세요.</strong></p>" +
                "<div style='margin: 30px 0;'>" +
                "<a href='" + signedContractUrl + "' style='background-color: #3182F6; color: white; padding: 12px 20px; " +
                "text-decoration: none; border-radius: 4px; font-weight: bold;'>서명 완료된 계약서 보기</a>" +
                "</div>" +
                passwordSection +
                "<p>문의사항은 1599-7181로 연락주시기 바랍니다.</p>" +
                "<p>감사합니다.<br>타이어뱅크 드림</p>" +
                getEmailFooter() +
                "</div></body></html>";
            
            // 이메일 전송
            sendHtmlMessage(participant.getEmail(), subject, htmlContent);
            log.info("계약 완료 이메일(암호 포함) 발송 성공 - 참여자ID: {}, 수신자: {}, 계약: {}", 
                    participant.getId(), participant.getEmail(), contractTitle);
            
        } catch (Exception e) {
            log.error("계약 완료 이메일 발송 실패: {}", e.getMessage(), e);
            // 이메일 실패는 Critical 오류가 아니므로 예외를 던지지 않고 로그만 기록
        }
    }

    /**
     * 이메일 템플릿 HTML 불러오기
     * 
     * @param templateName 템플릿 이름
     * @return 템플릿 HTML 문자열
     */
    private String getHtmlTemplate(String templateName) {
        try {
            // 템플릿 경로 (src/main/resources/templates/emails/)
            String templatePath = "templates/emails/" + templateName + ".html";
            
            // 클래스패스에서 리소스 로드
            org.springframework.core.io.Resource resource = 
                new org.springframework.core.io.ClassPathResource(templatePath);
            
            // 리소스가 존재하는지 확인
            if (resource.exists()) {
                // 파일 읽기
                java.nio.file.Path path = resource.getFile().toPath();
                return new String(java.nio.file.Files.readAllBytes(path), java.nio.charset.StandardCharsets.UTF_8);
            } else {
                log.warn("이메일 템플릿을 찾을 수 없음: {}", templatePath);
                return null;
            }
        } catch (Exception e) {
            log.error("템플릿 로드 중 오류 발생: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 재서명 요청 이메일 발송
     */
    public void sendCorrectionRequestEmail(String email, String name, String contractTitle, int fieldCount, String correctionUrl) {
        try {
            // 이메일 제목
            String subject = "[타이어뱅크] " + contractTitle + " - 계약서 재서명 요청";
            
            // HTML 템플릿 사용
            String htmlTemplate = getHtmlTemplate("correction_request");
            
            if (htmlTemplate != null) {
                // 템플릿에 변수 치환
                htmlTemplate = htmlTemplate.replace("{{name}}", name)
                        .replace("{{contractTitle}}", contractTitle)
                        .replace("{{fieldCount}}", String.valueOf(fieldCount))
                        .replace("{{correctionUrl}}", correctionUrl);
                
                sendHtmlMessage(email, subject, htmlTemplate);
                log.info("재서명 요청 이메일 발송 성공 - 수신자: {}, 계약서: {}", email, contractTitle);
            } else {
                // 템플릿 로드 실패시 인라인 HTML 사용
                String htmlContent = "<html><body style='font-family: Arial, sans-serif;'>" +
                    "<div style='max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd;'>" +
                    "<h2 style='color: #333;'>계약서 재서명 요청</h2>" +
                    "<p>안녕하세요, <strong>" + name + "</strong>님</p>" +
                    "<p><strong>" + contractTitle + "</strong> 계약서에 대한 재서명이 요청되었습니다.</p>" +
                    "<p>관리자가 확인한 결과, <strong>" + fieldCount + "개</strong>의 필드에 대한 수정이 필요합니다.</p>" +
                    "<p>아래 링크를 클릭하여 재서명을 진행해주세요.</p>" +
                    "<p><strong>※ 본 링크는 24시간 동안 유효합니다.</strong></p>" +
                    "<div style='margin: 30px 0;'>" +
                    "<a href='" + correctionUrl + "' style='background-color: #3182F6; color: white; padding: 12px 20px; " +
                    "text-decoration: none; border-radius: 4px; font-weight: bold;'>계약서 재서명하기</a>" +
                    "</div>" +
                    "<p>링크가 작동하지 않는 경우, 아래 URL을 브라우저에 복사하여 붙여넣기 해주세요:</p>" +
                    "<p style='word-break: break-all; font-size: 12px; color: #666;'>" + correctionUrl + "</p>" +
                    "<p>문의사항은 1599-7181로 연락주시기 바랍니다.</p>" +
                    "<p>감사합니다.<br>타이어뱅크 드림</p>" +
                    getEmailFooter() +
                    "</div></body></html>";
                
                sendHtmlMessage(email, subject, htmlContent);
                log.info("재서명 요청 이메일 발송 성공 (인라인 HTML) - 수신자: {}, 계약서: {}", email, contractTitle);
            }
        } catch (Exception e) {
            log.error("재서명 요청 이메일 발송 실패: {}", e.getMessage(), e);
            throw new RuntimeException("이메일 발송 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
} 