package com.inspection.service;

import org.springframework.stereotype.Service;
import java.io.File;
import java.io.IOException;

@Service
public class Pdf2HtmlService {

    private final String uploadDir = System.getProperty("user.dir") + "/uploads/pdf_uploads/";
    private final String pdf2htmlExPath = "C:\\Program Files\\pdf2htmlEX\\pdf2htmlEX.exe"; // 실행 파일 절대 경로

    public String convertPdfToHtml(String pdfFileName) throws IOException, InterruptedException {
        // 1. 원본 PDF 파일 경로 확인
        File pdfFile = new File(uploadDir + pdfFileName);
        if (!pdfFile.exists()) {
            throw new IOException("PDF 파일을 찾을 수 없습니다: " + pdfFile.getAbsolutePath());
        }

        // 2. 변환될 HTML 파일 경로 설정
        String htmlFileName = pdfFileName.replace(".pdf", ".html");
        File htmlFile = new File(uploadDir + htmlFileName);

        // 3. pdf2htmlEX 실행 (작업 디렉토리 이동 후 실행)
        ProcessBuilder pb = new ProcessBuilder(
            pdf2htmlExPath,   // 실행 파일 경로
            pdfFileName,       // 변환할 PDF 파일명
            htmlFileName       // 변환된 HTML 파일명
        );

        pb.directory(new File(uploadDir)); // ✅ 작업 디렉토리를 `uploads/pdf_uploads/`로 변경
        pb.redirectErrorStream(true); // 오류 출력도 표준 출력으로 합침

        Process process = pb.start();

        // 4. 변환 프로세스 실행 및 종료 대기
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("pdf2htmlEX 변환 실패, exit code: " + exitCode);
        }

        return htmlFileName; // 변환된 HTML 파일 이름 반환
    }
}
