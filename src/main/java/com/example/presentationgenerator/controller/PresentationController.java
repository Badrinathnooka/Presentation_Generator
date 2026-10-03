package com.example.presentationgenerator.controller;

import com.example.presentationgenerator.model.GenerationResult;
import com.example.presentationgenerator.service.PresentationPipelineService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/presentations")
public class PresentationController {
    private final PresentationPipelineService pipelineService;

    public PresentationController(PresentationPipelineService pipelineService) {
        this.pipelineService = pipelineService;
    }

    @PostMapping(value = "/generate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> generate(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "slides", required = false) Integer slides,
            @RequestParam(value = "visuals", defaultValue = "true") boolean visuals,
            @RequestParam(value = "interactions", defaultValue = "true") boolean interactions,
            @RequestParam(value = "style", defaultValue = "professional") String style) throws Exception {

        GenerationResult result = pipelineService.generate(file, slides, visuals, interactions, style);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(result.contentType()));
        headers.setContentDisposition(ContentDisposition.attachment().filename(result.fileName()).build());
        headers.setContentLength(result.pptx().length);
        headers.add("X-Extracted-Characters", String.valueOf(result.extractedCharacters()));
        headers.add("X-Chunk-Count", String.valueOf(result.chunkCount()));
        headers.add("X-Slide-Count", String.valueOf(result.slideCount()));

        return ResponseEntity.ok().headers(headers).body(result.pptx());
    }
}
