package com.example.presentationgenerator.model;

public record GenerationResult(
        byte[] pptx,
        String fileName,
        String contentType,
        int extractedCharacters,
        int chunkCount,
        int slideCount
) {
}
