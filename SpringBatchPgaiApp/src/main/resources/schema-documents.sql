-- Create documents table with proper array support for embeddings
DROP TABLE IF EXISTS documents;

CREATE TABLE IF NOT EXISTS documents (
    id BIGSERIAL PRIMARY KEY,
    filename VARCHAR(255) NOT NULL,
    content TEXT,
    file_size BIGINT,
    last_modified TIMESTAMP WITH TIME ZONE,
    status VARCHAR(50) NOT NULL,
    metadata JSONB,
    CONSTRAINT documents_filename_unique UNIQUE (filename)
);
