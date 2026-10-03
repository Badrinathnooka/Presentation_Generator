package com.example.presentationgenerator.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.apache.poi.sl.usermodel.PictureData;

@Service
public class ImageSearchService {
    private final HttpClient httpClient;
    private final JsonMapper jsonMapper;
    private final boolean enabled;
    private final String userAgent;

    public ImageSearchService(
            HttpClient httpClient,
            JsonMapper jsonMapper,
            @Value("${app.visuals.image-enabled:true}") boolean enabled,
            @Value("${app.visuals.user-agent:AI-Presentation-Generator/1.0 (local development)}") String userAgent) {
        this.httpClient = httpClient;
        this.jsonMapper = jsonMapper;
        this.enabled = enabled;
        this.userAgent = userAgent;
    }

    public ImageAsset search(String query) {
        if (!enabled || query == null || query.isBlank()) {
            return null;
        }

        try {
            String encoded = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8);
            String api = "https://commons.wikimedia.org/w/api.php"
                    + "?action=query"
                    + "&generator=search"
                    + "&gsrsearch=" + encoded + " filetype:bitmap"
                    + "&gsrnamespace=6"
                    + "&gsrlimit=5"
                    + "&prop=imageinfo"
                    + "&iiprop=url|mime"
                    + "&iiurlwidth=1100"
                    + "&format=json";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(api))
                    .timeout(Duration.ofSeconds(20))
                    .header("User-Agent", userAgent)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return null;
            }

            JsonNode pages = jsonMapper.readTree(response.body()).path("query").path("pages");
            if (!pages.isObject()) {
                return null;
            }

            var iterator = pages.properties().iterator();
            while (iterator.hasNext()) {
                JsonNode page = iterator.next().getValue();
                JsonNode info = page.path("imageinfo").isArray() && page.path("imageinfo").size() > 0
                        ? page.path("imageinfo").get(0) : null;
                if (info == null) continue;

                String thumb = info.path("thumburl").asText("");
                String source = info.path("descriptionurl").asText("");
                if (thumb.isBlank()) {
                    thumb = info.path("url").asText("");
                }
                if (thumb.isBlank()) continue;

                byte[] bytes = download(thumb);
                if (bytes == null || bytes.length < 1000) continue;

                String title = page.path("title").asText("Wikimedia Commons image");
                String mime = info.path("thumbmime").asText(info.path("mime").asText("image/jpeg"));
                PictureData.PictureType pictureType = mime.toLowerCase().contains("png")
                        ? PictureData.PictureType.PNG
                        : PictureData.PictureType.JPEG;
                return new ImageAsset(title, source, bytes, pictureType);
            }
        } catch (Exception ignored) {
            // Visual enrichment is best-effort. The presentation still succeeds without an image.
        }
        return null;
    }

    private byte[] download(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(25))
                .header("User-Agent", userAgent)
                .GET()
                .build();
        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            return null;
        }
        return response.body();
    }

    public record ImageAsset(String title, String sourceUrl, byte[] bytes, PictureData.PictureType pictureType) {}
}
