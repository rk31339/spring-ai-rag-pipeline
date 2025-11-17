package com.rk.ai.rag.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for intelligently chunking text documents for optimal RAG performance.
 * Implements semantic chunking with overlapping windows to preserve context.
 */
@Service
public class ChunkingService {
    
    private static final Logger logger = LoggerFactory.getLogger(ChunkingService.class);
    
    // Optimal chunk size for retrieval (in characters, roughly 300-400 tokens)
    private static final int CHUNK_SIZE = 1500;
    
    // Maximum chunk size - hard limit to prevent token overflow (roughly 500 tokens)
    private static final int MAX_CHUNK_SIZE = 2000;
    
    // Overlap between chunks to prevent context loss at boundaries
    private static final int CHUNK_OVERLAP = 200;
    
    // Minimum chunk size to avoid tiny fragments
    private static final int MIN_CHUNK_SIZE = 100;
    
    /**
     * Chunks a document into overlapping segments with metadata preservation.
     * 
     * @param content The text content to chunk
     * @param sourceMetadata Metadata from the source document (filename, upload date, etc.)
     * @param documentId The document ID to use (deterministic based on filename)
     * @return List of Spring AI Document objects ready for embedding and storage
     */
    public List<Document> chunkDocument(String content, Map<String, Object> sourceMetadata, String documentId) {
        long startTime = System.currentTimeMillis();
        String filename = (String) sourceMetadata.getOrDefault("filename", "unknown");
        
        List<Document> chunks = new ArrayList<>();
        
        if (content == null || content.trim().isEmpty()) {
            logger.warn("Empty content provided for chunking: {}", filename);
            return chunks;
        }
        
        logger.debug("Starting document chunking for '{}' - content length: {} chars", filename, content.length());
        
        // Split by paragraphs first for semantic boundaries
        String[] paragraphs = content.split("\\n\\n+");
        StringBuilder currentChunk = new StringBuilder();
        int chunkIndex = 0;
        
        for (String paragraph : paragraphs) {
            paragraph = paragraph.trim();
            
            if (paragraph.isEmpty()) {
                continue;
            }
            
            // If paragraph itself is too large, split it further
            if (paragraph.length() > MAX_CHUNK_SIZE) {
                // Save current chunk if it has content
                if (currentChunk.length() > MIN_CHUNK_SIZE) {
                    chunks.add(createDocument(currentChunk.toString(), documentId, chunkIndex++, sourceMetadata));
                    currentChunk.setLength(0);
                }
                
                // Split large paragraph into smaller chunks
                List<String> paragraphChunks = splitLargeParagraph(paragraph);
                for (String paraChunk : paragraphChunks) {
                    chunks.add(createDocument(paraChunk, documentId, chunkIndex++, sourceMetadata));
                }
                continue;
            }
            
            // If adding this paragraph exceeds chunk size, save current chunk first
            if (currentChunk.length() + paragraph.length() + 1 > CHUNK_SIZE && currentChunk.length() > MIN_CHUNK_SIZE) {
                chunks.add(createDocument(currentChunk.toString(), documentId, chunkIndex++, sourceMetadata));
                String overlap = getOverlap(currentChunk.toString());
                currentChunk.setLength(0);
                currentChunk.append(overlap);
            }
            
            // Add paragraph with spacing
            if (currentChunk.length() > 0) {
                currentChunk.append(" ");
            }
            currentChunk.append(paragraph);
        }
        
        // Add remaining content as final chunk
        if (currentChunk.length() > MIN_CHUNK_SIZE) {
            chunks.add(createDocument(currentChunk.toString(), documentId, chunkIndex, sourceMetadata));
        }
        
        // Add total chunks metadata to all chunks
        int totalChunks = chunks.size();
        chunks.forEach(doc -> doc.getMetadata().put("total_chunks", totalChunks));
        
        long processingTime = System.currentTimeMillis() - startTime;
        logger.info("Chunking completed for '{}': {} chunks created from {} chars in {}ms", 
                    filename, totalChunks, content.length(), processingTime);
        
        return chunks;
    }
    
    /**
     * Creates a Spring AI Document with enriched metadata.
     */
    private Document createDocument(String content, String documentId, int chunkIndex, Map<String, Object> sourceMetadata) {
        Map<String, Object> metadata = new HashMap<>(sourceMetadata);
        metadata.put("chunk_index", chunkIndex);
        metadata.put("source_document_id", documentId);
        metadata.put("chunk_size", content.length());
        
        return new Document(content, metadata);
    }
    
    /**
     * Splits a large paragraph into smaller chunks by sentences or fixed-size segments.
     */
    private List<String> splitLargeParagraph(String paragraph) {
        List<String> chunks = new ArrayList<>();
        
        // Try to split by sentences first
        String[] sentences = paragraph.split("(?<=[.!?])\\s+");
        
        StringBuilder currentChunk = new StringBuilder();
        
        for (String sentence : sentences) {
            sentence = sentence.trim();
            
            if (sentence.isEmpty()) {
                continue;
            }
            
            // If a single sentence is too large, split it by fixed size
            if (sentence.length() > MAX_CHUNK_SIZE) {
                // Save current chunk if it has content
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString());
                    currentChunk.setLength(0);
                }
                
                // Split by fixed size with overlap
                for (int i = 0; i < sentence.length(); i += CHUNK_SIZE - CHUNK_OVERLAP) {
                    int end = Math.min(i + CHUNK_SIZE, sentence.length());
                    chunks.add(sentence.substring(i, end));
                }
                continue;
            }
            
            // If adding this sentence exceeds chunk size, save current chunk
            if (currentChunk.length() + sentence.length() + 1 > CHUNK_SIZE && currentChunk.length() > 0) {
                chunks.add(currentChunk.toString());
                String overlap = getOverlap(currentChunk.toString());
                currentChunk.setLength(0);
                currentChunk.append(overlap).append(" ");
            }
            
            // Add sentence
            if (currentChunk.length() > 0) {
                currentChunk.append(" ");
            }
            currentChunk.append(sentence);
        }
        
        // Add remaining content
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString());
        }
        
        return chunks;
    }
    
    /**
     * Extracts overlap text from the end of a chunk for continuity.
     */
    private String getOverlap(String text) {
        if (text.length() <= CHUNK_OVERLAP) {
            return text;
        }
        return text.substring(text.length() - CHUNK_OVERLAP);
    }
}
