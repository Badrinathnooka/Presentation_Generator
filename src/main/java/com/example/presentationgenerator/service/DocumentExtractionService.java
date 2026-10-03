package com.example.presentationgenerator.service;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class DocumentExtractionService {
    private final Tika tika = new Tika();

    public String extractText(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty.");
        }

        String fileName = file.getOriginalFilename() == null ? "document" : file.getOriginalFilename();
        String lower = fileName.toLowerCase();
        boolean supported = lower.endsWith(".pdf") || lower.endsWith(".docx") || lower.endsWith(".doc")
                || lower.endsWith(".txt") || lower.endsWith(".md") || lower.endsWith(".rtf")
                || lower.endsWith(".pptx") || lower.endsWith(".ppt");

        if (!supported) {
            throw new IllegalArgumentException("Supported input files: PDF, DOCX, DOC, TXT, MD, RTF, PPTX, PPT.");
        }

        String text;
        try {
            text = tika.parseToString(file.getInputStream());
        } catch (TikaException e) {
            throw new IOException("Unable to extract text from the uploaded document.", e);
        }
        text = normalizeWhitespace(text);

        if (text.isBlank()) {
            throw new IllegalArgumentException("No readable text was extracted. Scanned/image-only PDFs need OCR.");
        }

        return text;
    }

    private String normalizeWhitespace(String text) {
        return text
                .replace("\u0000", " ")
                .replaceAll("[ \t]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }
}
