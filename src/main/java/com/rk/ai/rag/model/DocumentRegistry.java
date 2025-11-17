package com.rk.ai.rag.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a document in the registry.
 * Tracks uploaded documents to prevent duplicates and manage updates.
 */
@Entity
@Table(name = "document_registry", 
       uniqueConstraints = @UniqueConstraint(columnNames = "filename"))
public class DocumentRegistry {
    
    @Id
    private UUID documentId;
    
    @Column(nullable = false, length = 500)
    private String filename;
    
    @Column(nullable = false, length = 64)
    private String contentHash;
    
    @Column(nullable = false)
    private Long fileSize;
    
    @Column(nullable = false)
    private LocalDateTime uploadDate;
    
    @Column(nullable = false)
    private LocalDateTime lastModified;
    
    @Column(nullable = false)
    private Integer chunkCount;
    
    // Constructors
    public DocumentRegistry() {
    }
    
    public DocumentRegistry(UUID documentId, String filename, String contentHash, Long fileSize, Integer chunkCount) {
        this.documentId = documentId;
        this.filename = filename;
        this.contentHash = contentHash;
        this.fileSize = fileSize;
        this.chunkCount = chunkCount;
        this.uploadDate = LocalDateTime.now();
        this.lastModified = LocalDateTime.now();
    }
    
    // Getters and Setters
    public UUID getDocumentId() {
        return documentId;
    }
    
    public void setDocumentId(UUID documentId) {
        this.documentId = documentId;
    }
    
    public String getFilename() {
        return filename;
    }
    
    public void setFilename(String filename) {
        this.filename = filename;
    }
    
    public String getContentHash() {
        return contentHash;
    }
    
    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }
    
    public Long getFileSize() {
        return fileSize;
    }
    
    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }
    
    public LocalDateTime getUploadDate() {
        return uploadDate;
    }
    
    public void setUploadDate(LocalDateTime uploadDate) {
        this.uploadDate = uploadDate;
    }
    
    public LocalDateTime getLastModified() {
        return lastModified;
    }
    
    public void setLastModified(LocalDateTime lastModified) {
        this.lastModified = lastModified;
    }
    
    public Integer getChunkCount() {
        return chunkCount;
    }
    
    public void setChunkCount(Integer chunkCount) {
        this.chunkCount = chunkCount;
    }
    
    @PreUpdate
    public void preUpdate() {
        this.lastModified = LocalDateTime.now();
    }
}
