package com.example.presentationgenerator.service;

import com.example.presentationgenerator.model.PresentationPlan;
import com.example.presentationgenerator.model.PresentationOptions;
import com.example.presentationgenerator.util.Chunk;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class OllamaService {
    private final HttpClient httpClient;
    private final JsonMapper jsonMapper;
    private final String baseUrl;
    private final String model;
    private final int maxChunkCalls;
    private final int contextTokens;

    public OllamaService(
            HttpClient httpClient,
            JsonMapper jsonMapper,
            @Value("${OLLAMA_BASE_URL:http://localhost:11434}") String baseUrl,
            @Value("${OLLAMA_MODEL:llama3:latest}") String model,
            @Value("${app.llm.max-chunk-calls:8}") int maxChunkCalls,
            @Value("${app.llm.context-tokens:8192}") int contextTokens) {
        this.httpClient = httpClient;
        this.jsonMapper = jsonMapper;
        this.baseUrl = trimTrailingSlash(baseUrl);
        this.model = model;
        this.maxChunkCalls = maxChunkCalls;
        this.contextTokens = contextTokens;
    }

    public PresentationPlan createPresentation(List<Chunk> chunks, int requestedSlides, PresentationOptions options)
            throws IOException, InterruptedException {
        validateConfig();
        if (chunks.isEmpty()) {
            throw new IllegalArgumentException("No document chunks were produced.");
        }

        List<String> chunkInsights = new ArrayList<>();
        int calls = Math.min(chunks.size(), maxChunkCalls);
        for (int i = 0; i < calls; i++) {
            Chunk chunk = chunks.get(i);
            String insight = analyzeChunk(chunk, i + 1, calls);
            chunkInsights.add("CHUNK " + (i + 1) + "\n" + insight);
        }

        String combined = String.join("\n\n---\n\n", chunkInsights);
        return synthesizePresentation(combined, requestedSlides, options);
    }

    private String analyzeChunk(Chunk chunk, int number, int total)
            throws IOException, InterruptedException {
        String system = "You are a document analysis component in a presentation-generation pipeline. " +
                "Extract only important factual ideas from one document chunk. Do not invent facts. " +
                "Return JSON only and keep every string concise so another model can build slides from it.";

        String user = "Document chunk " + number + " of " + total + ":\n\n" + chunk.text() +
                "\n\nReturn key_points, important_entities, numbers_or_dates, and possible_slide_topics.";

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        schema.put("properties", Map.of(
                "key_points", arraySchema(),
                "important_entities", arraySchema(),
                "numbers_or_dates", arraySchema(),
                "possible_slide_topics", arraySchema()
        ));
        schema.put("required", List.of(
                "key_points", "important_entities", "numbers_or_dates", "possible_slide_topics"
        ));

        return callStructured(system, user, schema, 1000);
    }

    private PresentationPlan synthesizePresentation(String insights, int requestedSlides, PresentationOptions options)
            throws IOException, InterruptedException {
        String visualRules = options.includeVisuals()
                ? "For visualType choose image, diagram, image_diagram, or none. Use image for concept/topic slides, " +
                  "diagram for processes/architectures/comparisons, and image_diagram sparingly. " +
                  "For image slides provide a concise imageQuery suitable for Wikimedia Commons. " +
                  "For diagrams provide diagramType and 3-6 short diagramLabels."
                : "Set visualType to none, imageQuery to empty string, diagramType to none, and diagramLabels to an empty array.";

        String interactionRules = options.includeInteractions()
                ? "Create at least two audience interaction moments when the requested slide count is 6 or more. " +
                  "Use interactionType poll, question, discussion, show_of_hands, or scenario. " +
                  "Provide a concise interactionPrompt and 2-4 interactionOptions when useful. " +
                  "Do not make every slide interactive."
                : "Set interactionType to none, interactionPrompt to empty string, and interactionOptions to an empty array.";

        String system = "You are an expert presentation architect and visual storyteller. Create a coherent, factual presentation " +
                "from document analysis notes. Do not invent facts. Remove duplication. Use a clear narrative: context, core ideas, " +
                "evidence, process/architecture, implications, and conclusion. Keep text concise. " +
                visualRules + " " + interactionRules + " Return JSON only.";

        String user = "Build approximately " + requestedSlides + " content slides from these analysis notes.\n\n" + insights +
                "\n\nPresentation style: " + options.style() +
                ". Requirements: provide a deck title and subtitle plus slide objects. " +
                "Use 2-5 bullets per content slide, short speaker notes, and visual variety. " +
                "Do not invent numbers, names, studies, quotes, or examples not supported by the notes.";

        Map<String, Object> slideSchema = new LinkedHashMap<>();
        slideSchema.put("type", "object");
        slideSchema.put("additionalProperties", false);
        Map<String, Object> slideProperties = new LinkedHashMap<>();
        slideProperties.put("title", stringSchema());
        slideProperties.put("subtitle", stringSchema());
        slideProperties.put("bullets", arraySchema());
        slideProperties.put("speakerNotes", stringSchema());
        slideProperties.put("visualType", stringSchema());
        slideProperties.put("imageQuery", stringSchema());
        slideProperties.put("diagramType", stringSchema());
        slideProperties.put("diagramLabels", arraySchema());
        slideProperties.put("interactionType", stringSchema());
        slideProperties.put("interactionPrompt", stringSchema());
        slideProperties.put("interactionOptions", arraySchema());
        slideSchema.put("properties", slideProperties);
        slideSchema.put("required", List.of(
                "title", "subtitle", "bullets", "speakerNotes", "visualType", "imageQuery", "diagramType",
                "diagramLabels", "interactionType", "interactionPrompt", "interactionOptions"
        ));

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        schema.put("properties", Map.of(
                "title", stringSchema(),
                "subtitle", stringSchema(),
                "slides", Map.of("type", "array", "items", slideSchema)
        ));
        schema.put("required", List.of("title", "subtitle", "slides"));

        String json = callStructured(system, user, schema, 7000);
        PresentationPlan plan = jsonMapper.readValue(json, PresentationPlan.class);
        return normalizePlan(plan, requestedSlides, options.includeVisuals(), options.includeInteractions());
    }

    private PresentationPlan normalizePlan(PresentationPlan plan, int requestedSlides, boolean visualsEnabled, boolean interactionsEnabled) {
        List<com.example.presentationgenerator.model.SlideContent> normalized = new ArrayList<>(plan.slides());

        if (visualsEnabled && normalized.size() >= 5) {
            long diagramCount = normalized.stream().filter(com.example.presentationgenerator.model.SlideContent::wantsDiagram).count();
            if (diagramCount < 2) {
                int[] indices = {1, Math.min(3, normalized.size() - 1)};
                int added = 0;
                for (int index : indices) {
                    if (normalized.get(index).wantsDiagram()) continue;
                    var original = normalized.get(index);
                    List<String> labels = diagramLabelsFrom(original);
                    normalized.set(index, new com.example.presentationgenerator.model.SlideContent(
                            original.title(), original.subtitle(), original.bullets(), original.speakerNotes(),
                            "diagram", "", "process", labels, original.interactionType(),
                            original.interactionPrompt(), original.interactionOptions()
                    ));
                    added++;
                    if (diagramCount + added >= 2) break;
                }
            }

            long imageCount = normalized.stream().filter(com.example.presentationgenerator.model.SlideContent::wantsImage).count();
            if (imageCount < 2) {
                for (int i = 0; i < normalized.size() && imageCount < 2; i++) {
                    var original = normalized.get(i);
                    if (original.wantsImage() || original.hasInteraction()) continue;
                    normalized.set(i, new com.example.presentationgenerator.model.SlideContent(
                            original.title(), original.subtitle(), original.bullets(), original.speakerNotes(),
                            "image", original.title() + " concept", original.diagramType(), original.diagramLabels(),
                            original.interactionType(), original.interactionPrompt(), original.interactionOptions()
                    ));
                    imageCount++;
                }
            }
        }

        if (interactionsEnabled && requestedSlides >= 6) {
            long count = normalized.stream().filter(com.example.presentationgenerator.model.SlideContent::hasInteraction).count();
            if (count < 2 && !normalized.isEmpty()) {
                int mid = Math.min(Math.max(1, normalized.size() / 2), normalized.size() - 1);
                var original = normalized.get(mid);
                normalized.set(mid, new com.example.presentationgenerator.model.SlideContent(
                        original.title(), original.subtitle(), original.bullets(), original.speakerNotes(),
                        original.visualType(), original.imageQuery(), original.diagramType(), original.diagramLabels(),
                        "show_of_hands", "Quick audience check: which of these ideas best matches your current situation?",
                        List.of("A", "B", "C")
                ));
            }
        }
        return new PresentationPlan(plan.title(), plan.subtitle(), normalized);
    }

    private String callStructured(
            String system,
            String user,
            Map<String, Object> schema,
            int maxOutputTokens) throws IOException, InterruptedException {

        Map<String, Object> options = new LinkedHashMap<>();
        options.put("temperature", 0.2);
        options.put("num_ctx", contextTokens);
        options.put("num_predict", maxOutputTokens);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("stream", false);
        body.put("keep_alive", "5m");
        body.put("format", schema);
        body.put("options", options);
        body.put("messages", List.of(
                Map.of("role", "system", "content", system),
                Map.of("role", "user", "content", user)
        ));

        String requestBody = jsonMapper.writeValueAsString(body);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/chat"))
                .timeout(Duration.ofMinutes(5))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException(
                    "Ollama API error (" + response.statusCode() + "): " + response.body());
        }

        JsonNode root = jsonMapper.readTree(response.body());
        JsonNode content = root.path("message").path("content");
        if (content.isMissingNode() || content.asText().isBlank()) {
            throw new IllegalStateException("Ollama response did not contain message.content.");
        }

        return cleanJson(content.asText());
    }

    private List<String> diagramLabelsFrom(com.example.presentationgenerator.model.SlideContent slide) {
        List<String> labels = new ArrayList<>();
        for (String bullet : slide.bullets()) {
            if (bullet == null || bullet.isBlank()) continue;
            String label = bullet.trim().replaceAll("[.:;]+$", "");
            if (label.length() > 22) label = label.substring(0, 22).trim() + "…";
            labels.add(label);
            if (labels.size() == 4) break;
        }
        while (labels.size() < 3) labels.add(labels.isEmpty() ? "Key idea" : "Outcome");
        return labels;
    }

    private String cleanJson(String content) {
        String trimmed = content.trim();
        if (trimmed.startsWith("```") && trimmed.endsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline >= 0) {
                trimmed = trimmed.substring(firstNewline + 1, trimmed.length() - 3).trim();
            }
        }
        return trimmed;
    }

    private Map<String, Object> arraySchema() {
        return Map.of("type", "array", "items", Map.of("type", "string"));
    }

    private Map<String, Object> stringSchema() {
        return Map.of("type", "string");
    }

    private void validateConfig() {
        if (baseUrl.isBlank()) {
            throw new IllegalStateException("OLLAMA_BASE_URL is empty.");
        }
        if (model.isBlank()) {
            throw new IllegalStateException("OLLAMA_MODEL is empty.");
        }
    }

    private String trimTrailingSlash(String url) {
        String value = url == null ? "" : url.trim();
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }
}
