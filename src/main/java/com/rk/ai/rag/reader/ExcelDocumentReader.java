package com.rk.ai.rag.reader;

import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Reader for extracting text from Excel documents (.xls, .xlsx) using Apache POI.
 */
@Component
public class ExcelDocumentReader implements DocumentReader {
    
    private static final Logger logger = LoggerFactory.getLogger(ExcelDocumentReader.class);
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    @Override
    public String extractText(InputStream inputStream, String filename) throws Exception {
        long startTime = System.currentTimeMillis();
        logger.debug("Starting Excel text extraction for: {}", filename);
        
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            int sheetCount = workbook.getNumberOfSheets();
            logger.trace("Excel file has {} sheets", sheetCount);
            
            StringBuilder content = new StringBuilder();
            int totalRows = 0;
            int totalCells = 0;
            
            for (int i = 0; i < sheetCount; i++) {
                Sheet sheet = workbook.getSheetAt(i);
                String sheetName = sheet.getSheetName();
                logger.trace("Processing sheet {}: '{}'", i + 1, sheetName);
                
                if (i > 0) {
                    content.append("\n\n");
                }
                content.append("Sheet: ").append(sheetName).append("\n");
                content.append("=".repeat(50)).append("\n\n");
                
                int rowCount = 0;
                for (Row row : sheet) {
                    if (isRowEmpty(row)) {
                        continue;
                    }
                    
                    rowCount++;
                    int cellCount = 0;
                    for (Cell cell : row) {
                        String cellValue = getCellValueAsString(cell);
                        if (cellValue != null && !cellValue.isEmpty()) {
                            content.append(cellValue).append("\t");
                            cellCount++;
                        }
                    }
                    totalCells += cellCount;
                    content.append("\n");
                }
                totalRows += rowCount;
                logger.trace("Extracted {} rows from sheet '{}'", rowCount, sheetName);
            }
            
            String text = content.toString().trim();
            if (text.isEmpty()) {
                throw new IllegalArgumentException("Excel file contains no data");
            }
            
            long extractionTime = System.currentTimeMillis() - startTime;
            logger.info("Excel extraction completed for '{}': extracted {} chars from {} sheets, {} rows, {} cells in {}ms", 
                filename, text.length(), sheetCount, totalRows, totalCells, extractionTime);
            
            return text;
        } catch (Exception e) {
            long extractionTime = System.currentTimeMillis() - startTime;
            logger.error("Excel extraction failed for '{}' after {}ms: {}", 
                filename, extractionTime, e.getMessage());
            throw e;
        }
    }
    
    private boolean isRowEmpty(Row row) {
        if (row == null) {
            return true;
        }
        
        for (Cell cell : row) {
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String value = getCellValueAsString(cell);
                if (value != null && !value.trim().isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }
    
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    Date date = cell.getDateCellValue();
                    return DATE_FORMAT.format(date);
                } else {
                    double numericValue = cell.getNumericCellValue();
                    if (numericValue == (long) numericValue) {
                        return String.valueOf((long) numericValue);
                    } else {
                        return String.valueOf(numericValue);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return String.valueOf(cell.getNumericCellValue());
                } catch (IllegalStateException e) {
                    return cell.getStringCellValue();
                }
            case BLANK:
                return "";
            default:
                return "";
        }
    }
}
