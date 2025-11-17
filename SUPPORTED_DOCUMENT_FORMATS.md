# Supported Document Formats

This RAG application now supports multiple document formats for ingestion and querying.

## Supported File Types

### Text-Based Documents
- **Plain Text** (`.txt`)
- **Markdown** (`.md`, `.markdown`)

### Office Documents
- **PDF** (`.pdf`) - Text extraction using Apache PDFBox 3.0
- **Microsoft Word**
  - Legacy format (`.doc`) - Uses Apache POI Scratchpad
  - Modern format (`.docx`) - Uses Apache POI OOXML
- **Microsoft Excel**
  - Legacy format (`.xls`) - Uses Apache POI
  - Modern format (`.xlsx`) - Uses Apache POI OOXML

### Data Formats
- **CSV** (`.csv`) - Parsed using OpenCSV
- **JSON** (`.json`) - Parsed using Jackson (included with Spring Boot)

## File Size Limit

Maximum file size: **10 MB** per file

## How It Works

### Document Processing Pipeline

1. **Upload**: Files are uploaded through the UI or REST API
2. **Validation**: File type and size are validated
3. **Text Extraction**: Appropriate reader extracts text based on file extension
4. **Chunking**: Extracted text is split into chunks for vector storage
5. **Embedding**: Chunks are embedded and stored in PostgreSQL with pgvector
6. **Querying**: RAG queries retrieve relevant chunks and generate answers

### Reader Implementations

#### PDF Reader (`PdfDocumentReader`)
- Extracts plain text from PDF documents
- Note: Does not perform OCR on scanned/image-based PDFs

#### Word Reader (`WordDocumentReader`)
- Extracts paragraphs and table content
- Supports both legacy (.doc) and modern (.docx) formats

#### Excel Reader (`ExcelDocumentReader`)
- Processes all sheets in the workbook
- Converts cells to readable text format
- Handles dates, numbers, formulas, and booleans
- Preserves sheet structure for context

#### CSV Reader (`CsvDocumentReader`)
- Parses CSV with header row
- Formats data as "header: value" pairs
- Maintains row context for better RAG retrieval

#### JSON Reader (`JsonDocumentReader`)
- Parses JSON structure
- Converts to human-readable indented format
- Preserves nested object/array hierarchy

#### Text Reader (`TextDocumentReader`)
- Handles plain text and markdown files
- Preserves original formatting

## Metadata Tracking

Each document chunk includes metadata:
- `filename`: Original file name
- `file_size`: File size in bytes
- `upload_date`: Upload timestamp
- `document_id`: Unique document identifier
- `content_type`: MIME type
- `file_type`: Categorized type (pdf, word, excel, csv, json, markdown, text)

## Usage

### Upload via UI
1. Navigate to the "Upload Documents" view
2. Select one or more files (any supported format)
3. Click "Upload Documents"
4. View processing status and results

### Upload via REST API
```bash
curl -X POST http://localhost:8080/api/documents/upload \
  -F "files=@document.pdf" \
  -F "files=@data.xlsx" \
  -F "files=@config.json"
```

### Query Documents
After uploading, query your documents through the "Query RAG" view or API:

```bash
curl -X POST http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d '{
    "question": "What are the key features?",
    "topK": 5
  }'
```

## Dependencies

The following libraries enable document format support:

- **Apache PDFBox 3.0.3** - PDF text extraction
- **Apache POI 5.3.0** - Excel (.xls) support  
- **Apache POI OOXML 5.3.0** - Excel (.xlsx) and Word (.docx)
- **Apache POI Scratchpad 5.3.0** - Word (.doc)
- **OpenCSV 5.9** - CSV parsing
- **Jackson** - JSON parsing (included with Spring Boot)

## Error Handling

The system provides detailed error messages for:
- Unsupported file types
- Files exceeding size limit
- Empty or corrupted files
- Text extraction failures
- Vector storage issues

Each error includes:
- Error message
- Filename
- Processing stage where the error occurred

## Best Practices

1. **PDF Documents**: Ensure PDFs contain actual text (not scanned images)
2. **Excel Files**: Use clear column headers for better context
3. **CSV Files**: Include a header row
4. **JSON Files**: Use well-structured, meaningful key names
5. **File Names**: Use descriptive names for better organization

## Limitations

- PDF OCR is not supported (scanned/image PDFs won't extract text)
- Password-protected files are not supported
- Macros in Excel/Word files are ignored
- Complex Excel formulas are evaluated to their result values
- Binary file formats beyond those listed are not supported

## Future Enhancements

Potential future additions:
- PowerPoint (.ppt, .pptx) support
- Image OCR for scanned PDFs
- HTML file support
- XML file parsing
- Compressed archive support (.zip with multiple documents)
