package com.rk.ai.rag.reader;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * Reader for extracting text from Word documents (.doc, .docx) using Apache POI.
 */
@Component
public class WordDocumentReader implements DocumentReader {
    
    private static final Logger logger = LoggerFactory.getLogger(WordDocumentReader.class);
    
    @Override
    public String extractText(InputStream inputStream, String filename) throws Exception {
        long startTime = System.currentTimeMillis();
        logger.debug("Starting Word document text extraction for: {}", filename);
        
        try {
            String text;
            if (filename.toLowerCase().endsWith(".docx")) {
                text = extractFromDocx(inputStream);
            } else if (filename.toLowerCase().endsWith(".doc")) {
                text = extractFromDoc(inputStream);
            } else {
                throw new IllegalArgumentException("Unsupported Word document format: " + filename);
            }
            
            long extractionTime = System.currentTimeMillis() - startTime;
            logger.info("Word extraction completed for '{}': extracted {} chars in {}ms", 
                filename, text.length(), extractionTime);
            
            return text;
        } catch (Exception e) {
            long extractionTime = System.currentTimeMillis() - startTime;
            logger.error("Word extraction failed for '{}' after {}ms: {}", 
                filename, extractionTime, e.getMessage());
            throw e;
        }
    }
    
    private String extractFromDocx(InputStream inputStream) throws Exception {
        logger.trace("Extracting text from .docx format");
        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            StringBuilder content = new StringBuilder();
            
            // Extract paragraphs
            int paragraphCount = 0;
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                paragraphCount++;
                String text = paragraph.getText();
                if (text != null && !text.trim().isEmpty()) {
                    content.append(text).append("\n");
                }
            }
            
            logger.trace("Extracted {} paragraphs from .docx", paragraphCount);
            
            // Extract tables
            int tableCount = 0;
            for (XWPFTable table : document.getTables()) {
                tableCount++;
                content.append("\n");
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        String cellText = cell.getText();
                        if (cellText != null && !cellText.trim().isEmpty()) {
                            content.append(cellText).append("\t");
                        }
                    }
                    content.append("\n");
                }
                content.append("\n");
            }
            
            logger.trace("Extracted {} tables from .docx", tableCount);
            
            String text = content.toString().trim();
            if (text.isEmpty()) {
                throw new IllegalArgumentException("Word document contains no text");
            }
            
            return text;
        }
    }
    
    private String extractFromDoc(InputStream inputStream) throws Exception {
        logger.trace("Extracting text from .doc format");
        try (HWPFDocument document = new HWPFDocument(inputStream);
             WordExtractor extractor = new WordExtractor(document)) {
            
            String text = extractor.getText().trim();
            
            if (text.isEmpty()) {
                throw new IllegalArgumentException("Word document contains no text");
            }
            
            logger.trace("Extracted {} chars from .doc", text.length());
            return text;
        }
    }
}
