package com.rk.ai.rag.service;

import com.rk.ai.rag.model.DocumentRegistry;
import com.rk.ai.rag.repository.DocumentRegistryRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing document registry operations.
 * Handles document tracking, duplicate detection, and cleanup.
 */
@Service
public class DocumentRegistryService {
    
    private static final Logger logger = LoggerFactory.getLogger(DocumentRegistryService.class);
    
    private final DocumentRegistryRepository repository;
    private final VectorStore vectorStore;
    
    public DocumentRegistryService(DocumentRegistryRepository repository, VectorStore vectorStore) {
        this.repository = repository;
        this.vectorStore = vectorStore;
    }
    
    /**
     * Find a document by filename.
     */
    public Optional<DocumentRegistry> findByFilename(String filename) {
        return repository.findByFilename(filename);
    }
    
    /**
     * Check if a document exists.
     */
    public boolean exists(String filename) {
        return repository.existsByFilename(filename);
    }
    
    /**
     * Register a new document or update existing one.
     */
    @Transactional
    public DocumentRegistry register(UUID documentId, String filename, String contentHash, 
                                     Long fileSize, Integer chunkCount) {
        
        Optional<DocumentRegistry> existing = repository.findByFilename(filename);
        
        if (existing.isPresent()) {
            // Update existing entry
            DocumentRegistry doc = existing.get();
            doc.setContentHash(contentHash);
            doc.setFileSize(fileSize);
            doc.setChunkCount(chunkCount);
            
            logger.info("Updated registry entry for document: {}", filename);
            return repository.save(doc);
        } else {
            // Create new entry
            DocumentRegistry doc = new DocumentRegistry(documentId, filename, contentHash, 
                                                       fileSize, chunkCount);
            logger.info("Registered new document: {} with ID: {}", filename, documentId);
            return repository.save(doc);
        }
    }
    
    /**
     * Delete document from registry and vector store.
     * 
     * @param documentId The document ID to delete
     * @param filename The filename for logging
     */
    @Transactional
    public void deleteDocument(UUID documentId, String filename) {
        try {
            // Delete from vector store first
            logger.info("Deleting {} chunks from vector store for document: {}", 
                       documentId, filename);
            vectorStore.delete(java.util.List.of(documentId.toString()));
            
            // Delete from registry
            repository.deleteById(documentId);
            logger.info("Deleted document from registry: {}", filename);
            
        } catch (Exception e) {
            logger.error("Failed to delete document: {}", filename, e);
            throw new RuntimeException("Failed to delete document: " + filename, e);
        }
    }
    
    /**
     * Calculate deterministic document ID from filename.
     * Ensures the same filename always gets the same ID.
     */
    public UUID generateDocumentId(String filename) {
        return UUID.nameUUIDFromBytes(filename.getBytes());
    }
}
