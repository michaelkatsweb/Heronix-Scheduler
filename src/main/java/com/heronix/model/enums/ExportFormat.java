// Location: src/main/java/com/eduscheduler/model/enums/ExportFormat.java
package com.heronix.model.enums;

/**
 * Supported export formats for schedules
 */
public enum ExportFormat {
    EXCEL("Excel Spreadsheet", "xlsx"),
    PDF("PDF Document", "pdf"),
    CSV("CSV File", "csv"),
    ICAL("iCalendar", "ics"),
    HTML("HTML Page", "html"),
    JSON("JSON Data", "json");
    
    private final String displayName;
    private final String extension;
    
    ExportFormat(String displayName, String extension) {
        this.displayName = displayName;
        this.extension = extension;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getExtension() {
        return extension;
    }
}