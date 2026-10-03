# AI Presentation Generator — Rich Visual Edition

A local Java/Spring Boot presentation generator that turns a text-based PDF/DOCX/TXT into a PowerPoint using Ollama. The richer edition adds best-effort related-image retrieval from Wikimedia Commons, native PowerPoint diagrams, audience interaction panels, speaker notes, and selectable presentation styles.

## Pipeline

```text
Document upload
    |
    v
Apache Tika text extraction
    |
    v
Chunking
    |
    v
Ollama / llama3:latest
    |
    +----> slide content
    +----> visual type + image query
    +----> diagram hints
    +----> audience interaction prompts
    |
    +--> Wikimedia Commons image retrieval (best-effort, no API key)
    |
    v
Apache POI PowerPoint generation
    |
    v
.pptx download
```

## Important

Use **text-based PDFs** for this version. Scanned/image-only PDFs require OCR, which is intentionally not part of this edition.

## Requirements

- Windows 10/11
- Java 21+ (your Java 25 is fine)
- Maven (the setup script can install a project-local copy)
- Ollama running locally
- `llama3:latest` downloaded
- Internet access if you want Wikimedia Commons images

## Ollama

Check:

```cmd
ollama list
```

You should see:

```text
llama3:latest
```

If Ollama is already running, do not run `ollama serve` again.

## Windows setup

From the project root:

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force
Unblock-File .\scripts\setup-windows.ps1
.\scripts\setup-windows.ps1
```

## Run

```powershell
.\scripts\run-windows.ps1
```

Open:

```text
http://localhost:8080
```

## What is new

### Related images

The LLM creates an image search query for appropriate slides. The app calls the Wikimedia Commons API and downloads a thumbnail when a suitable result is available. If the image lookup fails, the presentation still completes with a visual fallback.

Wikimedia Commons' MediaWiki API supports image metadata and thumbnail URLs through `prop=imageinfo` and `iiurlwidth`. Source attribution is added in a small footer on image slides.

### Diagrams

The LLM can request simple native PowerPoint diagrams. The current renderer supports flow/process-style diagrams with 2–6 labeled boxes and arrows. Because these are PowerPoint shapes rather than screenshots, users can edit them after generation.

### Audience interaction

Slides can include:

- Quick polls
- Show-of-hands prompts
- Discussion prompts
- Scenarios
- Audience questions

The renderer displays these as editable cards on the slide. For 6+ slide decks, the pipeline ensures at least two interaction moments when interaction mode is enabled.

### Styles

The browser UI lets you choose:

- Professional
- Technical / Architecture
- Storytelling
- Executive Summary
- Teaching / Classroom

## API

`POST /api/presentations/generate`

Multipart field:

- `file` — PDF/DOCX/TXT/PPTX/etc.

Query parameters:

- `slides` — desired content-slide count, 3–15
- `visuals` — `true`/`false`
- `interactions` — `true`/`false`
- `style` — `professional`, `technical`, `storytelling`, `executive`, or `teaching`

Example:

```powershell
curl.exe -X POST "http://localhost:8080/api/presentations/generate?slides=8&visuals=true&interactions=true&style=technical" -F "file=@sample/sample_document.txt" -o generated.pptx
```

## Free local AI

This project uses Ollama locally. There is no OpenAI API key and no OpenAI billing requirement.

## Image-source note

Images are retrieved from Wikimedia Commons on a best-effort basis. Commons content can have different licenses; the generated deck includes a source footer, but for published/commercial decks you should review the individual file's license and attribution requirements.

## Troubleshooting

### `No readable text was extracted`
The uploaded PDF is likely scanned/image-only. Use a text-based PDF for this version.

### `Generation failed: No Archiver found for the stream signature`
Make sure you are using the project version with Apache Tika 3.2.3 or newer.

### Port 8080 is busy
Stop the process using port 8080 or change `server.port` in `application.properties`.

### Images are not appearing
The presentation still works without images. Check internet connectivity and make sure Wikimedia Commons is reachable from the machine.
