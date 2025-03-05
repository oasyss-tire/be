package com.inspection.service;

import com.inspection.entity.ContractPdfField;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.interactive.annotation.*;
import org.apache.pdfbox.pdmodel.common.*;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.util.List;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationSquareCircle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import java.util.Base64;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import java.io.InputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfProcessingService {
    
    // A4 크기 상수 정의
    private static final float PDF_WIDTH = 595.28f;
    private static final float PDF_HEIGHT = 841.89f;
    
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
    
    public byte[] addValueToField(byte[] pdf, ContractPdfField field, String value, String type) throws IOException {
        try (PDDocument document = PDDocument.load(pdf)) {
            PDPage page = document.getPage(field.getPage() - 1);
            
            float x = PDF_WIDTH * field.getRelativeX().floatValue();
            float y = PDF_HEIGHT * field.getRelativeY().floatValue();
            float width = PDF_WIDTH * field.getRelativeWidth().floatValue();
            float height = PDF_HEIGHT * field.getRelativeHeight().floatValue();
            
            try (PDPageContentStream contentStream = new PDPageContentStream(
                document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                
                switch (type) {
                    case "text" -> addTextContent(contentStream, document, x, y, height, value);
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
    
    private void addTextContent(PDPageContentStream contentStream, PDDocument document, float x, float y, float height, String text) throws IOException {
        try {
            InputStream fontStream = getClass().getResourceAsStream("/fonts/nanum-gothic/NanumGothic.ttf");
            PDType0Font nanumGothic = PDType0Font.load(document, fontStream);
            
            contentStream.beginText();
            contentStream.setFont(nanumGothic, 12);
            contentStream.setNonStrokingColor(0, 0, 0);
            contentStream.newLineAtOffset(x + 2, PDF_HEIGHT - y - height + 2);
            contentStream.showText(text != null ? text : "");
            contentStream.endText();
            
            if (fontStream != null) {
                fontStream.close();
            }
        } catch (IOException e) {
            log.error("나눔고딕 폰트 로드 실패: {}", e.getMessage());
            // 폰트 로드 실패시 기본 폰트로 폴백
            contentStream.beginText();
            contentStream.setFont(PDType1Font.HELVETICA, 12);
            contentStream.setNonStrokingColor(0, 0, 0);
            contentStream.newLineAtOffset(x + 2, PDF_HEIGHT - y - height + 2);
            contentStream.showText(text != null ? text : "");
            contentStream.endText();
        }
    }
    
    private void addSignatureContent(PDPageContentStream contentStream, PDDocument document, 
            float x, float y, float width, float height, byte[] imageData) throws IOException {
        PDImageXObject image = PDImageXObject.createFromByteArray(document, imageData, "signature");
        contentStream.drawImage(image, x, PDF_HEIGHT - y - height, width, height);
    }
    
    private void addCheckmarkContent(PDPageContentStream contentStream, float x, float y, float width, float height) throws IOException {
        contentStream.setLineWidth(2f);
        contentStream.setStrokingColor(0, 0, 0);
        
        float margin = 4f;
        contentStream.moveTo(x + margin, PDF_HEIGHT - y - margin);
        contentStream.lineTo(x + width - margin, PDF_HEIGHT - y - height + margin);
        contentStream.stroke();
        
        contentStream.moveTo(x + width - margin, PDF_HEIGHT - y - margin);
        contentStream.lineTo(x + margin, PDF_HEIGHT - y - height + margin);
        contentStream.stroke();
    }
    
    public byte[] addValuesToFields(byte[] pdf, List<ContractPdfField> fields) throws IOException {
        try (PDDocument document = PDDocument.load(pdf)) {
            for (ContractPdfField field : fields) {
                if (field.getValue() == null || field.getValue().isEmpty()) {
                    continue;  // 값이 없는 필드는 건너뛰기
                }

                PDPage page = document.getPage(field.getPage() - 1);
                
                float x = PDF_WIDTH * field.getRelativeX().floatValue();
                float y = PDF_HEIGHT * field.getRelativeY().floatValue();
                float width = PDF_WIDTH * field.getRelativeWidth().floatValue();
                float height = PDF_HEIGHT * field.getRelativeHeight().floatValue();
                
                try (PDPageContentStream contentStream = new PDPageContentStream(
                    document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                    
                    switch (field.getType()) {
                        case "text" -> addTextContent(contentStream, document, x, y, height, field.getValue());
                        case "signature" -> {
                            if (field.getValue().startsWith("data:image")) {
                                byte[] imageData = Base64.getDecoder().decode(field.getValue().split(",")[1]);
                                addSignatureContent(contentStream, document, x, y, width, height, imageData);
                            }
                        }
                        case "checkbox" -> {
                            if (Boolean.parseBoolean(field.getValue())) {
                                addCheckmarkContent(contentStream, x, y, width, height);
                            }
                        }
                    }
                }
            }
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
    }
} 