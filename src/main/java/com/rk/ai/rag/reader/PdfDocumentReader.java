package com.rk.ai.rag.reader;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * Reader for extracting text from PDF documents using Apache PDFBox.
 */
@Component
public class PdfDocumentReader implements DocumentReader {
    
    private static final Logger logger = LoggerFactory.getLogger(PdfDocumentReader.class);
    
    @Override
    public String extractText(InputStream inputStream, String filename) throws Exception {
        long startTime = System.currentTimeMillis();
        logger.debug("Starting PDF text extraction for: {}", filename);
        
        try {
            byte[] pdfBytes = inputStream.readAllBytes();
            logger.trace("Read {} bytes from PDF file", pdfBytes.length);
            
            try (PDDocument document = Loader.loadPDF(pdfBytes)) {
                int pageCount = document.getNumberOfPages();
                logger.trace("PDF has {} pages", pageCount);
                
                PDFTextStripper stripper = new PDFTextStripper();
                String text = stripper.getText(document);
                
                if (text == null || text.trim().isEmpty()) {
                    throw new IllegalArgumentException("PDF contains no extractable text");
                }
                
                long extractionTime = System.currentTimeMillis() - startTime;
                logger.info("PDF extraction completed for '{}': extracted {} chars from {} pages in {}ms", 
                    filename, text.trim().length(), pageCount, extractionTime);
                
                return text.trim();
            }
        } catch (Exception e) {
            long extractionTime = System.currentTimeMillis() - startTime;
            logger.error("PDF extraction failed for '{}' after {}ms: {}", 
                filename, extractionTime, e.getMessage());
            throw e;
        }
    }
}
