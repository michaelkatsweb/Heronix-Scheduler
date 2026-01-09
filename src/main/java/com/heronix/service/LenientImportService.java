package com.heronix.service;

import com.heronix.exception.ImportException;
import com.heronix.model.dto.ImportResult;
import org.springframework.web.multipart.MultipartFile;

/**
 * Lenient Import Service Interface
 * 
 * FILE LOCATION: src/main/java/com/eduscheduler/service/LenientImportService.java
 * 
 * =============================================================================
 * PURPOSE:
 * =============================================================================
 * Defines the contract for "lenient" (forgiving) data import operations.
 * Unlike strict import services that reject incomplete records, lenient import
 * auto-generates missing data and flags records for manual review.
 * 
 * =============================================================================
 * FEATURES:
 * =============================================================================
 * ✅ Auto-generates missing IDs (room numbers, employee IDs, student IDs, course codes)
 * ✅ Sets intelligent defaults for missing required fields
 * ✅ Flags incomplete records for review (needsReview = true)
 * ✅ Tracks which fields are missing per record
 * ✅ Prevents database constraint violations
 * ✅ Allows partial imports to succeed
 * 
 * =============================================================================
 * USE CASES:
 * =============================================================================
 * - Importing data from incomplete spreadsheets
 * - Bulk data migration with missing fields
 * - Quick data entry where some information is unknown
 * - Testing and development with sample data
 * - Legacy system data imports with inconsistent formats
 * 
 * =============================================================================
 * COMPARISON TO STRICT IMPORT:
 * =============================================================================
 * 
 * STRICT IMPORT (FileImportService):
 * - Rejects any record with missing required fields
 * - Returns errors for incomplete data
 * - Use when data quality is critical
 * 
 * LENIENT IMPORT (LenientImportService):
 * - Auto-generates missing required fields
 * - Flags records for review
 * - Use when getting data into system is priority
 * 
 * =============================================================================
 * IMPLEMENTATION:
 * =============================================================================
 * @see com.heronix.service.impl.LenientImportServiceImpl
 * 
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-10-18
 */
public interface LenientImportService {
    
    /**
     * Import rooms from file with lenient parsing
     * 
     * AUTO-GENERATES:
     * - Room numbers (ROOM-AUTO-001, ROOM-AUTO-002, etc.)
     * - Building name (defaults to "Main Building")
     * - Floor (defaults to 1)
     * - Capacity (defaults to 30)
     * 
     * SUPPORTED FORMATS:
     * - Excel (.xlsx, .xls)
     * - CSV (.csv)
     * 
     * DUPLICATE HANDLING:
     * - Checks for existing room numbers
     * - Skips duplicates with error message
     * 
     * @param file MultipartFile containing room data
     * @return ImportResult with success/error/warning counts and incomplete records list
     * @throws ImportException if file cannot be read or is corrupted
     * 
     * @example
     * ImportResult result = lenientImportService.importRoomsLenient(file);
     * System.out.println("Success: " + result.getSuccessCount());
     * System.out.println("Needs Review: " + result.getIncompleteRecords().size());
     */
    ImportResult importRoomsLenient(MultipartFile file) throws ImportException;
    
    /**
     * Import teachers from file with lenient parsing
     * 
     * AUTO-GENERATES:
     * - Employee IDs (EMP-00001, EMP-00002, etc.)
     * - Names (Teacher-AUTO-001 if completely missing)
     * - Department (defaults to "General")
     * 
     * SUPPORTED FORMATS:
     * - Excel (.xlsx, .xls)
     * - CSV (.csv)
     * 
     * DUPLICATE HANDLING:
     * - Checks for existing employee IDs
     * - Skips duplicates with error message
     * 
     * @param file MultipartFile containing teacher data
     * @return ImportResult with success/error/warning counts and incomplete records list
     * @throws ImportException if file cannot be read or is corrupted
     * 
     * @example
     * ImportResult result = lenientImportService.importTeachersLenient(file);
     * for (DataIssue issue : result.getIncompleteRecords()) {
     *     System.out.println("Row " + issue.getRowNumber() + " missing: " + issue.getMissingFields());
     * }
     */
    ImportResult importTeachersLenient(MultipartFile file) throws ImportException;
    
    /**
     * Import students from file with lenient parsing
     * 
     * AUTO-GENERATES:
     * - Student IDs (STU-00001, STU-00002, etc.)
     * - First names (defaults to "FirstName")
     * - Last names (defaults to "LastName")
     * - Grade level (defaults to "9")
     * 
     * SUPPORTED FORMATS:
     * - Excel (.xlsx, .xls)
     * - CSV (.csv)
     * 
     * DUPLICATE HANDLING:
     * - Checks for existing student IDs
     * - Skips duplicates with error message
     * 
     * @param file MultipartFile containing student data
     * @return ImportResult with success/error/warning counts and incomplete records list
     * @throws ImportException if file cannot be read or is corrupted
     * 
     * @example
     * ImportResult result = lenientImportService.importStudentsLenient(file);
     * if (result.getErrorCount() > 0) {
     *     for (String error : result.getErrors()) {
     *         System.err.println("Error: " + error);
     *     }
     * }
     */
    ImportResult importStudentsLenient(MultipartFile file) throws ImportException;
    
    /**
     * Import courses from file with lenient parsing
     * 
     * AUTO-GENERATES:
     * - Course codes (CRS-0001, CRS-0002, etc.)
     * - Course names (uses "Course-{code}" if missing)
     * - Max students (defaults to 30)
     * 
     * SUPPORTED FORMATS:
     * - Excel (.xlsx, .xls)
     * - CSV (.csv)
     * 
     * DUPLICATE HANDLING:
     * - Checks for existing course codes
     * - Skips duplicates with error message
     * 
     * @param file MultipartFile containing course data
     * @return ImportResult with success/error/warning counts and incomplete records list
     * @throws ImportException if file cannot be read or is corrupted
     * 
     * @example
     * ImportResult result = lenientImportService.importCoursesLenient(file);
     * System.out.println("Total imported: " + result.getSuccessCount());
     * System.out.println("Need review: " + result.getWarningCount());
     * System.out.println("Failed: " + result.getErrorCount());
     */
    ImportResult importCoursesLenient(MultipartFile file) throws ImportException;
}