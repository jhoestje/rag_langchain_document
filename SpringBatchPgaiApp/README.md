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
     embedding => ai.embedding_ollama('nomic-embed-text', 384),
     chunking => ai.chunking_recursive_character_text_splitter('content')
);


4. Configure the input directory in application.yml:
```yaml
document:
  input:
    directory: c:/data/documents
    polling-interval: 5000  # milliseconds
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

## Features

- Automatic file watching and processing
- Document embedding generation using AllMiniLmL6V2
- Vector similarity search support
- Batch processing with Spring Batch
- Automatic update of modified documents
