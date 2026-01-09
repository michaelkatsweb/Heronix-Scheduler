package com.heronix.model.domain;

import jakarta.persistence.*;
import java.util.*;

@Entity
@Table(name = "import_templates")
public class ImportTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String entityType; // "Student", "Teacher", etc.

    @ElementCollection
    @CollectionTable(name = "template_column_mappings")
    @MapKeyColumn(name = "standard_field")
    @Column(name = "actual_column")
    private Map<String, String> columnMapping = new HashMap<>();

    @ElementCollection
    @CollectionTable(name = "template_default_values")
    @MapKeyColumn(name = "field_name")
    @Column(name = "default_value")
    private Map<String, String> defaultValues = new HashMap<>();

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdDate;

    @Temporal(TemporalType.TIMESTAMP)
    private Date lastUsedDate;

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public Map<String, String> getColumnMapping() {
        return columnMapping;
    }

    public void setColumnMapping(Map<String, String> columnMapping) {
        this.columnMapping = columnMapping;
    }

    public Map<String, String> getDefaultValues() {
        return defaultValues;
    }

    public void setDefaultValues(Map<String, String> defaultValues) {
        this.defaultValues = defaultValues;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public Date getLastUsedDate() {
        return lastUsedDate;
    }

    public void setLastUsedDate(Date lastUsedDate) {
        this.lastUsedDate = lastUsedDate;
    }
}