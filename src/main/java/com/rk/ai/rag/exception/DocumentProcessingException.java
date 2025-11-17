package com.rk.ai.rag.exception;

/**
 * Custom exception for document processing errors in the RAG pipeline.
 */
public class DocumentProcessingException extends RuntimeException {
    
    private final String documentName;
    private final ProcessingStage stage;
    
    public enum ProcessingStage {
        FILE_READING,
        TEXT_EXTRACTION,
        CHUNKING,
        EMBEDDING_GENERATION,
        VECTOR_STORAGE
    }
    
    public DocumentProcessingException(String message, String documentName, ProcessingStage stage) {
        super(message);
        this.documentName = documentName;
        this.stage = stage;
    }
    
    public DocumentProcessingException(String message, String documentName, ProcessingStage stage, Throwable cause) {
        super(message, cause);
        this.documentName = documentName;
        this.stage = stage;
    }
    
    public String getDocumentName() {
        return documentName;
    }
    
    public ProcessingStage getStage() {
        return stage;
    }
    
    @Override
    public String getMessage() {
        return String.format("Error processing document '%s' at stage %s: %s", 
            documentName, stage, super.getMessage());
    }
}
