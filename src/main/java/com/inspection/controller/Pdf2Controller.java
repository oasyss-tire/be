package com.inspection.controller;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.inspection.service.Pdf2HtmlService;

@RestController
@RequestMapping("/api/pdf")
public class Pdf2Controller {
    private final Pdf2HtmlService pdf2HtmlService;

    public Pdf2Controller(Pdf2HtmlService pdf2HtmlService) {
        this.pdf2HtmlService = pdf2HtmlService;
    }

    // 업로드 폴더 (프로젝트 루트 내 uploads/pdf_uploads/ 폴더)
    private final String uploadDir = System.getProperty("user.dir") + "/uploads/pdf_uploads/";

    @PostMapping("/upload")
    public ResponseEntity<?> uploadPdf(@RequestParam("file") MultipartFile file) {
        // 1. 저장 폴더 생성 (없으면 자동 생성)
        File directory = new File(uploadDir);
        if (!directory.exists() && !directory.mkdirs()) {
            return ResponseEntity.internalServerError().body("폴더 생성 실패: " + uploadDir);
        }

        // 2. 파일 저장 경로 설정 (UUID를 이용해 고유 파일명 생성)
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return ResponseEntity.badRequest().body("파일 이름이 유효하지 않습니다.");
        }
        String uniqueFilename = UUID.randomUUID().toString() + "_" + originalFilename;
        File pdfFile = new File(uploadDir + uniqueFilename);
        try {
            // 3. 파일 저장
            file.transferTo(pdfFile);
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                                 .body("파일 저장 중 오류 발생: " + e.getMessage());
        }

        // 4. 저장된 파일 경로 반환 (필요시 URL로 변환)
        Map<String, String> result = new HashMap<>();
        result.put("message", "파일 업로드 성공");
        result.put("pdfFilePath", pdfFile.getAbsolutePath());
        // 예를 들어, 클라이언트에서 접근할 수 있는 URL을 구성하려면 (서버 설정에 따라 달라짐)
        result.put("pdfFileUrl", "/uploads/pdf_uploads/" + uniqueFilename);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/convert")
    public ResponseEntity<?> uploadAndConvertPdf(@RequestParam("file") MultipartFile file) {
        try {
            // 1. 저장 폴더 생성 (없으면 자동 생성)
            File directory = new File(uploadDir);
            if (!directory.exists() && !directory.mkdirs()) {
                return ResponseEntity.internalServerError().body("폴더 생성 실패: " + uploadDir);
            }

            // 2. 파일 저장 경로 설정 (UUID 사용)
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) {
                return ResponseEntity.badRequest().body("파일 이름이 유효하지 않습니다.");
            }
            String uniqueFilename = UUID.randomUUID().toString() + "_" + originalFilename;
            File pdfFile = new File(uploadDir + uniqueFilename);
            file.transferTo(pdfFile); // PDF 파일 저장

            // 3. PDF를 HTML로 변환
            // pdf2HtmlService.convertPdfToHtml() 메서드 내에서 입력 파일 경로와 출력 파일 경로를 관리하도록 수정할 수도 있음.
            String htmlFileName = pdf2HtmlService.convertPdfToHtml(uniqueFilename);

            // 4. 변환된 HTML 파일 URL 반환 (클라이언트에서 바로 불러올 수 있도록)
            Map<String, String> result = new HashMap<>();
            result.put("message", "변환 성공");
            result.put("htmlFileUrl", "/uploads/pdf_uploads/" + htmlFileName);
            return ResponseEntity.ok(result);
        } catch (IOException | InterruptedException e) {
            return ResponseEntity.internalServerError().body("변환 실패: " + e.getMessage());
        }
    }
}
