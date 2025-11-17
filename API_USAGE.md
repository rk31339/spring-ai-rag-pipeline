# Spring AI RAG Demo - API Usage Guide

This Spring AI RAG (Retrieval Augmented Generation) application provides document ingestion and intelligent query capabilities.

## Prerequisites

1. **PostgreSQL with pgvector extension** running at `localhost:5432`
2. **Local LLM server** (e.g., LM Studio) running at `http://127.0.0.1:1234`
   - Embedding model: `text-embedding-nomic-embed-text-v2-moe`
   - Chat model: `qwen/qwen3-next-80b`

## Architecture

### Components

1. **DocumentIngestionService**: Handles document upload, validation, text extraction, and chunking
2. **DocumentRegistryService**: Tracks uploaded documents, detects duplicates via content hashing
3. **ChunkingService**: Intelligent text chunking with overlapping windows (1500 char chunks)
4. **RagQueryService**: RAG query processing with vector similarity search
5. **VectorStore (PgVector)**: Stores document embeddings for similarity search
6. **EmbeddingModel**: Generates embeddings for documents and queries
7. **ChatClient**: LLM integration for answer generation
8. **Document Readers**: Format-specific text extraction (PDF, Word, Excel, CSV, JSON, Text)

### RAG Flow

**Document Ingestion:**
- Upload documents → Validate → Extract content → Calculate content hash → Check for duplicates → Chunk into segments → Generate embeddings → Store in vector database → Register in document registry

**Query Processing:**
- User query → Generate query embedding → Similarity search in vector store → Retrieve top-K relevant chunks → Build context → Generate answer using LLM

**Duplicate Handling:**
- Same filename + same content → Skip (return cached chunk count)
- Same filename + different content → Delete old chunks, ingest new version (upsert behavior)

## API Endpoints

### 1. Upload Documents

**Endpoint**: `POST /api/documents/upload`

**Content-Type**: `multipart/form-data`

**Parameters:**
- `files`: Array of files
  - **Text formats**: `.txt`, `.md`, `.markdown`
  - **Office documents**: `.pdf`, `.doc`, `.docx`, `.xls`, `.xlsx`
  - **Data formats**: `.csv`, `.json`
- Max file size: 10 MB per file

**Example Request:**
```bash
curl -X POST http://localhost:8080/api/documents/upload \
  -F "files=@document1.md" \
  -F "files=@report.pdf" \
  -F "files=@data.xlsx"
```

**Example Response:**
```json
{
  "jobId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "COMPLETED",
  "message": "Successfully processed all 2 documents (15 chunks)",
  "totalFiles": 2,
  "processedFiles": 2,
  "totalChunks": 15,
  "documents": [...]
}
```

### 2. Query Documents (with LLM)

**Endpoint**: `POST /api/documents/query`

**Content-Type**: `application/json`

**Request Body:**
```json
{
  "query": "What is the main purpose of this system?",
  "topK": 5
}
```

**Example Request:**
```bash
curl -X POST http://localhost:8080/api/documents/query \
  -H "Content-Type: application/json" \
  -d '{"query": "What are the key features?", "topK": 3}'
```

**Example Response:**
```json
{
  "query": "What are the key features?",
  "answer": "Based on the documentation, the key features include...",
  "topK": 3,
  "responseTimeMs": 2345
}
```

### 3. Search Documents (Vector Search Only - No LLM)

**Endpoint**: `POST /api/documents/search`

**Content-Type**: `application/json`

**Description**: Searches the vector database for similar document chunks without LLM processing. Useful for external services that want raw document chunks for their own processing.

**Request Body:**
```json
{
  "query": "vector database",
  "topK": 5,
  "similarityThreshold": 0.3
}
```

**Parameters:**
- `query` (required): Search query string
- `topK` (optional): Number of results to return (default: 5)
- `similarityThreshold` (optional): Minimum similarity score (0.0-1.0, default: 0.3)

**Example Request:**
```bash
curl -X POST http://localhost:8080/api/documents/search \
  -H "Content-Type: application/json" \
  -d '{
    "query": "vector database",
    "topK": 3,
    "similarityThreshold": 0.5
  }'
```

