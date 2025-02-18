# RAG Implementation with LangChain and Chroma

This repository contains a Retrieval-Augmented Generation (RAG) implementation using LangChain and Chroma vector store. The application processes documents, creates embeddings, and enables conversational question-answering based on the document content.

## Features

- Document processing with chunking and overlap
- Vector storage using Chroma DB
- Conversational memory for context retention
- Detailed logging of LLM interactions
- Graceful handling of empty document scenarios
- Interactive query interface

## Prerequisites

- Python 3.x
- Ollama (for LLM and embeddings)

## Setup

1. Clone the repository:
   ```bash
   git clone https://github.com/jhoestje/rag_langchain_document.git
   cd rag_langchain_document
   ```

2. Create and activate a virtual environment:
   ```bash
   # Windows
   python -m venv venv
   .\venv\Scripts\activate

   # Linux/Mac
   python3 -m venv venv
   source venv/bin/activate
   ```

3. Install required packages:
   ```bash
   pip install langchain langchain-community langchain-chroma langchain-ollama
   ```

4. Verify Ollama is running and the required model is available:
   ```bash
   # Check if the model exists
   ollama list
   
   # If needed, pull the model
   ollama pull llama3.2
   ```

## Project Structure

- `app.py` - Main application using Chroma vector store
- `app_mongo.py` - Alternative implementation using MongoDB (for reference)
- `documents/` - Directory for source documents (add your .txt files here)

## Configuration

1. Set up your document directory:
   - Place your .txt files in the `documents/` directory
   - The application will process all .txt files in this directory

2. Configure the LLM:
   - The application uses Ollama's llama3.2 model by default
   - You can modify the `MODEL` constant in `app.py` to use a different model

3. Chroma Settings:
   - Vector store data is persisted in `./chroma_db/`
   - Document chunks are created with:
     - Chunk size: 1000 characters
     - Chunk overlap: 200 characters

## Usage

1. Start the application:
   ```bash
   python app.py
   ```

2. Enter your queries when prompted
   - Type 'exit' to quit the application
   - The system will retrieve relevant document chunks and provide contextual answers

## Logging

The application includes comprehensive logging:
- Document processing status
- LLM interactions
- Query and response tracking
- Vector store operations

## Error Handling

The application includes robust error handling for:
- Empty document directories
- Document processing failures
- LLM and vector store interactions

## Contributing

Feel free to submit issues, fork the repository, and create pull requests for any improvements.

## License

This project is open source and available under the [MIT License](LICENSE).
