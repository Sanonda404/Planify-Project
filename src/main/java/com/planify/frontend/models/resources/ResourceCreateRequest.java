package com.planify.frontend.models.resources;

import com.planify.frontend.models.auth.MemberInfo;

public class ResourceCreateRequest {
    private String name;
    private String description;
    private String type;         // "LINK", "FILE", "IMAGE", "DOCUMENT"
    private String url;          // Web URL or local path
    private MemberInfo addedBy;
    private String addedAt;      // ISO Timestamp
    private String projectUuid;
    private String sourceUuid;   // UUID of the Task it belongs to, can be null
    private String sourceName;   // Name of the Task and milestone linked to it

    public ResourceCreateRequest(String name, String description, String type,
                           String url, MemberInfo addedBy, String addedAt, String projectUuid,
                           String sourceUuid, String sourceName) {
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
}