**Example Response:**
```json
{
  "query": "vector database",
  "resultsCount": 3,
  "documents": [
    {
      "content": "PostgreSQL with pgvector extension provides efficient vector storage...",
      "similarityScore": 0.85,
      "metadata": {
        "filename": "database-setup.md",
        "file_size": 5432,
        "file_type": "markdown",
        "document_id": "uuid-here",
        "upload_date": "2024-01-15T10:30:00"
      }
    },
    {
      "content": "Vector embeddings are generated using the embedding model...",
      "similarityScore": 0.72,
      "metadata": {
        "filename": "architecture.pdf",
        "file_size": 102400,
        "file_type": "pdf",
        "document_id": "uuid-here",
        "upload_date": "2024-01-15T11:00:00"
      }
    }
  ]
}
```

**Use Cases:**
- External services that need raw document chunks
- Custom LLM processing pipelines
- Building your own RAG implementation
- Semantic search without answer generation
- Integration with other AI systems

## Running the Application

### 1. Start PostgreSQL with pgvector

```bash
docker run -d --name postgres-pgvector \
  -e POSTGRES_PASSWORD=password \
  -e POSTGRES_DB=rk_db \
  -p 5432:5432 \
  pgvector/pgvector:pg16
```

### 2. Start LM Studio

Load the required models:
- Embedding: `text-embedding-nomic-embed-text-v2-moe`
- Chat: `qwen/qwen3-next-80b`

### 3. Build and run

```bash
./gradlew build
./gradlew bootRun
```

Application starts on `http://localhost:8080`

## Database Schema

The application requires a `document_registry` table. Create it manually:

```sql
CREATE TABLE document_registry (
    document_id UUID PRIMARY KEY,
    filename VARCHAR(500) NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    file_size BIGINT NOT NULL,
    upload_date TIMESTAMP NOT NULL,
    last_modified TIMESTAMP NOT NULL,
    chunk_count INTEGER NOT NULL,
    CONSTRAINT uk_document_registry_filename UNIQUE (filename)
);

CREATE INDEX idx_document_registry_filename ON document_registry(filename);
CREATE INDEX idx_document_registry_content_hash ON document_registry(content_hash);
```

## Project Structure

```
src/main/java/com/rk/ai/rag/
├── config/
│   └── VectorStoreConfig.java              # Spring AI configuration
├── controller/
│   └── DocumentController.java             # REST API endpoints
├── service/
│   ├── DocumentIngestionService.java       # Document processing orchestration
│   ├── DocumentRegistryService.java        # Document tracking & deduplication
│   ├── ChunkingService.java                # Text chunking (1500 char chunks)
│   └── RagQueryService.java                # RAG queries
├── reader/
│   ├── DocumentReader.java                 # Base interface
│   ├── PdfDocumentReader.java              # PDF text extraction
│   ├── WordDocumentReader.java             # Word (.doc, .docx)
│   ├── ExcelDocumentReader.java            # Excel (.xls, .xlsx)
│   ├── CsvDocumentReader.java              # CSV parsing
│   ├── JsonDocumentReader.java             # JSON parsing
│   └── TextDocumentReader.java             # Plain text & markdown
├── model/
│   ├── DocumentRegistry.java               # JPA entity for tracking
│   ├── UploadResponse.java                 # Upload response DTO
│   ├── QueryRequest.java / QueryResponse.java
│   └── SearchRequest.java / SearchResponse.java
├── repository/
│   └── DocumentRegistryRepository.java     # JPA repository
└── exception/
    └── DocumentProcessingException.java    # Stage-aware exceptions
```

## Summary

This RAG implementation provides:
- ✅ Multi-format document ingestion (PDF, Word, Excel, CSV, JSON, Text, Markdown)
- ✅ Document registry with SHA-256 content hashing for duplicate detection
- ✅ Automatic document upsert (replaces old version on content change)
- ✅ Intelligent chunking with semantic boundaries (1500 char chunks, 200 char overlap)
- ✅ Vector embeddings in PostgreSQL/pgvector
- ✅ Similarity search for document retrieval (with/without LLM)
- ✅ LLM-based answer generation with context
- ✅ RESTful API for upload, query, and search
- ✅ Stage-aware error handling and detailed logging
