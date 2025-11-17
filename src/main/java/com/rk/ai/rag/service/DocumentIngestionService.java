package com.rk.ai.rag.service;

import com.rk.ai.rag.exception.DocumentProcessingException;
import com.rk.ai.rag.exception.DocumentProcessingException.ProcessingStage;
import com.rk.ai.rag.model.DocumentRegistry;
import com.rk.ai.rag.model.UploadResponse;
import com.rk.ai.rag.reader.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class DocumentIngestionService {
    
    private static final Logger logger = LoggerFactory.getLogger(DocumentIngestionService.class);
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    private static final List<String> ALLOWED_EXTENSIONS = List.of(
        ".txt", ".md", ".markdown",  // Text files
        ".pdf",                       // PDF files
        ".doc", ".docx",             // Word files
        ".xls", ".xlsx",             // Excel files
        ".csv",                      // CSV files
        ".json"                      // JSON files
    );
    
    private final VectorStore vectorStore;
    private final ChunkingService chunkingService;
    private final DocumentRegistryService documentRegistryService;
    private final PdfDocumentReader pdfReader;
    private final WordDocumentReader wordReader;
    private final ExcelDocumentReader excelReader;
    private final CsvDocumentReader csvReader;
    private final JsonDocumentReader jsonReader;
    private final TextDocumentReader textReader;
    
    public DocumentIngestionService(
            VectorStore vectorStore, 
            ChunkingService chunkingService,
            DocumentRegistryService documentRegistryService,
            PdfDocumentReader pdfReader,
            WordDocumentReader wordReader,
            ExcelDocumentReader excelReader,
            CsvDocumentReader csvReader,
            JsonDocumentReader jsonReader,
            TextDocumentReader textReader) {
        this.vectorStore = vectorStore;
        this.chunkingService = chunkingService;
        this.documentRegistryService = documentRegistryService;
        this.pdfReader = pdfReader;
        this.wordReader = wordReader;
        this.excelReader = excelReader;
        this.csvReader = csvReader;
        this.jsonReader = jsonReader;
        this.textReader = textReader;
    }
    
    public UploadResponse ingestDocuments(MultipartFile[] files) {
        String jobId = UUID.randomUUID().toString();
        logger.info("Starting document ingestion job: {}", jobId);
        
        UploadResponse response = new UploadResponse(jobId, "PROCESSING", "Document ingestion started");
        response.setTotalFiles(files.length);
        
        List<UploadResponse.DocumentInfo> documentInfos = new ArrayList<>();
        int processedCount = 0;
        int totalChunks = 0;
        
        for (MultipartFile file : files) {
            UploadResponse.DocumentInfo docInfo = new UploadResponse.DocumentInfo(
                UUID.randomUUID().toString(),
                file.getOriginalFilename(),
                file.getSize()
            );
            
            try {
                validateFile(file);
                int chunks = processDocument(file, docInfo.getDocumentId());
                
                docInfo.setChunks(chunks);
                docInfo.setStatus("SUCCESS");
                processedCount++;
                totalChunks += chunks;
                
                logger.info("Successfully processed document: {} ({} chunks)", 
                    file.getOriginalFilename(), chunks);
                
            } catch (DocumentProcessingException e) {
                logger.error("Failed to process document: {}", file.getOriginalFilename(), e);
                docInfo.setStatus("FAILED");
                docInfo.setErrorMessage(e.getMessage());
                
            } catch (Exception e) {
                logger.error("Unexpected error processing document: {}", file.getOriginalFilename(), e);
                docInfo.setStatus("FAILED");
                docInfo.setErrorMessage("Unexpected error: " + e.getMessage());
            }
            
            documentInfos.add(docInfo);
        }
        
        response.setProcessedFiles(processedCount);
        response.setTotalChunks(totalChunks);
        response.setDocuments(documentInfos);
        
        if (processedCount == files.length) {
            response.setStatus("COMPLETED");
            response.setMessage(String.format("Successfully processed all %d documents (%d chunks)", 
                processedCount, totalChunks));
        } else if (processedCount == 0) {
            response.setStatus("FAILED");
            response.setMessage("Failed to process any documents");
        } else {
            response.setStatus("PARTIAL_SUCCESS");
            response.setMessage(String.format("Processed %d/%d documents (%d chunks)", 
                processedCount, files.length, totalChunks));
        }
        
        logger.info("Completed ingestion job {}: {}", jobId, response.getStatus());
        return response;
    }
    
    private int processDocument(MultipartFile file, String documentId) {
        String filename = file.getOriginalFilename();
        
        try {
            // Extract content first
            String content = extractContent(file);
            
            // Calculate content hash for duplicate detection
            String contentHash = calculateHash(content);
            
            // Check for existing document with same filename
            Optional<DocumentRegistry> existingDoc = documentRegistryService.findByFilename(filename);
            
            if (existingDoc.isPresent()) {
                DocumentRegistry existing = existingDoc.get();
                
                // Check if content has changed
                if (contentHash.equals(existing.getContentHash())) {
                    logger.info("Document {} already exists with same content hash. Skipping ingestion.", filename);
                    return existing.getChunkCount();
                }
                
                // Content has changed - delete old chunks before adding new ones (upsert)
                logger.info("Document {} exists but content has changed. Updating...", filename);
                documentRegistryService.deleteDocument(existing.getDocumentId(), filename);
            } else {
                logger.info("New document {}. Processing...", filename);
            }
            
            // Process the document
            Map<String, Object> metadata = createMetadata(file, documentId);
            metadata.put("content_hash", contentHash);
            
            List<Document> chunks = chunkDocument(content, filename, metadata);
            storeChunks(chunks, filename);

            // Register the document in the registry
            documentRegistryService.register(
                UUID.fromString(documentId),
                filename,
                contentHash,
                file.getSize(),
                chunks.size()
            );

            logger.info("Successfully processed and registered document: {} ({} chunks)", filename, chunks.size());
            return chunks.size();

        } catch (IOException e) {
            logger.error("IO error processing document: {}", filename, e);
            throw new DocumentProcessingException("Failed to read document", filename, ProcessingStage.FILE_READING, e);
        } catch (Exception e) {
            logger.error("Error processing document: {}", filename, e);
            throw new DocumentProcessingException("Failed to process document", filename, ProcessingStage.CHUNKING, e);
        }
    }

    private void validateFile(MultipartFile file) {
        String filename = file.getOriginalFilename();
        final String filenameForError = (filename != null) ? filename : "unknown";

        if (file.isEmpty()) {
            throw new DocumentProcessingException("File is empty", filenameForError, ProcessingStage.FILE_READING);
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new DocumentProcessingException(
                String.format("File size exceeds maximum allowed size of %d MB", MAX_FILE_SIZE / 1024 / 1024),
                filenameForError,
                ProcessingStage.FILE_READING
            );
        }

        if (filename == null || filename.isBlank()) {
            throw new DocumentProcessingException("Invalid filename", filenameForError, ProcessingStage.FILE_READING);
        }

        final String filenameLower = filename.toLowerCase();
        boolean hasValidExtension = ALLOWED_EXTENSIONS.stream()
            .anyMatch(ext -> filenameLower.endsWith(ext));

        if (!hasValidExtension) {
            throw new DocumentProcessingException(
                "Unsupported file type. Allowed types: " + String.join(", ", ALLOWED_EXTENSIONS),
                filenameForError,
                ProcessingStage.FILE_READING
            );
        }
    }

    private String extractContent(MultipartFile file) throws Exception {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new DocumentProcessingException("Invalid filename", "unknown", ProcessingStage.FILE_READING);
        }

        String extension = filename.substring(filename.lastIndexOf('.')).toLowerCase();

        return switch (extension) {
            case ".pdf" -> pdfReader.extractText(file.getInputStream(), filename);
            case ".doc", ".docx" -> wordReader.extractText(file.getInputStream(), filename);
            case ".xls", ".xlsx" -> excelReader.extractText(file.getInputStream(), filename);
            case ".csv" -> csvReader.extractText(file.getInputStream(), filename);
            case ".json" -> jsonReader.extractText(file.getInputStream(), filename);
            case ".txt", ".md", ".markdown" -> textReader.extractText(file.getInputStream(), filename);
            default -> throw new DocumentProcessingException("Unsupported file type: " + extension, filename, ProcessingStage.TEXT_EXTRACTION);
        };
    }

    private String calculateHash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new DocumentProcessingException("Failed to calculate content hash", "unknown", ProcessingStage.TEXT_EXTRACTION, e);
        }
    }

    private Map<String, Object> createMetadata(MultipartFile file, String documentId) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("document_id", documentId);
        metadata.put("filename", file.getOriginalFilename());
        metadata.put("file_size", file.getSize());
        metadata.put("content_type", file.getContentType());
        metadata.put("upload_timestamp", LocalDateTime.now().toString());
        return metadata;
    }

    private List<Document> chunkDocument(String content, String filename, Map<String, Object> metadata) {
        String documentId = (String) metadata.get("document_id");
        return chunkingService.chunkDocument(content, metadata, documentId);
    }

    private void storeChunks(List<Document> chunks, String filename) {
        if (chunks.isEmpty()) {
            logger.warn("No chunks generated for document: {}", filename);
            return;
        }

        logger.info("Storing {} chunks for document: {}", chunks.size(), filename);
        vectorStore.add(chunks);
    }
}
