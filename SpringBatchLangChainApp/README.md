# Document Processing with pgAI and Spring Batch

This project implements a document processing system that watches a directory for new or modified files, processes them using pgAI, and stores their embeddings in a PostgreSQL vector database. It includes robust file management with success/failure handling and document metadata tracking.

## Prerequisites

- Java 17 or higher
- Maven
- PostgreSQL with pgvector extension
- Docker (for PostgreSQL container)

## Setup

1. Ensure your PostgreSQL container is running with the pgvector extension installed.

2. Create the vector extension in your database:
```sql
CREATE EXTENSION IF NOT EXISTS vector;
```

3. Configure the application.yml with your PostgreSQL (pgvector_postgres) connection details:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/langchaindatabase
    username: postgres
    password: my_pwd
```

4. Configure the directories in application.yml:
```yaml
document:
  input:
    directory: c:/data/documents
    polling-interval: 5000  # milliseconds
  output:
    directory: c:/data/processed
    failed-directory: c:/data/failed
```

## Building and Running

1. Build the project:
```bash
mvn clean install
```

2. Run the application:
```bash
mvn spring-boot:run
```

## Usage

1. Place documents in the configured input directory
2. The application will automatically:
   - Detect new or modified files
   - Process them using pgAI
   - Generate embeddings
   - Store them in PostgreSQL with vector search capabilities
   - Move successfully processed files to the output directory
   - Move failed files to the failed directory with appropriate status tracking

## Features

- Automatic file watching and processing
- Document embedding generation using AllMiniLmL6V2
- Vector similarity search support
- Batch processing with Spring Batch
- Automatic update of modified documents
- File management with success/failure handling
- Document status tracking (NEW, PROCESSED, FAILED)
- Structured document metadata storage using JSON

## Vector Embedding Storage

The application implements best practices for storing and retrieving embeddings in PostgreSQL:

### Database Schema

- Uses PostgreSQL's native `vector(384)` type for storing embeddings
- Creates an IVFFlat index for fast similarity search:
  ```sql
  CREATE INDEX IF NOT EXISTS documents_embedding_idx ON documents 
  USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
  ```
- Stores document metadata as JSONB alongside embeddings for better context
- Uses proper timestamp with timezone for consistent date handling
- Tracks document status using an enumerated type

### LangChain4j Integration

- Utilizes `langchain4j-pgvector` for efficient vector operations
- Configures `PgVectorEmbeddingStore` with proper dimension (384) for all-MiniLM-L6-v2 model
- Stores embeddings in both the Document entity and the embedding store

### Semantic Search

- Provides a `DocumentSearchService` for semantic similarity search
- Supports finding relevant documents based on natural language queries
- Includes configurable similarity thresholds and result limits

### Performance Considerations

- Vector operations performed natively in the database
- IVFFlat index dramatically speeds up similarity searches for large collections
- Proper dimensionality ensures compatibility with the embedding model

## Architecture

The application follows a modular architecture:

1. **File Detection**: Monitors input directory for new/modified files
2. **Document Processing**: Extracts content and generates embeddings
3. **Vector Storage**: Stores embeddings in PostgreSQL using pgvector
4. **File Management**: Moves processed files to appropriate directories based on status
5. **Metadata Tracking**: Captures processing details in structured JSON format
6. **Semantic Search**: Enables similarity search across document collection

## Future Enhancements

- REST API for semantic search queries
- Support for additional document formats
- Fine-tuning of vector index parameters for larger collections
- Integration with other embedding models
