package com.rk.ai.rag.service;

import com.rk.ai.rag.model.SearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for RAG (Retrieval Augmented Generation) queries.
 * Retrieves relevant document chunks from vector store and generates answers using LLM.
 */
@Service
public class RagQueryService {
    
    private static final Logger logger = LoggerFactory.getLogger(RagQueryService.class);
    private static final int DEFAULT_TOP_K = 5;
    private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.3;
    
    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    
    public RagQueryService(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        this.vectorStore = vectorStore;
        this.chatClient = chatClientBuilder.build();
    }
    
    /**
     * Queries the RAG system with a question.
     * Retrieves relevant document chunks and generates an answer using the LLM.
     * 
     * @param query The user's question
     * @return The generated answer based on relevant documents
     */
    public String query(String query) {
        return query(query, DEFAULT_TOP_K);
    }
    
    /**
     * Queries the RAG system with custom top-K parameter.
     * 
     * @param query The user's question
     * @param topK Number of most relevant documents to retrieve
     * @return The generated answer based on relevant documents
     */
    public String query(String query, int topK) {
        logger.info("Processing RAG query with topK={}: {}", topK, query);
        
        try {
            // Step 1: Retrieve relevant documents from vector store
            SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(DEFAULT_SIMILARITY_THRESHOLD)
                .build();
            
            List<Document> similarDocuments = vectorStore.similaritySearch(searchRequest);
            
            if (similarDocuments.isEmpty()) {
                logger.warn("No relevant documents found for query");
                return "I couldn't find any relevant information in the knowledge base to answer your question.";
            }
            
            logger.info("Retrieved {} relevant documents", similarDocuments.size());
            
            // Step 2: Build context from retrieved documents
            String context = similarDocuments.stream()
                .map(doc -> {
                    String content = doc.getText();
                    String filename = (String) doc.getMetadata().get("filename");
                    return "Source: " + filename + "\n" + content;
                })
                .collect(Collectors.joining("\n\n---\n\n"));
            
            // Step 3: Create prompt with context and query
            String systemPrompt = """
                You are a helpful AI assistant. Answer the user's question based on the provided context.
                If the context doesn't contain enough information to answer the question, say so clearly.
                Be concise and accurate in your response.
                
                Context from knowledge base:
                %s
                """.formatted(context);
            
            // Step 4: Generate answer using LLM
            String answer = chatClient.prompt()
                .system(systemPrompt)
                .user(query)
                .call()
                .content();
            
            logger.info("Successfully generated answer for query");
            return answer;
            
        } catch (Exception e) {
            logger.error("Error processing RAG query", e);
            throw new RuntimeException("Failed to process query: " + e.getMessage(), e);
        }
    }
    
    /**
     * Searches the vector database for similar documents without LLM processing.
     * This is useful for external services that want raw document chunks.
     * 
     * @param query The search query
     * @param topK Number of most relevant documents to retrieve (default: 5)
     * @param similarityThreshold Minimum similarity score (default: 0.3)
     * @return SearchResponse containing matched document chunks
     */
    public SearchResponse search(String query, Integer topK, Double similarityThreshold) {
        logger.info("Processing vector search with topK={}, threshold={}: {}", 
            topK, similarityThreshold, query);
        
        try {
            // Use defaults if not provided
            int k = (topK != null) ? topK : DEFAULT_TOP_K;
            double threshold = (similarityThreshold != null) ? similarityThreshold : DEFAULT_SIMILARITY_THRESHOLD;
            
            // Build search request
            SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .topK(k)
                .similarityThreshold(threshold)
                .build();
            
            // Perform similarity search
            List<Document> similarDocuments = vectorStore.similaritySearch(searchRequest);
            
            logger.info("Vector search returned {} documents", similarDocuments.size());
            
            // Convert to response model
            List<SearchResponse.DocumentChunk> chunks = similarDocuments.stream()
                .map(doc -> new SearchResponse.DocumentChunk(
                    doc.getText(),
                    calculateSimilarityScore(doc),
                    doc.getMetadata()
                ))
                .collect(Collectors.toList());
            
            return new SearchResponse(query, chunks);
            
        } catch (Exception e) {
            logger.error("Error performing vector search", e);
            throw new RuntimeException("Failed to perform vector search: " + e.getMessage(), e);
        }
    }
    
    /**
     * Extracts similarity score from document metadata if available.
     * Falls back to 0.0 if not present.
     */
    private double calculateSimilarityScore(Document doc) {
        Object score = doc.getMetadata().get("distance");
        if (score instanceof Number) {
            // Convert distance to similarity (1 - distance)
            double distance = ((Number) score).doubleValue();
            return 1.0 - distance;
        }
        return 0.0;
    }
}
