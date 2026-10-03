package com.example.presentationgenerator.service;

import com.example.presentationgenerator.util.Chunk;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChunkingService {
    private final int maxChars;
    private final int overlap;

    public ChunkingService(
            @Value("${app.chunking.max-chars:12000}") int maxChars,
            @Value("${app.chunking.overlap:600}") int overlap) {
        if (maxChars <= 0 || overlap < 0 || overlap >= maxChars) {
            throw new IllegalArgumentException("Invalid chunk configuration.");
        }
        this.maxChars = maxChars;
        this.overlap = overlap;
    }

    public List<Chunk> split(String text) {
        List<Chunk> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return chunks;
        }

        int start = 0;
        int index = 0;
        while (start < text.length()) {
            int targetEnd = Math.min(start + maxChars, text.length());
            int end = chooseBoundary(text, start, targetEnd);
            if (end <= start) {
                end = targetEnd;
            }

            String chunkText = text.substring(start, end).trim();
            if (!chunkText.isBlank()) {
                chunks.add(new Chunk(index++, chunkText));
            }

            if (end >= text.length()) {
                break;
            }

            start = Math.max(0, end - overlap);
        }
        return chunks;
    }

    private int chooseBoundary(String text, int start, int targetEnd) {
        if (targetEnd >= text.length()) {
            return text.length();
        }

        int paragraph = text.lastIndexOf("\n\n", targetEnd);
        if (paragraph > start + maxChars / 2) {
            return paragraph;
        }

        int sentence = Math.max(
                text.lastIndexOf(". ", targetEnd),
                Math.max(text.lastIndexOf("? ", targetEnd), text.lastIndexOf("! ", targetEnd))
        );
        if (sentence > start + maxChars / 2) {
            return sentence + 1;
        }

        int space = text.lastIndexOf(' ', targetEnd);
        if (space > start + maxChars / 2) {
            return space;
        }
        return targetEnd;
    }
}
