package com.rk.ai.rag.reader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Reader for extracting text from plain text and markdown documents (.txt, .md, .markdown).
 */
@Component
public class TextDocumentReader implements DocumentReader {
    
    private static final Logger logger = LoggerFactory.getLogger(TextDocumentReader.class);
    
    @Override
    public String extractText(InputStream inputStream, String filename) throws Exception {
        long startTime = System.currentTimeMillis();
        logger.debug("Starting text/markdown extraction for: {}", filename);
        
        try {
            byte[] bytes = inputStream.readAllBytes();
            logger.trace("Read {} bytes from text file", bytes.length);
            
            String content = new String(bytes, StandardCharsets.UTF_8);
            
            if (content.trim().isEmpty()) {
                throw new IllegalArgumentException("File content is empty or contains only whitespace");
            }
            
            long extractionTime = System.currentTimeMillis() - startTime;
            int lineCount = content.split("\n").length;
            logger.info("Text extraction completed for '{}': extracted {} chars, {} lines in {}ms", 
                filename, content.trim().length(), lineCount, extractionTime);
            
            return content.trim();
        } catch (Exception e) {
            long extractionTime = System.currentTimeMillis() - startTime;
            logger.error("Text extraction failed for '{}' after {}ms: {}", 
                filename, extractionTime, e.getMessage());
            throw e;
        }
    }
}
