package com.example.presentationgenerator.service;

import com.example.presentationgenerator.model.GenerationResult;
import com.example.presentationgenerator.model.PresentationOptions;
import com.example.presentationgenerator.model.PresentationPlan;
import com.example.presentationgenerator.util.Chunk;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class PresentationPipelineService {
    private final DocumentExtractionService extractionService;
    private final ChunkingService chunkingService;
    private final OllamaService ollamaService;
    private final PptxGenerationService pptxGenerationService;
    private final int defaultSlides;
    private final int maxSlides;

    public PresentationPipelineService(
            DocumentExtractionService extractionService,
            ChunkingService chunkingService,
            OllamaService ollamaService,
            PptxGenerationService pptxGenerationService,
            @Value("${app.presentation.default-slides:8}") int defaultSlides,
            @Value("${app.presentation.max-slides:15}") int maxSlides) {
        this.extractionService = extractionService;
        this.chunkingService = chunkingService;
        this.ollamaService = ollamaService;
        this.pptxGenerationService = pptxGenerationService;
        this.defaultSlides = defaultSlides;
        this.maxSlides = maxSlides;
    }

    public GenerationResult generate(MultipartFile file, Integer requestedSlides,
                                     boolean includeVisuals, boolean includeInteractions, String style)
            throws IOException, InterruptedException {
        String text = extractionService.extractText(file);
        List<Chunk> chunks = chunkingService.split(text);
        int slideCount = requestedSlides == null ? defaultSlides : Math.max(3, Math.min(requestedSlides, maxSlides));
        PresentationOptions options = new PresentationOptions(includeVisuals, includeInteractions,
                style == null || style.isBlank() ? "professional" : style.trim());

        PresentationPlan plan = ollamaService.createPresentation(chunks, slideCount, options);
        if (plan.slides().isEmpty()) {
            throw new IllegalStateException("The LLM returned an empty presentation plan.");
        }
        byte[] pptx = pptxGenerationService.generate(plan, includeVisuals);

        String original = file.getOriginalFilename() == null ? "document" : file.getOriginalFilename();
        String base = original.replaceAll("\\.[^.]+$", "").replaceAll("[^a-zA-Z0-9_-]+", "_");
        String outputName = base + "_presentation.pptx";

        return new GenerationResult(
                pptx,
                outputName,
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                text.length(),
                chunks.size(),
                plan.slides().size() + 1
        );
    }

    public GenerationResult generate(MultipartFile file, Integer requestedSlides)
            throws IOException, InterruptedException {
        return generate(file, requestedSlides, true, true, "professional");
    }
}
