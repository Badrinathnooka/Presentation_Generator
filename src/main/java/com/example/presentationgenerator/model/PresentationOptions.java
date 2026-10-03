package com.example.presentationgenerator.model;

public record PresentationOptions(
        boolean includeVisuals,
        boolean includeInteractions,
        String style
) {
    public static PresentationOptions defaults() {
        return new PresentationOptions(true, true, "professional");
    }
}
