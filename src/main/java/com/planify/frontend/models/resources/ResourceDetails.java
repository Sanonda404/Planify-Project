package com.planify.frontend.models.resources;

import com.planify.frontend.models.auth.MemberInfo;
import java.awt.Desktop;
import java.net.URI;

public class ResourceDetails {
    private String uuid;
    private String name;
    private String description;
    private String type;         // "LINK", "FILE", "IMAGE", "DOCUMENT"
    private String url;          // Web URL or local path
    private MemberInfo addedBy;
    private String addedAt;      // ISO Timestamp
    private String projectUuid;
    private String sourceUuid;   // UUID of the Task or Project it belongs to
    private String sourceName;   // Name of the Task or Project (for Bot context)

    public ResourceDetails(String uuid, String name, String description, String type,
                           String url, MemberInfo addedBy, String addedAt, String projectUuid,
                           String sourceUuid, String sourceName) {
        this.uuid = uuid;
        this.name = name;
        this.description = description;
        this.type = type;
        this.url = url;
        this.addedBy = addedBy;
        this.addedAt = addedAt;
        this.projectUuid = projectUuid;
        this.sourceUuid = sourceUuid;
        this.sourceName = sourceName;
    }

    /**
     * Utility method to open the resource in the system's default browser or app.
     */
    public void openResource() {
        if (url == null || url.isEmpty()) return;
        try {
            if (url.startsWith("http")) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                // For local files
                Desktop.getDesktop().open(new java.io.File(url));
            }
        } catch (Exception e) {
            System.err.println("Could not open resource: " + e.getMessage());
        }
    }

    // Standard Getters and Setters
    public String getUuid() { return uuid; }
    public String getName() { return name; }
    public String getType() { return type; }
    public String getUrl() { return url; }
    public String getProjectUuid(){
        return projectUuid;
    }
    public MemberInfo getAddedBy() { return addedBy; }
    public String getSourceUuid() { return sourceUuid; }
    public String getSourceName() { return sourceName; }
}
