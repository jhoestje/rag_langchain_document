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

- `document_app.py` - Main application using Chroma vector store
- `document_app_mongo.py` - Alternative implementation using MongoDB (for reference)
- `stock_agent.py` - Stock market information agent
- `tavily_agent.py` - Web search agent using Tavily
- `documents/` - Directory for source documents (add your .txt files here)

## Configuration

1. Set up your document directory:
   - Place your .txt files in the `documents/` directory
   - The application will process all .txt files in this directory

2. Configure the LLM:
   - The application uses Ollama's llama3.2 model by default
   - You can modify the `MODEL` constant in `document_app.py` to use a different model

3. Chroma Settings:
   - Vector store data is persisted in `./chroma_db/`
   - Document chunks are created with:
     - Chunk size: 1000 characters
     - Chunk overlap: 200 characters

## Usage

1. Start the application:
   ```bash
   python document_app.py
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

## Additional Agents

### Stock Market Agent

The repository includes a stock market agent that provides real-time stock information and analysis:

- Real-time stock data using Alpha Vantage API
- Stock price queries and analysis
- Historical data retrieval
- Market insights and trends
- LangSmith integration for monitoring and debugging

#### Stock Agent Setup

1. Get an API key from [Alpha Vantage](https://www.alphavantage.co/support/#api-key)
2. Add to your `.env` file:
   ```bash
   ALPHA_VANTAGE_API_KEY=your_api_key_here
   ```

### Tavily Search Agent

A search agent powered by Tavily's AI-optimized search engine:

- Real-time web search capabilities
- AI-optimized search results
- Factual and accurate information retrieval
- Integrated with LangSmith for performance tracking

#### Tavily Agent Setup

1. Get an API key from [Tavily](https://tavily.com)
2. Add to your `.env` file:
   ```bash
   TAVILY_API_KEY=your_api_key_here
   ```

### LangSmith Integration

Both agents include LangSmith integration for advanced monitoring and debugging:

1. Get a LangSmith API key from [LangSmith](https://smith.langchain.com)
2. Add to your `.env` file:
   ```bash
   LANGSMITH_API_KEY=your_api_key_here
   ```
3. Environment variables are automatically configured in both agents:
   - LANGSMITH_TRACING=true
   - LANGSMITH_ENDPOINT="https://api.smith.langchain.com"
   - Project names: "stock_agent_poc" and "tavily_poc"

## Additional Dependencies

Install the required packages for the new agents:
```bash
pip install tavily-python langchain-community python-dotenv requests
```

## Contributing

Feel free to submit issues, fork the repository, and create pull requests for any improvements.

## License

This project is open source and available under the [MIT License](LICENSE).
