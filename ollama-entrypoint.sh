#!/bin/sh
set -e

# Start the Ollama server in the background
ollama serve &

# Wait for the server to be ready
sleep 10

# Pull the model
ollama pull llama3

# Keep the container running
exec tail -f /dev/null
