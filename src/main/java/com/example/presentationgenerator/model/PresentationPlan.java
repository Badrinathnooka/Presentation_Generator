package com.example.presentationgenerator.model;

import java.util.List;

public record PresentationPlan(
        String title,
        String subtitle,
        List<SlideContent> slides
) {
    public PresentationPlan {
        slides = slides == null ? List.of() : List.copyOf(slides);
    }
}
