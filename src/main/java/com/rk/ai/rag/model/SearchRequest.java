package com.rk.ai.rag.model;

/**
 * Request model for vector database search without LLM processing.
 */
public class SearchRequest {
    
    private String query;
    private Integer topK;
    private Double similarityThreshold;
    
    public SearchRequest() {
    }
    
    public SearchRequest(String query, Integer topK, Double similarityThreshold) {
        this.query = query;
        this.topK = topK;
        this.similarityThreshold = similarityThreshold;
    }
    
    public String getQuery() {
        return query;
    }
    
    public void setQuery(String query) {
        this.query = query;
    }
    
    public Integer getTopK() {
        return topK;
    }
    
    public void setTopK(Integer topK) {
        this.topK = topK;
    }
    
    public Double getSimilarityThreshold() {
        return similarityThreshold;
    }
    
    public void setSimilarityThreshold(Double similarityThreshold) {
        this.similarityThreshold = similarityThreshold;
    }
}
