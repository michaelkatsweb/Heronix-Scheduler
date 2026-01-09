package com.heronix.service;

import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * File Parser Service - Universal File Parsing
 * Location: src/main/java/com/eduscheduler/service/FileParserService.java
 * 
 * Parses multiple file formats into a common data structure:
 * - Excel (.xlsx, .xls)
 * - CSV (.csv)
 * - PDF (.pdf) - tables and text
 * - Images (.jpg, .png) - with OCR
 * - Text (.txt)
 * 
 * Returns data as List<Map<String, Object>> where each map represents a row.
 * 
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-10-09
 */
@Slf4j
@Service
public class FileParserService {

    /**
     * Parse file based on extension
     * 
     * @param file       The file to parse
     * @param hasHeaders Whether the file has a header row
     * @return List of maps, each representing a row
     * @throws Exception if parsing fails
     */
    public List<Map<String, Object>> parseFile(File file, boolean hasHeaders) throws Exception {
        String extension = getFileExtension(file);

        log.info("Parsing file: {} (type: {})", file.getName(), extension);

        return switch (extension) {
            case "xlsx" -> parseExcelFile(file, hasHeaders, true);
            case "xls" -> parseExcelFile(file, hasHeaders, false);
            case "csv" -> parseCsvFile(file, hasHeaders);
            case "pdf" -> parsePdfFile(file, hasHeaders);
            case "jpg", "png", "tiff" -> parseImageFile(file, hasHeaders);
            case "txt" -> parseTextFile(file, hasHeaders);
            default -> throw new IllegalArgumentException("Unsupported file type: " + extension);
        };
    }

    /**
     * Parse Excel file (.xlsx or .xls)
     */
    private List<Map<String, Object>> parseExcelFile(File file, boolean hasHeaders, boolean isXlsx)
            throws Exception {

        log.debug("Parsing Excel file: {}", file.getName());

        List<Map<String, Object>> result = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(file);
                Workbook workbook = isXlsx ? new XSSFWorkbook(fis) : new HSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0); // First sheet

            List<String> headers = new ArrayList<>();
            int startRow = 0;

            // Read headers
            if (hasHeaders) {
                Row headerRow = sheet.getRow(0);
                if (headerRow != null) {
                    for (Cell cell : headerRow) {
                        headers.add(getCellValueAsString(cell));
                    }
                }
                startRow = 1;
            } else {
                // Generate column names: Column A, Column B, etc.
                Row firstRow = sheet.getRow(0);
                if (firstRow != null) {
                    int colCount = firstRow.getLastCellNum();
                    for (int i = 0; i < colCount; i++) {
                        headers.add("Column " + (char) ('A' + i));
                    }
                }
            }

            // Read data rows
            for (int i = startRow; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null)
                    continue;

                Map<String, Object> rowData = new LinkedHashMap<>();
                boolean hasData = false;

                for (int j = 0; j < headers.size(); j++) {
                    Cell cell = row.getCell(j);
                    Object value = getCellValue(cell);

                    if (value != null && !value.toString().isEmpty()) {
                        hasData = true;
                    }

                    rowData.put(headers.get(j), value);
                }

