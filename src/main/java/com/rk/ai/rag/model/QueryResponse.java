package com.rk.ai.rag.model;

/**
 * Response model for RAG queries.
 */
public class QueryResponse {
    
    private String query;
    private String answer;
    private Integer topK;
    private long responseTimeMs;
    
    public QueryResponse() {
    }
    
    public QueryResponse(String query, String answer) {
        this.query = query;
        this.answer = answer;
    }
    
    public QueryResponse(String query, String answer, Integer topK, long responseTimeMs) {
        this.query = query;
        this.answer = answer;
        this.topK = topK;
        this.responseTimeMs = responseTimeMs;
    }
    
    public String getQuery() {
        return query;
    }
    
    public void setQuery(String query) {
        this.query = query;
    }
    
    public String getAnswer() {
        return answer;
    }
    
    public void setAnswer(String answer) {
        this.answer = answer;
    }
    
    public Integer getTopK() {
        return topK;
    }
    
    public void setTopK(Integer topK) {
        this.topK = topK;
    }
    
    public long getResponseTimeMs() {
        return responseTimeMs;
    }
    
    public void setResponseTimeMs(long responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }
}
