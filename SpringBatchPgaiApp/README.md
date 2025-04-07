# Document Processing with pgAI and Spring Batch

This project implements a document processing system that watches a directory for new or modified files, processes them using pgAI, and stores their embeddings in a PostgreSQL vector database.

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
    url: jdbc:postgresql://localhost:5432/vectordb
    username: postgres
    password: my_pwd
    jpa:
      properties:
        hibernate.jdbc.time_zone: UTC
```
// to load the squad dataset from huggingface
SELECT ai.load_dataset('squad');

// to list the available ollama models
// if running pgai in docker
SELECT ai.ollama_list_models('http://host.docker.internal:11434');

// to manually generate embeddings
select ai.ollama_generate
( 'llama3.2:latest'
, 'what is the typical weather like in Alabama in June'
, host=>'http://host.docker.internal:11434' 
)

SELECT ai.create_vectorizer(
     'public.documents'::regclass,
     destination => 'document_contents_embeddings',
     embedding => ai.embedding_ollama(
        'nomic-embed-text',
        768,
        'http://host.docker.internal:11434' -- Specify the host for docker host name
    ),
     chunking => ai.chunking_recursive_character_text_splitter('content')
);


4. Configure the input and output directories in application.yml:
```yaml
document:
  input:
    directory: c:/data/documents
    polling-interval: 5000  # milliseconds
  output:
    directory: c:/data/documents/processed
    failed-directory: c:/data/documents/failed
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
   - Detect new files
   - Process each file in its own Spring Batch job
   - Store document content and metadata in PostgreSQL
   - Move processed files to the output directory if successful
   - Move failed files to the failed directory if processing fails

## Features

- Automatic file watching and processing
- Single-file-per-job processing for better isolation and reliability
- Centralized file operations through dedicated service
- Batch processing with Spring Batch
- Automatic update of modified documents
- Post-processing file movement with success/failure handling
- Timestamp-based file renaming to prevent conflicts
- Document status tracking using enum (NEW, PROCESSED, FAILED)
- Rich document metadata support with JSON serialization
- Proper handling of timestamps with UTC timezone
- Optimized PostgreSQL array handling for embeddings

## Architecture

### Key Components

- **FileWatcherService**: Monitors the input directory and launches a job for each file
- **FileOperations**: Centralized service for file-related operations (reading content, getting metadata, ensuring directories exist, moving files)
- **DocumentProcessor**: Processes each document and prepares it for storage
- **DocumentReader**: Reads a single file specified by job parameters
- **DocumentWriter**: Writes processed documents to the database and moves processed files based on job success/failure
- **DocumentStatus**: Enum for tracking document processing state
- **DocumentMetadata**: Stores structured metadata about each document

### Processing Flow

1. FileWatcherService polls the input directory at configured intervals
2. For each file, a separate Spring Batch job is launched with the filename as a parameter
3. DocumentReader reads the specified file
4. DocumentProcessor processes the file content and populates metadata
5. DocumentWriter stores the document in the database with appropriate status
6. After processing:
   - If successful: the file is moved to the output directory and document status is set to PROCESSED
   - If failed: the file is moved to the failed directory and document status is set to FAILED

### Data Model

- **Document**: Main entity with fields for content, status, and metadata
  - Uses DocumentStatus enum for status tracking
  - Stores structured metadata as JSON in a JSONB column
  - Properly handles Java 8 date/time types with Jackson JSR310 module
  - Uses PostgreSQL TIMESTAMP WITH TIME ZONE for consistent timestamp handling
  - Uses proper REAL[] type for embeddings array

This architecture ensures that each file is processed independently, providing better isolation and error handling. Moving processed files prevents duplicate processing and maintains a clean workflow.
