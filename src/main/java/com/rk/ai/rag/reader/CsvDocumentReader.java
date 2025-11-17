package com.rk.ai.rag.reader;

import com.opencsv.CSVReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Reader for extracting text from CSV documents using OpenCSV.
 */
@Component
public class CsvDocumentReader implements DocumentReader {
    
    private static final Logger logger = LoggerFactory.getLogger(CsvDocumentReader.class);
    
    @Override
    public String extractText(InputStream inputStream, String filename) throws Exception {
        long startTime = System.currentTimeMillis();
        logger.debug("Starting CSV text extraction for: {}", filename);
        
        try (CSVReader csvReader = new CSVReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            List<String[]> allRows = csvReader.readAll();
            
            if (allRows.isEmpty()) {
                throw new IllegalArgumentException("CSV file is empty");
            }
            
            logger.trace("CSV file has {} total rows", allRows.size());
            
            StringBuilder content = new StringBuilder();
            int dataRows = 0;
            int columns = 0;
            
            // Process header (first row)
            if (!allRows.isEmpty()) {
                String[] header = allRows.get(0);
                columns = header.length;
                logger.trace("CSV has {} columns", columns);
                
                content.append("CSV Data:\n");
                content.append("=".repeat(50)).append("\n\n");
                
                // Add header
                content.append("Headers: ");
                content.append(String.join(" | ", header));
                content.append("\n");
                content.append("-".repeat(50)).append("\n");
                
                // Add data rows
                for (int i = 1; i < allRows.size(); i++) {
                    String[] row = allRows.get(i);
                    
                    // Skip empty rows
                    if (isRowEmpty(row)) {
                        continue;
                    }
                    
                    dataRows++;
                    
                    // Format as "header: value" pairs for better context
                    for (int j = 0; j < row.length && j < header.length; j++) {
                        if (row[j] != null && !row[j].trim().isEmpty()) {
                            content.append(header[j]).append(": ").append(row[j]);
                            if (j < row.length - 1 && j < header.length - 1) {
                                content.append(", ");
                            }
                        }
                    }
                    content.append("\n");
                }
            }
            
            String text = content.toString().trim();
            if (text.isEmpty() || text.equals("CSV Data:\n" + "=".repeat(50))) {
                throw new IllegalArgumentException("CSV file contains no data");
            }
            
            long extractionTime = System.currentTimeMillis() - startTime;
            logger.info("CSV extraction completed for '{}': extracted {} chars from {} data rows, {} columns in {}ms", 
                filename, text.length(), dataRows, columns, extractionTime);
            
            return text;
        } catch (Exception e) {
            long extractionTime = System.currentTimeMillis() - startTime;
            logger.error("CSV extraction failed for '{}' after {}ms: {}", 
                filename, extractionTime, e.getMessage());
            throw e;
        }
    }
    
    private boolean isRowEmpty(String[] row) {
        if (row == null || row.length == 0) {
            return true;
        }
        
        for (String cell : row) {
            if (cell != null && !cell.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
