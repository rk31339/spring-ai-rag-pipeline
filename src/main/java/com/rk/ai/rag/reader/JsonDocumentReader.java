package com.rk.ai.rag.reader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Reader for extracting text from JSON documents using Jackson.
 */
@Component
public class JsonDocumentReader implements DocumentReader {
    
    private static final Logger logger = LoggerFactory.getLogger(JsonDocumentReader.class);
    private final ObjectMapper objectMapper;
    
    public JsonDocumentReader() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }
    
    @Override
    public String extractText(InputStream inputStream, String filename) throws Exception {
        long startTime = System.currentTimeMillis();
        logger.debug("Starting JSON text extraction for: {}", filename);
        
        try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            // Parse JSON into a generic object structure
            Object jsonObject = objectMapper.readValue(reader, Object.class);
            logger.trace("Parsed JSON object type: {}", jsonObject.getClass().getSimpleName());
            
            // Convert to human-readable format
            String formattedJson = convertToReadableText(jsonObject, 0);
            
            if (formattedJson == null || formattedJson.trim().isEmpty()) {
                throw new IllegalArgumentException("JSON file contains no data");
            }
            
            long extractionTime = System.currentTimeMillis() - startTime;
            int objectCount = countObjects(jsonObject);
            logger.info("JSON extraction completed for '{}': extracted {} chars from {} objects in {}ms", 
                filename, formattedJson.length(), objectCount, extractionTime);
            
            return formattedJson;
        } catch (Exception e) {
            long extractionTime = System.currentTimeMillis() - startTime;
            logger.error("JSON extraction failed for '{}' after {}ms: {}", 
                filename, extractionTime, e.getMessage());
            throw e;
        }
    }
    
    private int countObjects(Object obj) {
        if (obj instanceof java.util.Map) {
            return ((java.util.Map<?, ?>) obj).size();
        } else if (obj instanceof java.util.List) {
            return ((java.util.List<?>) obj).size();
        }
        return 1;
    }
    
    private String convertToReadableText(Object obj, int indentLevel) {
        if (obj == null) {
            return "null";
        }
        
        String indent = "  ".repeat(indentLevel);
        StringBuilder content = new StringBuilder();
        
        if (obj instanceof java.util.Map) {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> map = (java.util.Map<String, Object>) obj;
            
            for (java.util.Map.Entry<String, Object> entry : map.entrySet()) {
                content.append(indent).append(entry.getKey()).append(": ");
                
                Object value = entry.getValue();
                if (value instanceof java.util.Map || value instanceof java.util.List) {
                    content.append("\n");
                    content.append(convertToReadableText(value, indentLevel + 1));
                } else {
                    content.append(convertToReadableText(value, 0)).append("\n");
                }
            }
        } else if (obj instanceof java.util.List) {
            @SuppressWarnings("unchecked")
            java.util.List<Object> list = (java.util.List<Object>) obj;
            
            for (int i = 0; i < list.size(); i++) {
                content.append(indent).append("- ");
                Object item = list.get(i);
                
                if (item instanceof java.util.Map || item instanceof java.util.List) {
                    content.append("\n");
                    content.append(convertToReadableText(item, indentLevel + 1));
                } else {
                    content.append(convertToReadableText(item, 0)).append("\n");
                }
            }
        } else {
            return obj.toString();
        }
        
        return content.toString();
    }
}
