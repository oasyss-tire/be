package com.inspection.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.pdmodel.graphics.state.RenderingMode;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationSquareCircle;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary;
import org.springframework.stereotype.Service;

import com.inspection.entity.ContractPdfField;
import com.inspection.entity.ParticipantPdfField;
import com.inspection.util.EncryptionUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfProcessingService {
    
    // A4 크기 상수 정의
    private static final float PDF_WIDTH = 595.28f;
    private static final float PDF_HEIGHT = 841.89f;
    
    private final EncryptionUtil encryptionUtil;
    
    public byte[] addFieldsToPdf(byte[] originalPdf, List<ContractPdfField> fields) throws IOException {
        try (PDDocument document = PDDocument.load(originalPdf)) {
            for (ContractPdfField field : fields) {
                PDPage page = document.getPage(field.getPage() - 1);
                
                float x = PDF_WIDTH * field.getRelativeX().floatValue();
                float y = PDF_HEIGHT * field.getRelativeY().floatValue();
                float width = PDF_WIDTH * field.getRelativeWidth().floatValue();
                float height = PDF_HEIGHT * field.getRelativeHeight().floatValue();
                
                // Y좌표 계산 (PDF 좌표계는 하단이 원점)
                float pdfY = PDF_HEIGHT - y - height;
                
                PDRectangle rect = new PDRectangle(x, pdfY, width, height);
                
                // 모든 필드 타입을 단순 영역으로 표시
                addField(page, rect, field.getFieldName(), field.getType());
            }
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
    }
    
    private void addField(PDPage page, PDRectangle rect, String fieldName, String type) throws IOException {
        PDAnnotationSquareCircle annotation = new PDAnnotationSquareCircle(PDAnnotationSquareCircle.SUB_TYPE_SQUARE);
        annotation.setRectangle(rect);
        annotation.setAnnotationName(fieldName);
        
        // 모든 필드 타입에 대해 동일한 색상(FED900) 적용
        annotation.setColor(new PDColor(new float[]{
            254f/255f,  // R: 254
            217f/255f,  // G: 217
            0f/255f     // B: 0
        }, PDDeviceRGB.INSTANCE));
        
        annotation.setBorderStyle(new PDBorderStyleDictionary());
        annotation.getBorderStyle().setWidth(1);
        annotation.setPrinted(true);
        
        page.getAnnotations().add(annotation);
    }
    
    public byte[] addValueToField(byte[] pdf, ParticipantPdfField field, String value, String type) throws IOException {
        try (PDDocument document = PDDocument.load(pdf)) {
            PDPage page = document.getPage(field.getPage() - 1);
            
            float x = PDF_WIDTH * field.getRelativeX().floatValue();
            float y = PDF_HEIGHT * field.getRelativeY().floatValue();
            float width = PDF_WIDTH * field.getRelativeWidth().floatValue();
            float height = PDF_HEIGHT * field.getRelativeHeight().floatValue();
            
            try (PDPageContentStream contentStream = new PDPageContentStream(
                document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                
                switch (type) {
                    case "text", "confirmText" -> addTextContent(contentStream, document, x, y, width, height, value);
                    case "signature" -> {
                        // Base64 이미지 데이터를 바이트 배열로 변환
                        byte[] imageData = Base64.getDecoder().decode(value.split(",")[1]);
                        addSignatureContent(contentStream, document, x, y, width, height, imageData);
                    }
                    case "checkbox" -> addCheckmarkContent(contentStream, x, y, width, height);
                }
            }
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
    }
    
    private void addTextContent(PDPageContentStream contentStream, PDDocument document, float x, float y, float width, float height, String text) throws IOException {
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        
        try {
            InputStream fontStream = getClass().getResourceAsStream("/fonts/nanum-gothic/NanumGothic.ttf");
            PDType0Font nanumGothic = PDType0Font.load(document, fontStream);
            
            // 필드 영역 계산 (여백 고려)
            float availableWidth = width - 4; // 좌우 여백 2px씩
            float availableHeight = height - 4; // 상하 여백 2px씩
            float padding = 2f;
            
            // 최적 폰트 크기 계산 (계약서 정보 보호를 위해 더 작은 폰트까지 허용)
            float fontSize = calculateOptimalFontSize(nanumGothic, text, availableWidth, availableHeight);
            
            contentStream.beginText();
            contentStream.setFont(nanumGothic, fontSize);
            contentStream.setNonStrokingColor(0, 0, 0);
            
            // 텍스트를 줄바꿈하여 표시
            List<String> lines = wrapText(nanumGothic, text, availableWidth, fontSize);
            
            float lineHeight = fontSize + 1; // 줄 간격 최소화
            float startY = PDF_HEIGHT - y - padding - fontSize;
            
            // 첫 번째 줄 위치 설정
            contentStream.newLineAtOffset(x + padding, startY);
            
            // 계약서 정보 보호: 영역을 벗어나더라도 모든 줄 표시
            for (int i = 0; i < lines.size(); i++) {
                // 영역을 벗어나는 경우 경고 로그만 출력하고 계속 진행
                if (i * lineHeight > availableHeight && i > 0) {
                    log.warn("텍스트가 지정된 영역을 벗어남 (줄 {}/{}): {}", 
                            i + 1, lines.size(), 
                            text.substring(0, Math.min(20, text.length())) + "...");
                }
                
                // 첫 번째 줄이 아니면 아래로 이동
                if (i > 0) {
                    contentStream.newLineAtOffset(0, -lineHeight);
                }
                
                contentStream.showText(lines.get(i));
            }
            
            if (fontStream != null) {
                fontStream.close();
            }
        } catch (IOException e) {
            log.error("나눔고딕 폰트 로드 실패: {}", e.getMessage());
            // 폰트 로드 실패시 기본 폰트로 폴백
            addTextContentFallback(contentStream, x, y, height, text);
        }
    }
    
    /**
     * 폰트와 텍스트에 맞는 최적 폰트 크기 계산 (계약서 정보 보호를 위해 더 작은 폰트까지 허용)
     */
    private float calculateOptimalFontSize(PDType0Font font, String text, float availableWidth, float availableHeight) throws IOException {
        float maxFontSize = 14f;
        float minFontSize = 7f;  // 가독성 개선 (기존 5px → 7px)
        
        for (float fontSize = maxFontSize; fontSize >= minFontSize; fontSize -= 0.5f) {
            float textWidth = font.getStringWidth(text) / 1000 * fontSize;
            
            // 한 줄에 들어가면 해당 폰트 크기 사용
            if (textWidth <= availableWidth) {
                return fontSize;
            }
            
            // 여러 줄로 나눠서 들어가는지 확인 (줄 간격 최소화)
            List<String> lines = wrapText(font, text, availableWidth, fontSize);
            float lineHeight = fontSize + 1; // 줄 간격 최소화 (기존 +2 → +1)
            float totalHeight = lines.size() * lineHeight;
            
            if (totalHeight <= availableHeight) {
                return fontSize;
            }
        }
        
        // 계약서 정보 보호: 최소 폰트로도 안 들어가면 그냥 최소 폰트 사용
        log.warn("텍스트가 지정된 영역에 완전히 들어가지 않음 - 영역을 벗어나더라도 모든 정보 표시");
        return minFontSize;
    }
    
    /**
     * 텍스트를 지정된 너비에 맞게 줄바꿈
     */
    private List<String> wrapText(PDType0Font font, String text, float maxWidth, float fontSize) throws IOException {
        List<String> lines = new ArrayList<>();
        String[] words = text.split("\\s+");
        StringBuilder currentLine = new StringBuilder();
        
        for (String word : words) {
            String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
            float testWidth = font.getStringWidth(testLine) / 1000 * fontSize;
            
            if (testWidth <= maxWidth) {
                currentLine = new StringBuilder(testLine);
            } else {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    // 단어가 너무 긴 경우 강제로 자르기
                    lines.add(word);
                }
            }
        }
        
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
        
        return lines;
    }
    
    /**
     * 기본 폰트 사용 시 폴백 메서드 (계약서 정보 보호를 위해 텍스트 자르지 않음)
     */
    private void addTextContentFallback(PDPageContentStream contentStream, float x, float y, float height, String text) throws IOException {
        log.warn("나눔고딕 폰트 로드 실패로 기본 폰트 사용 - 텍스트: {}", text.substring(0, Math.min(10, text.length())) + "...");
        
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA, 10);
        contentStream.setNonStrokingColor(0, 0, 0);
        
        // 계약서 정보 보호: 텍스트를 자르지 않고 모두 표시
        // 기본 폰트로도 간단한 줄바꿈 처리
        float fontSize = 10f;
        float lineHeight = fontSize + 2;
        float maxWidth = 200f; // 대략적인 최대 너비 (실제 필드 너비를 모르므로 보수적으로 설정)
        
        // 단순 줄바꿈 (공백 기준)
        String[] words = text.split("\\s+");
        StringBuilder currentLine = new StringBuilder();
        float currentY = PDF_HEIGHT - y - height + height - fontSize; // 시작 Y 위치
        int lineCount = 0;
        
        contentStream.newLineAtOffset(x + 2, currentY);
        
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
            
            // 대략적인 폭 계산 (정확하지 않지만 안전)
            if (testLine.length() * 6 <= maxWidth) { // 평균 글자 폭 6px로 추정
                currentLine = new StringBuilder(testLine);
            } else {
                // 현재 줄 출력
                if (currentLine.length() > 0) {
                    contentStream.showText(currentLine.toString());
                    lineCount++;
                    
                    // 다음 줄로 이동
                    if (i < words.length - 1) { // 마지막이 아니면
                        contentStream.newLineAtOffset(0, -lineHeight);
                    }
                }
                currentLine = new StringBuilder(word);
            }
        }
        
        // 마지막 줄 출력
        if (currentLine.length() > 0) {
            contentStream.showText(currentLine.toString());
        }
        
        contentStream.endText();
        
        // 경고 로그 추가
        log.warn("폰트 로드 실패로 기본 폰트 사용됨 - 계약서 정보 완전성 확인 필요");
    }
    
    private void addSignatureContent(PDPageContentStream contentStream, PDDocument document, 
            float x, float y, float width, float height, byte[] imageData) throws IOException {
        PDImageXObject image = PDImageXObject.createFromByteArray(document, imageData, "signature");
        
        // 이미지 렌더링 상태 설정
        contentStream.saveGraphicsState();
        // 투명도 설정 (1.0f = 완전 불투명)
        contentStream.setGraphicsStateParameters(new PDExtendedGraphicsState());
        contentStream.setRenderingMode(RenderingMode.FILL);
        
        // 이미지 크기 조정 (약간 크게)
        float scale = 1.2f;  // 20% 크게
        float scaledWidth = width * scale;
        float scaledHeight = height * scale;
        // 중앙 정렬을 위한 오프셋 계산
        float xOffset = (width - scaledWidth) / 2;
        float yOffset = (height - scaledHeight) / 2;
        
        contentStream.drawImage(image, 
            x + xOffset, 
            PDF_HEIGHT - y - height + yOffset, 
            scaledWidth, 
            scaledHeight);
        
        contentStream.restoreGraphicsState();
    }
    
    private void addCheckmarkContent(PDPageContentStream contentStream, float x, float y, float width, float height) throws IOException {
        contentStream.beginText();
        contentStream.setFont(PDType1Font.ZAPF_DINGBATS, 12);
        contentStream.setNonStrokingColor(0, 0, 0);
        
        float fontSize = Math.min(width, height) * 0.8f;
        contentStream.setFont(PDType1Font.ZAPF_DINGBATS, fontSize);
        
        contentStream.newLineAtOffset(
            x + (width - fontSize) / 2,
            PDF_HEIGHT - y - height + (height - fontSize) / 2
        );
        contentStream.showText("✓");
        contentStream.endText();
    }
    
    public byte[] addValuesToFields(byte[] pdf, List<ParticipantPdfField> fields) throws IOException {
        try (PDDocument document = PDDocument.load(pdf)) {
            // 기존 annotation 제거 (노란색 테두리 제거)
            for (PDPage page : document.getPages()) {
                page.setAnnotations(new ArrayList<>());
            }

            // 디버깅용 로그 - 필드 정보 출력
            log.info("PDF에 {} 개의 필드 값을 추가합니다.", fields.size());
            
            // 값만 추가
            for (ParticipantPdfField field : fields) {
                if (field.getValue() == null || field.getValue().isEmpty()) {
                    continue;
                }
                
                // 페이지 인덱스 유효성 검사
                int pageIndex = field.getPage() - 1;
                if (pageIndex < 0 || pageIndex >= document.getNumberOfPages()) {
                    log.error("유효하지 않은 페이지 번호: {} (필드: {})", field.getPage(), field.getFieldName());
                    continue;
                }

                PDPage page = document.getPage(pageIndex);
                float x = PDF_WIDTH * field.getRelativeX().floatValue();
                float y = PDF_HEIGHT * field.getRelativeY().floatValue();
                float width = PDF_WIDTH * field.getRelativeWidth().floatValue();
                float height = PDF_HEIGHT * field.getRelativeHeight().floatValue();
                
                // 필드 값 처리 (민감 정보인 경우 복호화)
                String fieldValue = field.getValue();
                String fieldName = field.getFieldName();
                
                try {
                    // 민감 정보 필드인 경우
                    boolean isSensitiveField = false;
                    if (field.getFormat() != null) {
                        String formatCode = field.getFormat().getCodeId();
                        isSensitiveField = "001004_0001".equals(formatCode) || "001004_0002".equals(formatCode);
                        
                        if (isSensitiveField) {
                            // 값이 암호화되어 있는지 확인하고 복호화
                            try {
                                fieldValue = encryptionUtil.decrypt(fieldValue);
                                log.info("민감정보 복호화 처리: {} ({})", fieldName, formatCode);
                            } catch (Exception e) {
                                log.error("민감정보 복호화 중 오류 발생: {} - {}", fieldName, e.getMessage());
                                // 복호화 실패해도 진행 (원본 값 사용)
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("필드 값 처리 중 오류: {}", e.getMessage());
                }
                
                try (PDPageContentStream contentStream = new PDPageContentStream(
                    document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                    
                    switch (field.getType()) {
                        case "text", "confirmText" -> addTextContent(contentStream, document, x, y, width, height, fieldValue);
                        case "signature" -> {
                            if (fieldValue.startsWith("data:image")) {
                                byte[] imageData = Base64.getDecoder().decode(fieldValue.split(",")[1]);
                                addSignatureContent(contentStream, document, x, y, width, height, imageData);
                            }
                        }
                        case "checkbox" -> {
                            if (Boolean.parseBoolean(fieldValue)) {
                                addCheckmarkContent(contentStream, x, y, width, height);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("필드 {} 추가 중 오류 발생: {}", field.getFieldName(), e.getMessage(), e);
                }
            }
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
    }
    
    /**
     * PDF 문서의 모든 페이지 하단에 서명 시간을 추가합니다.
     * 
     * @param pdf 원본 PDF 바이트 배열
     * @param timeText 추가할 시간 텍스트
     * @return 시간이 추가된 PDF 바이트 배열
     * @throws IOException PDF 처리 중 오류 발생 시
     */
    public byte[] addSignatureTimeToPdf(byte[] pdf, String timeText) throws IOException {
        try (PDDocument document = PDDocument.load(pdf)) {
            // 폰트 준비
            PDType0Font nanumGothic = null;
            InputStream fontStream = null;
            
            try {
                fontStream = getClass().getResourceAsStream("/fonts/nanum-gothic/NanumGothic.ttf");
                nanumGothic = PDType0Font.load(document, fontStream);
            } catch (IOException e) {
                log.error("나눔고딕 폰트 로드 실패: {}", e.getMessage());
            }
            
            // 모든 페이지에 서명 시간 추가
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                PDPage page = document.getPage(i);
                
                try (PDPageContentStream contentStream = new PDPageContentStream(
                        document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                    
                    if (nanumGothic != null) {
                        // 나눔고딕 폰트 사용
                        contentStream.beginText();
                        contentStream.setFont(nanumGothic, 10);
                        contentStream.setNonStrokingColor(0, 0, 0);
                        
                        // 텍스트 위치 설정 (페이지 하단 중앙)
                        float textWidth = nanumGothic.getStringWidth(timeText) / 1000 * 10;
                        float centerX = (PDF_WIDTH - textWidth) / 2;
                        float bottomY = 20; // 페이지 하단에서 20 픽셀 위
                        
                        contentStream.newLineAtOffset(centerX, bottomY);
                        contentStream.showText(timeText);
                        contentStream.endText();
                    } else {
                        // 기본 폰트로 폴백
                        contentStream.beginText();
                        contentStream.setFont(PDType1Font.HELVETICA, 10);
                        contentStream.setNonStrokingColor(0, 0, 0);
                        
                        // 텍스트 위치 설정 (페이지 하단 중앙)
                        float textWidth = PDType1Font.HELVETICA.getStringWidth(timeText) / 1000 * 10;
                        float centerX = (PDF_WIDTH - textWidth) / 2;
                        float bottomY = 20; // 페이지 하단에서 20 픽셀 위
                        
                        contentStream.newLineAtOffset(centerX, bottomY);
                        contentStream.showText(timeText);
                        contentStream.endText();
                    }
                }
            }
            
            // 폰트 스트림 닫기
            if (fontStream != null) {
                fontStream.close();
            }
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
    }
    
    /**
     * PDF 문서의 모든 페이지에 로고 워터마크를 추가합니다.
     * 
     * @param pdf 원본 PDF 바이트 배열
     * @param logoPath 로고 이미지 경로
     * @return 워터마크가 추가된 PDF 바이트 배열
     * @throws IOException PDF 처리 중 오류 발생 시
     */
    public byte[] addLogoWatermark(byte[] pdf, String logoPath) throws IOException {
        try (PDDocument document = PDDocument.load(pdf)) {
            // 로고 이미지 로드
            InputStream logoStream = getClass().getResourceAsStream(logoPath);
            if (logoStream == null) {
                log.error("로고 이미지를 찾을 수 없습니다: {}", logoPath);
                return pdf; // 이미지가 없으면 원본 PDF 반환
            }
            
            byte[] logoBytes = IOUtils.toByteArray(logoStream);
            logoStream.close(); // 스트림 사용 후 닫기
            
            PDImageXObject logoImage = PDImageXObject.createFromByteArray(document, logoBytes, "logo");
            
            // 모든 페이지에 워터마크 추가
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                PDPage page = document.getPage(i);
                
                try (PDPageContentStream contentStream = new PDPageContentStream(
                        document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                    
                    // 워터마크 투명도 설정
                    PDExtendedGraphicsState gs = new PDExtendedGraphicsState();
                    gs.setNonStrokingAlphaConstant(0.2f); // 20% 불투명도
                    contentStream.setGraphicsStateParameters(gs);
                    
                    // 이미지 크기 계산 (페이지 중앙에 위치, 적절한 크기로)
                    float imageWidth = 200; // 로고 너비
                    float imageHeight = imageWidth * logoImage.getHeight() / logoImage.getWidth(); // 비율 유지
                    
                    // 페이지 중앙 좌표
                    float centerX = (PDF_WIDTH - imageWidth) / 2;
                    float centerY = (PDF_HEIGHT - imageHeight) / 2;
                    
                    // 이미지 그리기
                    contentStream.drawImage(logoImage, centerX, centerY, imageWidth, imageHeight);
                }
            }
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
    }
} 