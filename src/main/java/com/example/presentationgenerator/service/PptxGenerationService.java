package com.example.presentationgenerator.service;

import com.example.presentationgenerator.model.PresentationPlan;
import com.example.presentationgenerator.model.SlideContent;
import org.apache.poi.sl.usermodel.PictureData;
import org.apache.poi.sl.usermodel.VerticalAlignment;
import org.apache.poi.sl.usermodel.ShapeType;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFAutoShape;
import org.apache.poi.xslf.usermodel.XSLFPictureShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextRun;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.geom.Rectangle2D;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class PptxGenerationService {
    private final ImageSearchService imageSearchService;

    public PptxGenerationService(ImageSearchService imageSearchService) {
        this.imageSearchService = imageSearchService;
    }

    public byte[] generate(PresentationPlan plan, boolean includeVisuals) throws IOException {
        try (XMLSlideShow ppt = new XMLSlideShow()) {
            ppt.setPageSize(new Dimension((int) (13.333333 * 72), (int) (7.5 * 72)));

            addTitleSlide(ppt, plan.title(), plan.subtitle());
            for (SlideContent slideContent : plan.slides()) {
                addContentSlide(ppt, slideContent, includeVisuals);
            }
            addClosingQASlide(ppt);

            try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                ppt.write(output);
                return output.toByteArray();
            }
        }
    }

    public byte[] generate(PresentationPlan plan) throws IOException {
        return generate(plan, true);
    }

    private void addTitleSlide(XMLSlideShow ppt, String title, String subtitle) {
        XSLFSlide slide = ppt.createSlide();
        addAccentBar(slide);
        addTextBox(slide, safe(title, "AI Generated Presentation"), 0.9, 1.55, 11.5, 1.2, 30, true);
        if (subtitle != null && !subtitle.isBlank()) {
            addTextBox(slide, subtitle, 0.95, 3.0, 10.8, 0.8, 18, false);
        }
        addTextBox(slide, "Generated with Spring Boot + Ollama + Apache POI", 0.95, 6.55, 10.8, 0.35, 9, false);
    }

    private void addContentSlide(XMLSlideShow ppt, SlideContent content, boolean includeVisuals) throws IOException {
        XSLFSlide slide = ppt.createSlide();
        addAccentBar(slide);
        addTextBox(slide, safe(content.title(), "Untitled"), 0.65, 0.35, 12.0, 0.65, 24, true);
        if (content.subtitle() != null && !content.subtitle().isBlank()) {
            addTextBox(slide, content.subtitle(), 0.72, 0.98, 11.6, 0.42, 11, false);
        }

        boolean hasInteraction = content.hasInteraction();
        boolean hasVisual = includeVisuals && (content.wantsImage() || content.wantsDiagram());
        double textX = 0.75;
        double textY = 1.55;
        double textW = hasVisual ? 6.15 : 11.6;
        double textH = hasInteraction ? 2.85 : 4.95;

        addBulletList(slide, content.bullets(), textX, textY, textW, textH);

        if (hasVisual) {
            if (content.wantsImage() && content.imageQuery() != null && !content.imageQuery().isBlank()) {
                ImageSearchService.ImageAsset image = imageSearchService.search(content.imageQuery());
                if (image != null) {
                    addImage(slide, image, 7.2, 1.45, 5.35, 3.45);
                    addTextBox(slide, "Source: Wikimedia Commons — " + trimSource(image.title()),
                            7.25, 4.95, 5.15, 0.38, 7.5, false);
                } else {
                    addVisualFallback(slide, content.title(), 7.25, 1.55, 5.2, 2.9);
                }
            }
            if (content.wantsDiagram() && !hasInteraction) {
                double diagramY = content.wantsImage() ? 5.15 : 1.55;
                double diagramH = content.wantsImage() ? 1.55 : 3.9;
                addDiagram(slide, content, 7.25, diagramY, 5.15, diagramH);
            }
        }

        if (hasInteraction) {
            addInteractionPanel(slide, content, 0.75, 4.85, 11.7, 1.75);
        }

        if (content.speakerNotes() != null && !content.speakerNotes().isBlank()) {
            addTextBox(slide, "Speaker note: " + content.speakerNotes(), 0.75, 6.72, 11.8, 0.3, 7.5, false);
        }
    }

    private void addBulletList(XSLFSlide slide, List<String> bullets,
                               double x, double y, double w, double h) {
        if (bullets == null || bullets.isEmpty()) {
            addTextBox(slide, "", x, y, w, h, 16, false);
            return;
        }
        XSLFTextBox body = slide.createTextBox();
        body.setAnchor(new Rectangle2D.Double(x * 72, y * 72, w * 72, h * 72));
        body.setVerticalAlignment(VerticalAlignment.TOP);
        boolean first = true;
        for (String bullet : bullets) {
            XSLFTextParagraph p = body.addNewTextParagraph();
            p.setBullet(true);
            p.setLeftMargin(18.0);
            p.setIndent(0.0);
            if (!first) p.setSpaceBefore(8.0);
            XSLFTextRun run = p.addNewTextRun();
            run.setFontSize(16.5);
            run.setText(safe(bullet, ""));
            first = false;
        }
    }

    private void addInteractionPanel(XSLFSlide slide, SlideContent content,
                                     double x, double y, double w, double h) {
        XSLFAutoShape panel = slide.createAutoShape();
        panel.setShapeType(ShapeType.ROUND_RECT);
        panel.setAnchor(new Rectangle2D.Double(x * 72, y * 72, w * 72, h * 72));
        panel.setFillColor(new Color(245, 247, 250));
        panel.setLineColor(new Color(180, 190, 205));

        addTextBox(slide, interactionHeading(content.interactionType()), x + 0.18, y + 0.12, 2.25, 0.28, 9, true);
        addTextBox(slide, safe(content.interactionPrompt(), "What do you think?"), x + 0.18, y + 0.45, w - 0.36, 0.45, 12.5, true);

        List<String> options = content.interactionOptions();
        if (options == null || options.isEmpty()) options = List.of("Discuss", "Compare", "Share");
        double gap = 0.15;
        double cardW = (w - gap * (options.size() - 1)) / options.size();
        for (int i = 0; i < options.size(); i++) {
            double cx = x + i * (cardW + gap);
            XSLFAutoShape card = slide.createAutoShape();
            card.setShapeType(ShapeType.ROUND_RECT);
            card.setAnchor(new Rectangle2D.Double(cx * 72, (y + 1.05) * 72, cardW * 72, 0.48 * 72));
            card.setFillColor(Color.WHITE);
            card.setLineColor(new Color(190, 200, 215));
            addTextBox(slide, options.get(i), cx + 0.04, y + 1.15, cardW - 0.08, 0.25, 10, true);
        }
    }

    private String interactionHeading(String type) {
        if (type == null) return "AUDIENCE MOMENT";
        return switch (type.toLowerCase()) {
            case "poll" -> "QUICK POLL";
            case "show_of_hands" -> "SHOW OF HANDS";
            case "discussion" -> "DISCUSS";
            case "scenario" -> "SCENARIO";
            default -> "AUDIENCE QUESTION";
        };
    }

    private void addDiagram(XSLFSlide slide, SlideContent content,
                            double x, double y, double w, double h) {
        List<String> labels = content.diagramLabels();
        if (labels == null || labels.size() < 2) {
            labels = List.of("Input", "Process", "Output");
        }
        int count = Math.min(labels.size(), 6);
        double gap = 0.12;
        double boxW = Math.min(1.5, (w - gap * (count - 1)) / count);
        double boxH = Math.min(1.0, h * 0.55);
        double totalW = count * boxW + (count - 1) * gap;
        double startX = x + (w - totalW) / 2.0;
        double boxY = y + (h - boxH) / 2.0;

        for (int i = 0; i < count; i++) {
            double bx = startX + i * (boxW + gap);
            XSLFAutoShape box = slide.createAutoShape();
            box.setShapeType(ShapeType.ROUND_RECT);
            box.setAnchor(new Rectangle2D.Double(bx * 72, boxY * 72, boxW * 72, boxH * 72));
            box.setFillColor(new Color(235, 242, 250));
            box.setLineColor(new Color(110, 135, 165));
            addTextBox(slide, labels.get(i), bx + 0.06, boxY + 0.28, boxW - 0.12, 0.38, 10.5, true);
            if (i < count - 1) {
                addTextBox(slide, "→", bx + boxW + 0.01, boxY + 0.22, gap - 0.02, 0.4, 16, true);
            }
        }
        addTextBox(slide, titleCase(content.diagramType()) + " diagram", x, y + h - 0.3, w, 0.25, 7.5, false);
    }

    private void addImage(XSLFSlide slide, ImageSearchService.ImageAsset image,
                          double x, double y, double w, double h) {
        try {
            var pictureData = slide.getSlideShow().addPicture(image.bytes(), image.pictureType());
            XSLFPictureShape pic = slide.createPicture(pictureData);
            pic.setAnchor(new Rectangle2D.Double(x * 72, y * 72, w * 72, h * 72));
        } catch (Exception ignored) {
            addVisualFallback(slide, "Image unavailable", x, y, w, h);
        }
    }

    private void addVisualFallback(XSLFSlide slide, String title,
                                   double x, double y, double w, double h) {
        XSLFAutoShape box = slide.createAutoShape();
        box.setShapeType(ShapeType.RECT);
        box.setAnchor(new Rectangle2D.Double(x * 72, y * 72, w * 72, h * 72));
        box.setFillColor(new Color(238, 241, 245));
        box.setLineColor(new Color(190, 198, 210));
        addTextBox(slide, "VISUAL\n" + safe(title, "Topic image"), x + 0.15, y + h / 2 - 0.35, w - 0.3, 0.7, 12, true);
    }

    private void addClosingQASlide(XMLSlideShow ppt) {
        XSLFSlide slide = ppt.createSlide();
        addAccentBar(slide);
        addTextBox(slide, "Questions & Discussion", 0.85, 1.55, 11.7, 0.9, 28, true);
        addTextBox(slide, "What would you challenge, apply, or explore next?", 0.9, 2.75, 11.2, 0.7, 20, false);
        addInteractionPanel(slide,
                new SlideContent("Q&A", "", List.of(), "", "none", "", "none", List.of(),
                        "question", "What is the one takeaway you would act on first?", List.of("Takeaway", "Question", "Idea")),
                1.0, 4.2, 11.2, 1.6);
    }

    private void addAccentBar(XSLFSlide slide) {
        XSLFAutoShape bar = slide.createAutoShape();
        bar.setShapeType(ShapeType.RECT);
        bar.setAnchor(new Rectangle2D.Double(0, 0, 13.333333 * 72, 0.12 * 72));
        bar.setFillColor(new Color(45, 92, 135));
        bar.setLineColor(new Color(45, 92, 135));
    }

    private void addTextBox(XSLFSlide slide, String text, double x, double y, double w, double h,
                            double fontSize, boolean bold) {
        XSLFTextBox box = slide.createTextBox();
        box.setAnchor(new Rectangle2D.Double(x * 72, y * 72, w * 72, h * 72));
        XSLFTextParagraph paragraph = box.addNewTextParagraph();
        paragraph.setTextAlign(org.apache.poi.sl.usermodel.TextParagraph.TextAlign.LEFT);
        XSLFTextRun run = paragraph.addNewTextRun();
        run.setFontSize(fontSize);
        run.setBold(bold);
        run.setText(text == null ? "" : text);
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String titleCase(String text) {
        if (text == null || text.isBlank()) return "Flow";
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }

    private String trimSource(String title) {
        if (title == null) return "image";
        return title.replace("File:", "").replace('_', ' ');
    }
}
