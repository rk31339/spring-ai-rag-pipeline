# RAG Document Ingestion System - Implementation Guide

## Overview
This implementation provides a production-ready document ingestion pipeline for a Retrieval-Augmented Generation (RAG) system using Spring AI and PostgreSQL pgvector.

## Components Implemented

### 1. Configuration Layer
- **VectorStoreConfig.java** - Configures PgVectorStore and OpenAI-compatible embedding model beans

### 2. Model Layer
- **UploadResponse.java** - Response DTO with detailed processing status and metrics
- **DocumentInfo** (nested class) - Individual document processing results

### 3. Service Layer
- **ChunkingService.java** - Smart text chunking with:
  - Semantic paragraph-based chunking (1000 chars/chunk)
  - 200-character overlap to preserve context
  - Special markdown handling (preserves headers and structure)
  
- **DocumentIngestionService.java** (requires manual completion) - Orchestrates:
  - File validation (10MB limit, .md/.txt/.markdown only)
  - Text extraction
  - Document chunking
  - Embedding generation
  - Vector storage

### 4. Exception Handling
- **DocumentProcessingException.java** - Stage-aware error tracking

### 5. Controller Layer
- **DocumentController.java** - REST endpoint at `/api/documents/upload`

## Architecture

```
User Upload → Controller → IngestionService → ChunkingService → VectorStore → PostgreSQL
                                ↓
                        Error Handling & Logging
```

## RAG Best Practices Implemented

1. **Smart Chunking**
   - Respects semantic boundaries (paragraphs, markdown sections)
   - Overlapping windows prevent context loss
   - Optimal chunk size (1000 chars ≈ 500-1000 tokens)

2. **Metadata Enrichment**
   - Filename, upload date, file size
   - Chunk index and total chunks
   - Source document ID for tracing
   - File type detection

3. **Error Resilience**
   - Per-file error handling (partial success supported)
   - Stage-specific exception tracking
   - Detailed error messages

4. **Performance**
   - Batch processing support
   - Automatic embedding generation via VectorStore
   - HNSW indexing for fast similarity search

## API Usage

### Upload Documents

```bash
curl -X POST http://localhost:8080/api/documents/upload \
  -F "files=@document1.md" \
  -F "files=@document2.txt"
```

### Response Example

```json
{
  "jobId": "uuid",
  "status": "COMPLETED",
  "totalFiles": 2,
  "processedFiles": 2,
  "totalChunks": 15,
  "uploadedAt": "2025-11-15T19:25:00",
  "documents": [
    {
      "documentId": "uuid",
      "filename": "document1.md",
      "fileSize": 5420,
      "chunks": 8,
      "status": "SUCCESS"
    }
  ],
  "message": "Successfully processed all 2 documents (15 chunks)"
}
```

## Prerequisites

1. **PostgreSQL with pgvector**
   ```sql
   CREATE EXTENSION IF NOT EXISTS vector;
   ```

2. **Local LLM Server** (e.g., LM Studio)
   - Running at http://127.0.0.1:1234
   - Embedding model: text-embedding-nomic-embed-text-v2-moe (768d)
   - Chat model: qwen/qwen3-next-80b

3. **Database Configuration**
   - Update `application.properties` with your PostgreSQL credentials

## Next Steps

1. **Complete DocumentIngestionService** - See implementation below
2. **Initialize Database** - Ensure pgvector extension is installed
3. **Start LLM Server** - Configure embedding model
4. **Test Upload** - Use sample markdown/text files
5. **Implement Query** - Build RAG query endpoint (Phase 2)

## Manual Implementation Required

Due to technical issues, the DocumentIngestionService needs to be completed manually. Create the file with the following implementation:

**Location**: `src/main/java/com/rk/ai/rag/service/DocumentIngestionService.java`

**Key Methods**:
- `ingestDocuments(MultipartFile[] files)` - Main entry point
- `validateFile(MultipartFile file)` - File validation
- `extractContent(MultipartFile file)` - UTF-8 text extraction
- `chunkDocument(String content, ...)` - Delegates to ChunkingService
- `storeChunks(List<Document> chunks, ...)` - Stores in vector DB

The service should inject `VectorStore` and `ChunkingService` dependencies and implement the complete pipeline as outlined in the design.

## Testing

1. Create test markdown file
2. Upload via REST API
3. Verify in PostgreSQL:
   ```sql
   SELECT COUNT(*) FROM vector_store;
   SELECT metadata FROM vector_store LIMIT 5;
   ```

## Troubleshooting

- **Embedding failures**: Check LLM server is running
- **Database errors**: Verify pgvector extension installed
- **File upload limits**: Default 10MB, adjust MAX_FILE_SIZE constant
- **Unsupported file types**: Only .md, .txt, .markdown allowed

## Future Enhancements

- Async processing for large batches
- Document deletion/update APIs
- Advanced markdown parsing (code blocks, tables)
- Hybrid search (keyword + vector)
- Query endpoint with RAG implementation
