package com.example.presentationgenerator.model;

import java.util.List;

public record SlideContent(
        String title,
        String subtitle,
        List<String> bullets,
        String speakerNotes,
        String visualType,
        String imageQuery,
        String diagramType,
        List<String> diagramLabels,
        String interactionType,
        String interactionPrompt,
        List<String> interactionOptions
) {
    public SlideContent {
        bullets = bullets == null ? List.of() : List.copyOf(bullets);
        diagramLabels = diagramLabels == null ? List.of() : List.copyOf(diagramLabels);
        interactionOptions = interactionOptions == null ? List.of() : List.copyOf(interactionOptions);
    }

    public boolean wantsImage() {
        return "image".equalsIgnoreCase(visualType) || "image_diagram".equalsIgnoreCase(visualType);
    }

    public boolean wantsDiagram() {
        return "diagram".equalsIgnoreCase(visualType) || "image_diagram".equalsIgnoreCase(visualType);
    }

    public boolean hasInteraction() {
        return interactionType != null && !interactionType.isBlank() && !"none".equalsIgnoreCase(interactionType);
    }
}
