package com.rk.ai.rag.controller;

import com.rk.ai.rag.model.QueryRequest;
import com.rk.ai.rag.model.QueryResponse;
import com.rk.ai.rag.model.SearchRequest;
import com.rk.ai.rag.model.SearchResponse;
import com.rk.ai.rag.model.UploadResponse;
import com.rk.ai.rag.service.DocumentIngestionService;
import com.rk.ai.rag.service.RagQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {
    
    private static final Logger logger = LoggerFactory.getLogger(DocumentController.class);
    
    private final DocumentIngestionService ingestionService;
    private final RagQueryService ragQueryService;
    
    public DocumentController(DocumentIngestionService ingestionService, RagQueryService ragQueryService) {
        this.ingestionService = ingestionService;
        this.ragQueryService = ragQueryService;
    }
    
    @PostMapping("/upload")
    public ResponseEntity<UploadResponse> uploadDocuments(
        @RequestParam("files") MultipartFile[] files
    ) {
        logger.info("POST /api/documents/upload - Received upload request with {} files", 
            files != null ? files.length : 0);
        
        if (files == null || files.length == 0) {
            logger.warn("Upload request rejected: No files provided");
            return ResponseEntity.badRequest()
                .body(new UploadResponse(null, "FAILED", "No files provided"));
        }
        
        // Log file names and sizes
        for (MultipartFile file : files) {
            logger.debug("Uploading file: {} (size: {} bytes)", 
                file.getOriginalFilename(), file.getSize());
        }
        
        UploadResponse response = ingestionService.ingestDocuments(files);
        
        if ("FAILED".equals(response.getStatus())) {
            logger.error("Upload request failed: {}", response.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
        
        logger.info("Upload request completed: status={}, processedFiles={}/{}, totalChunks={}", 
            response.getStatus(), response.getProcessedFiles(), response.getTotalFiles(), 
            response.getTotalChunks());
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/query")
    public ResponseEntity<QueryResponse> query(@RequestBody QueryRequest request) {
        logger.info("POST /api/documents/query - Received query request: '{}' (topK={})", 
            request.getQuery() != null ? request.getQuery().substring(0, Math.min(100, request.getQuery().length())) : "null",
            request.getTopK());
        
        if (request.getQuery() == null || request.getQuery().trim().isEmpty()) {
            logger.warn("Query request rejected: Query is empty");
            return ResponseEntity.badRequest()
                .body(new QueryResponse(null, "Query cannot be empty"));
        }
        
        long startTime = System.currentTimeMillis();
        
        try {
            String answer;
            if (request.getTopK() != null && request.getTopK() > 0) {
                answer = ragQueryService.query(request.getQuery(), request.getTopK());
            } else {
                answer = ragQueryService.query(request.getQuery());
            }
            
            long responseTime = System.currentTimeMillis() - startTime;
            
            QueryResponse response = new QueryResponse(
                request.getQuery(),
                answer,
                request.getTopK(),
                responseTime
            );
            
            logger.info("Query request completed successfully in {}ms, answer length: {} chars", 
                responseTime, answer.length());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            logger.error("Query request failed after {}ms: {}", responseTime, e.getMessage(), e);
            
            QueryResponse errorResponse = new QueryResponse(
                request.getQuery(),
                "Error processing query: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    @PostMapping("/search")
    public ResponseEntity<SearchResponse> search(@RequestBody SearchRequest request) {
        logger.info("POST /api/documents/search - Received vector search request: '{}' (topK={}, threshold={})", 
            request.getQuery() != null ? request.getQuery().substring(0, Math.min(100, request.getQuery().length())) : "null",
            request.getTopK(), request.getSimilarityThreshold());
        
        if (request.getQuery() == null || request.getQuery().trim().isEmpty()) {
            logger.warn("Search request rejected: Query is empty");
            return ResponseEntity.badRequest()
                .body(new SearchResponse("", null));
        }
        
        long startTime = System.currentTimeMillis();
        
        try {
            SearchResponse response = ragQueryService.search(
                request.getQuery(),
                request.getTopK(),
                request.getSimilarityThreshold()
            );
            
            long responseTime = System.currentTimeMillis() - startTime;
            int resultCount = response.getDocuments() != null ? response.getDocuments().size() : 0;
            
            logger.info("Search request completed successfully in {}ms, returned {} document chunks", 
                responseTime, resultCount);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            logger.error("Search request failed after {}ms: {}", responseTime, e.getMessage(), e);
            
            return ResponseEntity.internalServerError()
                .body(new SearchResponse(request.getQuery(), null));
        }
    }
}
