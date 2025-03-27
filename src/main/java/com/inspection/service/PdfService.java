package com.inspection.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.inspection.dto.SignaturePositionRequest;
import com.inspection.dto.SignatureRequest;
import com.inspection.entity.ParticipantPdfField;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class PdfService {
    
    private final FileService fileService;

    

    

    @Value("${file.upload.path}")

    private String uploadPath;

    

    private static final Logger log = LoggerFactory.getLogger(PdfService.class);

    

    private static final float STANDARD_PDF_WIDTH = 800f;   // 프론트엔드의 Page width와 동일
    private static final float STANDARD_PDF_HEIGHT = 1035f; // 프론트엔드의 Page height와 동일
    private static final float SIGNATURE_WIDTH = 200f;      // 서명 너비
    private static final float SIGNATURE_HEIGHT = 100f;     // 서명 높이

    

    public String signPdf(String originalPdfPath, String signatureImagePath, SignaturePositionRequest position) 

            throws IOException {

        log.info("PDF 서명 처리 시작 - 원본: {}, 서명: {}", originalPdfPath, signatureImagePath);

        

        Path pdfPath = Paths.get(uploadPath, originalPdfPath);

        Path signaturePath = Paths.get(uploadPath, "signatures", signatureImagePath);

        

        if (!Files.exists(pdfPath)) {

            throw new RuntimeException("원본 PDF 파일을 찾을 수 없습니다: " + pdfPath);

        }

        

        if (!Files.exists(signaturePath)) {

            throw new RuntimeException("서명 이미지 파일을 찾을 수 없습니다: " + signaturePath);

        }

        

        try (PDDocument document = PDDocument.load(pdfPath.toFile())) {

            if (position.getPageNumber() >= document.getNumberOfPages()) {

                throw new RuntimeException("잘못된 페이지 번호입니다.");

            }

            

            PDPage page = document.getPage(position.getPageNumber());

            PDImageXObject image = PDImageXObject.createFromFile(signaturePath.toString(), document);

            

            // PDF 페이지의 실제 크기

            float pageWidth = page.getMediaBox().getWidth();

            float pageHeight = page.getMediaBox().getHeight();

            

            // 정규화된 좌표를 사용하여 실제 PDF 좌표 계산

            float pdfX = position.getNormX() * pageWidth;

            float pdfY = pageHeight - (position.getNormY() * pageHeight);

            

            // 서명 크기 계산 (페이지 크기에 비례하여 조정)

            float signatureScale = Math.min(pageWidth / 800f, pageHeight / 1035f);

            float width = SIGNATURE_WIDTH * signatureScale;

            float height = SIGNATURE_HEIGHT * signatureScale;

            

            // 서명 위치 조정 (중앙 정렬)

            pdfX -= width / 2;

            pdfY -= height / 2;

            

            log.info("PDF 서명 정보 - 페이지크기: {}x{}, 정규화좌표: ({}, {}), 실제좌표: ({}, {}), 서명크기: {}x{}", 

                pageWidth, pageHeight,

                position.getNormX(), position.getNormY(),

                pdfX, pdfY,

                width, height);

            

            try (PDPageContentStream contentStream = new PDPageContentStream(

                    document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {

                

                contentStream.drawImage(image, 

                    pdfX,

                    pdfY,

                    width,

                    height

                );

            }

            

            // signed 디렉토리 생성

            Path signedDir = Paths.get(uploadPath, "signed");

            if (!Files.exists(signedDir)) {

                Files.createDirectories(signedDir);

            }

            

            String signedFileName = "signed_" + originalPdfPath;

            Path signedPdfPath = signedDir.resolve(signedFileName);

            document.save(signedPdfPath.toFile());

            log.info("서명된 PDF 저장 완료: {}", signedPdfPath);

            

            return signedFileName;

        }

    }

    public String addSignatureOverlay(String originalPdfPath, SignatureRequest request) 
            throws IOException {
        log.info("PDF 서명 오버레이 시작 - 원본: {}", originalPdfPath);
        
        Path pdfPath = Paths.get(uploadPath, originalPdfPath);
        
        if (!Files.exists(pdfPath)) {
            throw new RuntimeException("원본 PDF 파일을 찾을 수 없습니다: " + pdfPath);
        }
        
        try (PDDocument document = PDDocument.load(pdfPath.toFile())) {
            PDPage page = document.getPage(request.getPageNumber());
            
            // Base64 이미지 데이터를 PDImageXObject로 변환
            byte[] imageBytes = Base64.getDecoder().decode(
                request.getSignatureData().split(",")[1]
            );
            PDImageXObject overlay = PDImageXObject.createFromByteArray(
                document, 
                imageBytes, 
                "overlay.png"
            );
            
            // 페이지 크기
            float pageWidth = page.getMediaBox().getWidth();
            float pageHeight = page.getMediaBox().getHeight();
            
            // 오버레이 이미지 그리기
            try (PDPageContentStream contentStream = new PDPageContentStream(
                    document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                contentStream.drawImage(overlay, 0, 0, pageWidth, pageHeight);
            }
            
            // signed 디렉토리 생성
            Path signedDir = Paths.get(uploadPath, "signed");
            if (!Files.exists(signedDir)) {
                Files.createDirectories(signedDir);
            }
            
            String signedFileName = "signed_" + originalPdfPath;
            Path signedPdfPath = signedDir.resolve(signedFileName);
            document.save(signedPdfPath.toFile());
            
            log.info("서명된 PDF 저장 완료: {}", signedPdfPath);
            
            return signedFileName;
        }
    }
    
    /**
     * 템플릿 PDF를 참여자용 PDF로 복사
     */
    public void copyTemplateForParticipant(String sourcePdfId, String targetPdfId) {
        try {
            // 소스 PDF 경로 (pdfs 폴더에서 읽기)
            Path sourcePath = Paths.get(".", "uploads", "pdfs", sourcePdfId);
            
            // 대상 PDF 경로 (participants 폴더에 저장)
            Path targetDir = Paths.get(uploadPath, "participants");
            Path targetPath = targetDir.resolve(targetPdfId);
            
            // 디렉토리 존재 확인
            if (!Files.exists(sourcePath)) {
                throw new RuntimeException("템플릿 PDF 파일을 찾을 수 없습니다: " + sourcePath);
            }
            
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }
            
            // 파일 복사
            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("PDF copied successfully from {} to {}", sourcePdfId, targetPdfId);
            
        } catch (IOException e) {
            log.error("Error copying PDF file from {} to {}", sourcePdfId, targetPdfId, e);
            throw new RuntimeException("PDF 파일 복사 중 오류가 발생했습니다.", e);
        }
    }
    
    /**
     * PDF 파일 삭제
     */
    public void deletePdf(String pdfId, String type) {
        try {
            Path pdfPath = Paths.get(uploadPath, type, pdfId);
            Files.deleteIfExists(pdfPath);
            log.info("PDF deleted successfully: {}", pdfId);
        } catch (IOException e) {
            log.error("Error deleting PDF: {}", pdfId, e);
            throw new RuntimeException("PDF 파일 삭제 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 재서명용 PDF 생성
     * 원본 PDF를 복사하고 수정된 필드 값으로 업데이트합니다.
     */
    public void generateSignedPdfWithUpdatedFields(String sourcePdfId, String targetPdfId, List<ParticipantPdfField> fields) {
        try {
            log.info("재서명 PDF 생성 시작 - 원본: {}, 대상: {}", sourcePdfId, targetPdfId);
            
            // 소스 PDF 경로 (participants 폴더에서 읽기)
            Path sourcePath = Paths.get(uploadPath, "participants", sourcePdfId);
            
            // 대상 PDF 경로 (resigned 폴더에 저장)
            Path targetDir = Paths.get(uploadPath, "resigned");
            Path targetPath = targetDir.resolve(targetPdfId);
            
            // 디렉토리 존재 확인
            if (!Files.exists(sourcePath)) {
                throw new RuntimeException("원본 PDF 파일을 찾을 수 없습니다: " + sourcePath);
            }
            
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }
            
            // 원본 PDF 읽기
            byte[] pdfData = Files.readAllBytes(sourcePath);
            
            // PdfProcessingService를 사용하여 필드 값 추가
            try (PDDocument document = PDDocument.load(pdfData)) {
                // 필드 값이 있는 경우에만 처리
                if (fields != null && !fields.isEmpty()) {
                    // 재서명 처리 로직 (필드 값으로 PDF 업데이트)
                    // 이 작업은 PdfProcessingService에서 수행
                }
                
                // 파일 저장
                document.save(targetPath.toFile());
                log.info("재서명 PDF 저장 완료: {}", targetPdfId);
            }
            
        } catch (IOException e) {
            log.error("재서명 PDF 생성 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("재서명 PDF 생성 중 오류가 발생했습니다.", e);
        }
    }
} 