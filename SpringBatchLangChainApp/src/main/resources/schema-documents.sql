-- Create documents table with proper vector support for embeddings
-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- Drop existing table if exists
DROP TABLE IF EXISTS documents;

CREATE TABLE IF NOT EXISTS documents (
    id BIGSERIAL PRIMARY KEY,
    filename VARCHAR(255) NOT NULL,
    content TEXT,
    embedding vector(384),
    file_size BIGINT,
    last_modified TIMESTAMP WITH TIME ZONE,
    status VARCHAR(50) NOT NULL,
    CONSTRAINT documents_filename_unique UNIQUE (filename)
);

-- Create an index for faster vector similarity search
CREATE INDEX IF NOT EXISTS documents_embedding_idx ON documents 
USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
