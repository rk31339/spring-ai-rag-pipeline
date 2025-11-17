package com.rk.ai.rag.model;

import java.util.List;
import java.util.Map;

/**
 * Response model for vector database search results.
 */
public class SearchResponse {
    
    private String query;
    private int resultsCount;
    private List<DocumentChunk> documents;
    
    public SearchResponse() {
    }
    
    public SearchResponse(String query, List<DocumentChunk> documents) {
        this.query = query;
        this.documents = documents;
        this.resultsCount = documents != null ? documents.size() : 0;
    }
    
    public String getQuery() {
        return query;
    }
    
    public void setQuery(String query) {
        this.query = query;
    }
    
    public int getResultsCount() {
        return resultsCount;
    }
    
    public void setResultsCount(int resultsCount) {
        this.resultsCount = resultsCount;
    }
    
    public List<DocumentChunk> getDocuments() {
        return documents;
    }
    
    public void setDocuments(List<DocumentChunk> documents) {
        this.documents = documents;
        this.resultsCount = documents != null ? documents.size() : 0;
    }
    
    /**
     * Represents a document chunk from the vector database.
     */
    public static class DocumentChunk {
        private String content;
        private double similarityScore;
        private Map<String, Object> metadata;
        
        public DocumentChunk() {
        }
        
        public DocumentChunk(String content, double similarityScore, Map<String, Object> metadata) {
            this.content = content;
            this.similarityScore = similarityScore;
            this.metadata = metadata;
        }
        
        public String getContent() {
            return content;
        }
        
        public void setContent(String content) {
            this.content = content;
        }
        
        public double getSimilarityScore() {
            return similarityScore;
        }
        
        public void setSimilarityScore(double similarityScore) {
            this.similarityScore = similarityScore;
        }
        
        public Map<String, Object> getMetadata() {
            return metadata;
        }
        
        public void setMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
        }
    }
}
