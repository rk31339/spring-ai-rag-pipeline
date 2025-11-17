package com.rk.ai.rag.model;

/**
 * Request model for RAG queries.
 */
public class QueryRequest {
    
    private String query;
    private Integer topK;
    
    public QueryRequest() {
    }
    
    public QueryRequest(String query) {
        this.query = query;
    }
    
    public QueryRequest(String query, Integer topK) {
        this.query = query;
        this.topK = topK;
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
}
