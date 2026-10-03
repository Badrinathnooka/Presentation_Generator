$ErrorActionPreference = "Stop"

Write-Host "=== AI Presentation Generator setup ===" -ForegroundColor Cyan

function Test-Command($name) {
    return $null -ne (Get-Command $name -ErrorAction SilentlyContinue)
}

# ------------------------------------------------------------
# Java
# ------------------------------------------------------------
if (-not (Test-Command "java")) {
    throw "Java was not found. Install a JDK 21+ and run this script again."
}

$javaVersionOutput = (& java --version 2>&1 | Out-String).Trim()
$javaFirstLine = ($javaVersionOutput -split "`r?`n" | Select-Object -First 1).Trim()
Write-Host "Java: $javaFirstLine" -ForegroundColor Green

if ($javaFirstLine -notmatch 'java (?:version )?([0-9]+)') {
    throw "Could not determine the installed Java major version. Run 'java --version' manually."
}

$javaMajor = [int]$Matches[1]
Write-Host "Java major version: $javaMajor" -ForegroundColor Green
if ($javaMajor -lt 21) {
    throw "This project requires JDK 21 or newer. Detected Java $javaMajor."
}

# ------------------------------------------------------------
# Ollama
# ------------------------------------------------------------
if (-not (Test-Command "ollama")) {
    throw "Ollama was not found on PATH. Install Ollama, reopen PowerShell, and run this script again."
}

$env:OLLAMA_BASE_URL = if ($env:OLLAMA_BASE_URL) { $env:OLLAMA_BASE_URL } else { "http://localhost:11434" }
$env:OLLAMA_MODEL = if ($env:OLLAMA_MODEL) { $env:OLLAMA_MODEL } else { "llama3:latest" }
Write-Host "Ollama model: $env:OLLAMA_MODEL" -ForegroundColor Green
try {
    $null = Invoke-RestMethod -Uri "$env:OLLAMA_BASE_URL/api/tags" -Method Get -TimeoutSec 10
} catch {
    throw "Ollama is not reachable at $env:OLLAMA_BASE_URL. Start Ollama, verify with 'ollama list', and run again."
}

$modelNames = (ollama list | Select-Object -Skip 1 | ForEach-Object { ($_ -split '\s+')[0] })
if ($modelNames -notcontains $env:OLLAMA_MODEL) {
    throw "Ollama model '$env:OLLAMA_MODEL' is not installed. Run: ollama pull $env:OLLAMA_MODEL"
}

# ------------------------------------------------------------
# Maven
# Prefer a globally installed Maven. If it is missing, download
# an official Apache Maven binary locally into .tools.
# No admin rights and no winget package are required.
# ------------------------------------------------------------
$projectRoot = Split-Path -Parent $PSScriptRoot
$localMavenVersion = "3.10.0"
$localMavenHome = Join-Path $projectRoot ".tools\apache-maven-$localMavenVersion"
$localMavenExe = Join-Path $localMavenHome "bin\mvn.cmd"

if (Test-Command "mvn") {
    $mvnExe = (Get-Command "mvn").Source
    Write-Host "Maven: using system Maven at $mvnExe" -ForegroundColor Green
} else {
    if (-not (Test-Path $localMavenExe)) {
        $toolsDir = Join-Path $projectRoot ".tools"
        New-Item -ItemType Directory -Force -Path $toolsDir | Out-Null

        $zipPath = Join-Path $toolsDir "apache-maven-$localMavenVersion-bin.zip"
        $url = "https://dlcdn.apache.org/maven/maven-3/$localMavenVersion/binaries/apache-maven-$localMavenVersion-bin.zip"

        Write-Host "Maven was not found. Downloading Apache Maven $localMavenVersion..." -ForegroundColor Yellow
        try {
            Invoke-WebRequest -Uri $url -OutFile $zipPath -UseBasicParsing
        } catch {
            throw "Could not download Maven from $url. Check your internet connection and run the script again. Error: $($_.Exception.Message)"
        }

        Write-Host "Extracting Maven..." -ForegroundColor Yellow
        Expand-Archive -Path $zipPath -DestinationPath $toolsDir -Force
        Remove-Item $zipPath -Force
    }

    if (-not (Test-Path $localMavenExe)) {
        throw "Maven extraction completed, but $localMavenExe was not found."
    }

    $mvnExe = $localMavenExe
    Write-Host "Maven: using project-local Maven at $mvnExe" -ForegroundColor Green
}

Write-Host "Maven version:" -ForegroundColor Green
& $mvnExe -version
if ($LASTEXITCODE -ne 0) {
    throw "Maven could not start successfully."
}

# ------------------------------------------------------------
# Build
# ------------------------------------------------------------
Push-Location $projectRoot
try {
    Write-Host "Downloading/building Java dependencies..." -ForegroundColor Green
    & $mvnExe -DskipTests dependency:go-offline
    if ($LASTEXITCODE -ne 0) { throw "Maven dependency download failed." }

    Write-Host "Building the application..." -ForegroundColor Green
    & $mvnExe -DskipTests package
    if ($LASTEXITCODE -ne 0) { throw "Maven build failed." }
} finally {
    Pop-Location
}

Write-Host "" 
Write-Host "Setup complete." -ForegroundColor Green
Write-Host "Next steps:" -ForegroundColor Cyan
Write-Host "  1. Verify Ollama: ollama list"
Write-Host "  2. .\scripts\run-windows.ps1"
Write-Host "  3. Open http://localhost:8080"