                // Only add non-empty rows
                if (hasData) {
                    result.add(rowData);
                }
            }

            log.info("✓ Parsed {} rows from Excel file", result.size());

        } catch (Exception e) {
            log.error("Error parsing Excel file", e);
            throw new Exception("Failed to parse Excel file: " + e.getMessage(), e);
        }

        return result;
    }

    /**
     * Parse CSV file
     */
    private List<Map<String, Object>> parseCsvFile(File file, boolean hasHeaders) throws Exception {
        log.debug("Parsing CSV file: {}", file.getName());

        List<Map<String, Object>> result = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            List<String> headers = new ArrayList<>();
            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                // Skip empty lines
                if (line.trim().isEmpty())
                    continue;

                // Parse CSV line (handle quoted fields)
                List<String> values = parseCsvLine(line);

                if (isFirstLine) {
                    if (hasHeaders) {
                        headers.addAll(values);
                        isFirstLine = false;
                        continue;
                    } else {
                        // Generate column names
                        for (int i = 0; i < values.size(); i++) {
                            headers.add("Column " + (i + 1));
                        }
                    }
                    isFirstLine = false;
                }

                // Create row map
                Map<String, Object> rowData = new LinkedHashMap<>();
                for (int i = 0; i < Math.min(headers.size(), values.size()); i++) {
                    String value = values.get(i).trim();
                    rowData.put(headers.get(i), value.isEmpty() ? null : value);
                }

                result.add(rowData);
            }

            log.info("✓ Parsed {} rows from CSV file", result.size());

        } catch (Exception e) {
            log.error("Error parsing CSV file", e);
            throw new Exception("Failed to parse CSV file: " + e.getMessage(), e);
        }

        return result;
    }

    /**
     * Parse CSV line handling quoted fields
     */
    private List<String> parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }

        result.add(current.toString());
        return result;
    }

    /**
     * Parse PDF file (text and basic table extraction using Apache PDFBox)
     */
    private List<Map<String, Object>> parsePdfFile(File file, boolean hasHeaders) throws Exception {
        log.debug("Parsing PDF file: {}", file.getName());

        List<Map<String, Object>> result = new ArrayList<>();

        try (PDDocument document = org.apache.pdfbox.Loader.loadPDF(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            log.debug("Extracted {} characters from PDF", text.length());

            // Try to detect tabular data by looking for common patterns
            String[] lines = text.split("\n");
            List<String> headers = new ArrayList<>();
            boolean foundTable = false;

            for (int i = 0; i < lines.length; i++) {
                String line = lines[i].trim();

                // Skip empty lines
                if (line.isEmpty()) {
                    continue;
                }

                // Try to detect table structure
                // Look for lines with multiple whitespace-separated values or tab-separated
                String[] parts = line.split("\\s{2,}|\\t");

                // If line has multiple parts, might be tabular
                if (parts.length >= 2) {
                    if (!foundTable && hasHeaders && headers.isEmpty()) {
                        // First multi-column line is headers
                        for (String part : parts) {
                            String header = part.trim();
                            if (!header.isEmpty()) {
                                headers.add(header);
                            }
                        }
                        foundTable = true;
                        continue;
                    } else if (!foundTable && !hasHeaders) {
                        // Generate column names
                        for (int j = 0; j < parts.length; j++) {
                            headers.add("Column " + (j + 1));
                        }
                        foundTable = true;
                    }

                    // Parse data row
                    if (foundTable && parts.length > 0) {
                        Map<String, Object> rowData = new LinkedHashMap<>();
                        boolean hasData = false;

                        for (int j = 0; j < parts.length; j++) {
                            String value = parts[j].trim();
                            String columnName = j < headers.size() ? headers.get(j) : "Column " + (j + 1);

                            if (!value.isEmpty()) {
                                hasData = true;
                            }

                            rowData.put(columnName, value.isEmpty() ? null : value);
                        }

                        if (hasData) {
                            result.add(rowData);
                        }
                    }
                } else if (!foundTable) {
                    // Not tabular data - treat as single column text
                    if (headers.isEmpty()) {
                        headers.add(hasHeaders ? line : "Text");
                        if (hasHeaders) {
                            continue;
                        }
                    }

                    if (!line.isEmpty()) {
                        Map<String, Object> rowData = new LinkedHashMap<>();
                        rowData.put(headers.get(0), line);
                        result.add(rowData);
                    }
                }
            }

            if (result.isEmpty()) {
                // Fallback: return all text as single column
                Map<String, Object> textData = new LinkedHashMap<>();
                textData.put("Text", text);
                result.add(textData);
            }

            log.info("✓ Parsed {} rows from PDF file (table detection: {})", result.size(), foundTable);

        } catch (Exception e) {
            log.error("Error parsing PDF file", e);
            throw new Exception("Failed to parse PDF file: " + e.getMessage(), e);
        }

        return result;
    }

    /**
     * Parse image file with OCR using Tesseract
     */
    private List<Map<String, Object>> parseImageFile(File file, boolean hasHeaders) throws Exception {
        log.debug("Parsing image file with OCR: {}", file.getName());

        List<Map<String, Object>> result = new ArrayList<>();

        try {
            // Load image
            BufferedImage image = ImageIO.read(file);
            if (image == null) {
                throw new Exception("Unable to read image file");
            }

            log.debug("Image loaded: {}x{} pixels", image.getWidth(), image.getHeight());

            // Initialize Tesseract
            Tesseract tesseract = new Tesseract();

            // Try to set tessdata path (common locations)
            // Users may need to install Tesseract and set TESSDATA_PREFIX environment variable
            String tessDataPath = System.getenv("TESSDATA_PREFIX");
            if (tessDataPath != null && !tessDataPath.isEmpty()) {
                tesseract.setDatapath(tessDataPath);
                log.debug("Using Tesseract data path: {}", tessDataPath);
            } else {
                // Try default locations
                String[] defaultPaths = {
                    "C:/Program Files/Tesseract-OCR/tessdata",
                    "C:/Program Files (x86)/Tesseract-OCR/tessdata",
                    "/usr/share/tesseract-ocr/4.00/tessdata",
                    "/usr/share/tesseract-ocr/tessdata",
                    "/opt/homebrew/share/tessdata"
                };

                for (String path : defaultPaths) {
                    File tessDataDir = new File(path);
                    if (tessDataDir.exists() && tessDataDir.isDirectory()) {
                        tesseract.setDatapath(tessDataDir.getParent());
                        log.debug("Found Tesseract data path: {}", tessDataDir.getParent());
                        break;
                    }
                }
            }

            // Set language to English
            tesseract.setLanguage("eng");

            // Perform OCR
            log.debug("Performing OCR on image...");
            String ocrText = tesseract.doOCR(image);

            if (ocrText == null || ocrText.trim().isEmpty()) {
                log.warn("OCR returned no text from image");
                Map<String, Object> emptyResult = new LinkedHashMap<>();
                emptyResult.put("Note", "No text detected in image");
                result.add(emptyResult);
                return result;
            }

            log.debug("OCR extracted {} characters", ocrText.length());

            // Process OCR text similar to PDF parsing
            String[] lines = ocrText.split("\n");
            List<String> headers = new ArrayList<>();
            boolean foundTable = false;

            for (String line : lines) {
                line = line.trim();

                // Skip empty lines
                if (line.isEmpty()) {
                    continue;
                }

                // Try to detect table structure by looking for multiple columns
                // OCR text often has irregular spacing, so we use a more lenient pattern
                String[] parts = line.split("\\s{2,}|\\t");

                // If line has multiple parts, might be tabular
                if (parts.length >= 2) {
                    if (!foundTable && hasHeaders && headers.isEmpty()) {
                        // First multi-column line is headers
                        for (String part : parts) {
                            String header = part.trim();
                            if (!header.isEmpty()) {
                                headers.add(header);
                            }
                        }
                        foundTable = true;
                        continue;
                    } else if (!foundTable && !hasHeaders) {
                        // Generate column names
                        for (int j = 0; j < parts.length; j++) {
                            headers.add("Column " + (j + 1));
                        }
                        foundTable = true;
                    }

                    // Parse data row
                    if (foundTable && parts.length > 0) {
                        Map<String, Object> rowData = new LinkedHashMap<>();
                        boolean hasData = false;

                        for (int j = 0; j < parts.length; j++) {
                            String value = parts[j].trim();
                            String columnName = j < headers.size() ? headers.get(j) : "Column " + (j + 1);

                            if (!value.isEmpty()) {
                                hasData = true;
                            }

                            rowData.put(columnName, value.isEmpty() ? null : value);
                        }

                        if (hasData) {
                            result.add(rowData);
                        }
                    }
                } else if (!foundTable) {
                    // Not tabular data - treat as single column text
                    if (headers.isEmpty()) {
                        headers.add(hasHeaders ? line : "Text");
                        if (hasHeaders) {
                            continue;
                        }
                    }

                    if (!line.isEmpty()) {
                        Map<String, Object> rowData = new LinkedHashMap<>();
                        rowData.put(headers.get(0), line);
                        result.add(rowData);
                    }
                }
            }

            if (result.isEmpty() && !ocrText.trim().isEmpty()) {
                // Fallback: return all text as single column
                Map<String, Object> textData = new LinkedHashMap<>();
                textData.put("Text", ocrText.trim());
                result.add(textData);
            }

            log.info("✓ Parsed {} rows from image file via OCR (table detection: {})", result.size(), foundTable);

        } catch (TesseractException e) {
            log.error("Tesseract OCR error", e);
            throw new Exception("OCR failed: " + e.getMessage() +
                "\n\nNote: Tesseract OCR must be installed on your system. " +
                "Please install Tesseract from: https://github.com/tesseract-ocr/tesseract", e);
        } catch (Exception e) {
            log.error("Error parsing image file", e);
            throw new Exception("Failed to parse image file: " + e.getMessage(), e);
        }

        return result;
    }

    /**
     * Parse text file (tab or space delimited)
     */
    private List<Map<String, Object>> parseTextFile(File file, boolean hasHeaders) throws Exception {
        log.debug("Parsing text file: {}", file.getName());

        List<Map<String, Object>> result = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            List<String> headers = new ArrayList<>();
            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                // Skip empty lines
                if (line.trim().isEmpty())
                    continue;

                // Split by tabs or multiple spaces
                String[] values = line.split("\t|\\s{2,}");

                if (isFirstLine) {
                    if (hasHeaders) {
                        headers.addAll(Arrays.asList(values));
                        isFirstLine = false;
                        continue;
                    } else {
                        // Generate column names
                        for (int i = 0; i < values.length; i++) {
                            headers.add("Column " + (i + 1));
                        }
                    }
                    isFirstLine = false;
                }

                // Create row map
                Map<String, Object> rowData = new LinkedHashMap<>();
                for (int i = 0; i < Math.min(headers.size(), values.length); i++) {
                    String value = values[i].trim();
                    rowData.put(headers.get(i), value.isEmpty() ? null : value);
                }

                result.add(rowData);
            }

            log.info("✓ Parsed {} rows from text file", result.size());

        } catch (Exception e) {
            log.error("Error parsing text file", e);
            throw new Exception("Failed to parse text file: " + e.getMessage(), e);
        }

        return result;
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    /**
     * Get cell value as string
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null)
            return "";

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getDateCellValue().toString();
                } else {
                    double value = cell.getNumericCellValue();
                    // Check if it's a whole number
                    if (value == (long) value) {
                        yield String.valueOf((long) value);
                    } else {
                        yield String.valueOf(value);
                    }
                }
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield String.valueOf(cell.getNumericCellValue());
                } catch (Exception e) {
                    yield cell.getStringCellValue();
                }
            }
            case BLANK -> "";
            default -> "";
        };
    }

    /**
     * Get cell value as appropriate type
     */
    private Object getCellValue(Cell cell) {
        if (cell == null)
            return null;

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getDateCellValue();
                } else {
                    double value = cell.getNumericCellValue();
                    // Return as integer if whole number
                    if (value == (long) value) {
                        yield (long) value;
                    } else {
                        yield value;
                    }
                }
            }
            case BOOLEAN -> cell.getBooleanCellValue();
            case FORMULA -> {
                try {
                    yield cell.getNumericCellValue();
                } catch (Exception e) {
                    yield cell.getStringCellValue();
                }
            }
            case BLANK -> null;
            default -> null;
        };
    }

    /**
     * Get file extension
     */
    private String getFileExtension(File file) {
        String name = file.getName();
        int lastDot = name.lastIndexOf('.');
        return lastDot > 0 ? name.substring(lastDot + 1).toLowerCase() : "";
    }
}
