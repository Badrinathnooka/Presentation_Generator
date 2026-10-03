#!/usr/bin/env bash
set -euo pipefail

echo "=== AI Presentation Generator setup ==="
command -v java >/dev/null 2>&1 || { echo "Java 21+ is required."; exit 1; }
command -v mvn >/dev/null 2>&1 || { echo "Maven 3.9+ is required on PATH."; exit 1; }
command -v ollama >/dev/null 2>&1 || { echo "Ollama is required. Install it and make sure the ollama command is on PATH."; exit 1; }

export OLLAMA_BASE_URL="${OLLAMA_BASE_URL:-http://localhost:11434}"
export OLLAMA_MODEL="${OLLAMA_MODEL:-llama3:latest}"

curl -fsS "$OLLAMA_BASE_URL/api/tags" >/dev/null || {
  echo "Ollama is not reachable at $OLLAMA_BASE_URL. Start Ollama and try again."
  exit 1
}

if ! ollama list | awk 'NR>1 {print $1}' | grep -Fxq "$OLLAMA_MODEL"; then
  echo "Model $OLLAMA_MODEL is not installed. Run: ollama pull $OLLAMA_MODEL"
  exit 1
fi

mvn -DskipTests package

echo "Setup complete. Run: mvn spring-boot:run"
echo "No OpenAI API key is required."
