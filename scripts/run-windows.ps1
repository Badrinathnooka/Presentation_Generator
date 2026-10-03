$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot

if (-not (Get-Command ollama -ErrorAction SilentlyContinue)) {
    throw "Ollama was not found on PATH. Install Ollama, then reopen Command Prompt/PowerShell."
}

$env:OLLAMA_BASE_URL = if ($env:OLLAMA_BASE_URL) { $env:OLLAMA_BASE_URL } else { "http://localhost:11434" }
$env:OLLAMA_MODEL = if ($env:OLLAMA_MODEL) { $env:OLLAMA_MODEL } else { "llama3:latest" }

Write-Host "Checking Ollama at $env:OLLAMA_BASE_URL" -ForegroundColor Cyan
try {
    $null = Invoke-RestMethod -Uri "$env:OLLAMA_BASE_URL/api/tags" -Method Get -TimeoutSec 10
} catch {
    throw "Ollama is not reachable at $env:OLLAMA_BASE_URL. Start Ollama and run again."
}

$modelNames = (ollama list | Select-Object -Skip 1 | ForEach-Object { ($_ -split '\s+')[0] })
if ($modelNames -notcontains $env:OLLAMA_MODEL) {
    throw "Ollama model '$env:OLLAMA_MODEL' is not installed. Run: ollama pull $env:OLLAMA_MODEL"
}

if (Get-Command mvn -ErrorAction SilentlyContinue) {
    $mvnExe = (Get-Command mvn).Source
} else {
    $mvnExe = Join-Path $projectRoot ".tools\apache-maven-3.10.0\bin\mvn.cmd"
    if (-not (Test-Path $mvnExe)) {
        throw "Maven was not found. Run .\scripts\setup-windows.ps1 first."
    }
}

Write-Host "Starting AI Presentation Generator on http://localhost:8080" -ForegroundColor Cyan
Write-Host "Ollama model: $env:OLLAMA_MODEL" -ForegroundColor DarkGray

Push-Location $projectRoot
try {
    & $mvnExe spring-boot:run
} finally {
    Pop-Location
}
