$ErrorActionPreference = "Stop"

if (-not (Get-Command ollama -ErrorAction SilentlyContinue)) {
    throw "Ollama was not found on PATH."
}

$baseUrl = if ($env:OLLAMA_BASE_URL) { $env:OLLAMA_BASE_URL } else { "http://localhost:11434" }
$model = if ($env:OLLAMA_MODEL) { $env:OLLAMA_MODEL } else { "llama3:latest" }

Write-Host "Ollama URL: $baseUrl" -ForegroundColor Cyan
Write-Host "Model: $model" -ForegroundColor Cyan

try {
    Invoke-RestMethod -Uri "$baseUrl/api/tags" -Method Get -TimeoutSec 10 | Out-Null
} catch {
    throw "Ollama is not reachable. Start Ollama and try again."
}

ollama list
$modelNames = (ollama list | Select-Object -Skip 1 | ForEach-Object { ($_ -split '\s+')[0] })
if ($modelNames -notcontains $model) {
    throw "Model '$model' is not installed. Run: ollama pull $model"
}

Write-Host "Ollama is ready for the presentation generator." -ForegroundColor Green
