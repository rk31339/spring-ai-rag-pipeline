package com.rk.ai.rag.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for document upload operations.
 * Contains processing status and metadata for uploaded documents.
 */
public class UploadResponse {
    
    private String jobId;
    private String status;
    private int totalFiles;
    private int processedFiles;
    private int totalChunks;
    private LocalDateTime uploadedAt;
    private List<DocumentInfo> documents;
    private String message;
    
    public UploadResponse() {
    }
    
    public UploadResponse(String jobId, String status, String message) {
        this.jobId = jobId;
        this.status = status;
        this.message = message;
        this.uploadedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public String getJobId() {
        return jobId;
    }
    
    public void setJobId(String jobId) {
        this.jobId = jobId;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public int getTotalFiles() {
        return totalFiles;
    }
    
    public void setTotalFiles(int totalFiles) {
        this.totalFiles = totalFiles;
    }
    
    public int getProcessedFiles() {
        return processedFiles;
    }
    
    public void setProcessedFiles(int processedFiles) {
        this.processedFiles = processedFiles;
    }
    
    public int getTotalChunks() {
        return totalChunks;
    }
    
    public void setTotalChunks(int totalChunks) {
        this.totalChunks = totalChunks;
    }
    
    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }
    
    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
    
    public List<DocumentInfo> getDocuments() {
        return documents;
    }
    
    public void setDocuments(List<DocumentInfo> documents) {
        this.documents = documents;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    /**
     * Information about each uploaded document.
     */
    public static class DocumentInfo {
        private String documentId;
        private String filename;
        private long fileSize;
        private int chunks;
        private String status;
        private String errorMessage;
        
        public DocumentInfo() {
        }
        
        public DocumentInfo(String documentId, String filename, long fileSize) {
            this.documentId = documentId;
            this.filename = filename;
            this.fileSize = fileSize;
        }
        
        // Getters and Setters
        public String getDocumentId() {
            return documentId;
        }
        
        public void setDocumentId(String documentId) {
            this.documentId = documentId;
        }
        
        public String getFilename() {
            return filename;
        }
        
        public void setFilename(String filename) {
            this.filename = filename;
        }
        
        public long getFileSize() {
            return fileSize;
        }
        
        public void setFileSize(long fileSize) {
            this.fileSize = fileSize;
        }
        
        public int getChunks() {
            return chunks;
        }
        
        public void setChunks(int chunks) {
            this.chunks = chunks;
        }
        
        public String getStatus() {
            return status;
        }
        
        public void setStatus(String status) {
            this.status = status;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
        
        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }
}
