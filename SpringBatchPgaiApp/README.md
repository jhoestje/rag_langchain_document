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

3. Configure the application.yml with your PostgreSQL connection details:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/vectordb
    username: postgres
    password: postgres
```

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
