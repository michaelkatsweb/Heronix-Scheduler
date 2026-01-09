package com.heronix.service;

import com.heronix.exception.ImportException;
import com.heronix.model.dto.ImportResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * File Import Service Interface - CORRECTED
 * Location: src/main/java/com/eduscheduler/service/FileImportService.java
 */
public interface FileImportService {

    ImportResult importTeachers(MultipartFile file) throws ImportException;

    ImportResult importCourses(MultipartFile file) throws ImportException;

    ImportResult importRooms(MultipartFile file) throws ImportException;

    ImportResult importStudents(MultipartFile file) throws ImportException;

    ImportResult importSchedule(MultipartFile file) throws ImportException;

    ImportResult importFile(MultipartFile file, String entityType) throws ImportException;

    ImportResult processExcelFile(MultipartFile file) throws ImportException;

    boolean validateFile(MultipartFile file) throws ImportException;

    boolean isSupportedFileType(String filename);

    String getFileType(String filename);

    List<String> getSupportedFormats();

    ImportResult importEvents(MultipartFile file) throws ImportException;
}