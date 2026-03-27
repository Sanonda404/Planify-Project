// models/QueryContext.java
package com.planify.frontend.chatbot.models;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public class QueryContext {
    private String originalQuery;
    private Intent intent;
    private LocalDate referenceDate;
    private LocalDateTime referenceDateTime;
    private TimeRange timeRange;
    private String targetProject;
    private String targetGroup;
    private String targetCategory;
    private List<String> keywords;
    private Map<String, Object> extractedEntities;
    private double confidence;
    private boolean requiresHistoricalData;
    private boolean requiresPrediction;

    public QueryContext() {
        this.referenceDate = LocalDate.now();
        this.referenceDateTime = LocalDateTime.now();
        this.keywords = new ArrayList<>();
        this.extractedEntities = new HashMap<>();
        this.timeRange = new TimeRange();
    }

    // Getters and Setters
    public String getOriginalQuery() { return originalQuery; }
    public void setOriginalQuery(String originalQuery) { this.originalQuery = originalQuery; }

    public Intent getIntent() { return intent; }
    public void setIntent(Intent intent) { this.intent = intent; }

    public LocalDate getReferenceDate() { return referenceDate; }
    public void setReferenceDate(LocalDate referenceDate) { this.referenceDate = referenceDate; }

    public LocalDateTime getReferenceDateTime() { return referenceDateTime; }
    public void setReferenceDateTime(LocalDateTime referenceDateTime) { this.referenceDateTime = referenceDateTime; }

    public TimeRange getTimeRange() { return timeRange; }
    public void setTimeRange(TimeRange timeRange) { this.timeRange = timeRange; }

    public String getTargetProject() { return targetProject; }
    public void setTargetProject(String targetProject) { this.targetProject = targetProject; }

    public String getTargetGroup() { return targetGroup; }
    public void setTargetGroup(String targetGroup) { this.targetGroup = targetGroup; }

    public String getTargetCategory() { return targetCategory; }
    public void setTargetCategory(String targetCategory) { this.targetCategory = targetCategory; }

    public List<String> getKeywords() { return keywords; }
    public void setKeywords(List<String> keywords) { this.keywords = keywords; }

    public Map<String, Object> getExtractedEntities() { return extractedEntities; }
    public void setExtractedEntities(Map<String, Object> extractedEntities) { this.extractedEntities = extractedEntities; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    public boolean isRequiresHistoricalData() { return requiresHistoricalData; }
    public void setRequiresHistoricalData(boolean requiresHistoricalData) { this.requiresHistoricalData = requiresHistoricalData; }

    public boolean isRequiresPrediction() { return requiresPrediction; }
    public void setRequiresPrediction(boolean requiresPrediction) { this.requiresPrediction = requiresPrediction; }

    public void addKeyword(String keyword) {
        this.keywords.add(keyword.toLowerCase());
    }

    public void addEntity(String key, Object value) {
        this.extractedEntities.put(key, value);
    }
}