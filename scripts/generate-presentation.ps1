param(
    [Parameter(Mandatory=$true)]
    [string]$File,
    [int]$Slides = 8,
    [string]$Output = "generated-presentation.pptx"
)

$ErrorActionPreference = "Stop"

Write-Host "Generating presentation from: $File" -ForegroundColor Cyan
curl.exe -f -X POST "http://localhost:8080/api/presentations/generate?slides=$Slides" `
  -F "file=@$File" `
  -o $Output

Write-Host "Saved presentation to: $Output" -ForegroundColor Green
