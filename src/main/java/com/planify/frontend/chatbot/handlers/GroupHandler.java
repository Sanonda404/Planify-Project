// handlers/GroupHandler.java
package com.planify.frontend.chatbot.handlers;

import com.planify.frontend.chatbot.models.QueryContext;
import com.planify.frontend.chatbot.utils.ResponseFormatter;
import com.planify.frontend.models.group.GroupDetails;
import com.planify.frontend.models.group.GroupMember;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class GroupHandler {

    private final List<GroupDetails> groups;

    public GroupHandler(List<GroupDetails> groups) {
        this.groups = groups;
    }

    public String handle(QueryContext context) {
        List<GroupDetails> filteredGroups = filterGroups(context);

        if (filteredGroups.isEmpty()) {
            return "No groups found.";
        }

        switch (context.getIntent()) {
            case GROUP_LIST:
                return formatGroupList(filteredGroups);
            case GROUP_MEMBERS:
                return formatGroupMembers(filteredGroups, context);
            case GROUP_EVENTS:
                return formatGroupEvents(filteredGroups, context);
            case GROUP_ROLE:
                return formatGroupRole(filteredGroups, context);
            default:
                return formatGroupList(filteredGroups);
        }
    }

    private List<GroupDetails> filterGroups(QueryContext context) {
        if (context.getTargetGroup() != null) {
            return groups.stream()
                    .filter(g -> g.getName().toLowerCase().contains(context.getTargetGroup().toLowerCase()))
                    .collect(Collectors.toList());
        }
        return groups;
    }

    private String formatGroupList(List<GroupDetails> groups) {
        StringBuilder response = new StringBuilder();
        response.append("👥 **Your Groups**\n\n");

        for (GroupDetails group : groups) {
            response.append("• **").append(group.getName()).append("**\n");
            response.append("  Type: ").append(group.getGroupType()).append("\n");
            response.append("  Role: ").append(group.getRole() != null ? group.getRole() : "Member").append("\n");
            if (group.getMembers() != null) {
                response.append("  Members: ").append(group.getMembers().size()).append("\n");
            }
            response.append("\n");
        }

        return response.toString();
    }

    private String formatGroupMembers(List<GroupDetails> groups, QueryContext context) {
        GroupDetails group = groups.get(0);

        if (group.getMembers() == null || group.getMembers().isEmpty()) {
            return "No member information available for " + group.getName();
        }

        StringBuilder response = new StringBuilder();
        response.append("👥 **Members of ").append(group.getName()).append("**\n\n");

        // Find admin/moderator roles
        List<GroupMember> admins = group.getMembers().stream()
                .filter(m -> m.getRole() != null && m.getRole().toLowerCase().contains("admin"))
                .collect(Collectors.toList());

        List<GroupMember> regularMembers = group.getMembers().stream()
                .filter(m -> m.getRole() == null || !m.getRole().toLowerCase().contains("admin"))
                .collect(Collectors.toList());

        if (!admins.isEmpty()) {
            response.append("**Admins/Moderators**\n");
            for (GroupMember member : admins) {
                response.append("• ").append(member.getName());
                if (member.getEmail() != null) {
                    response.append(" (").append(member.getEmail()).append(")");
                }
                response.append("\n");
            }
            response.append("\n");
        }

        if (!regularMembers.isEmpty()) {
            response.append("**Members** (").append(regularMembers.size()).append(")\n");
            for (GroupMember member : regularMembers) {
                response.append("• ").append(member.getName());
                if (member.getEmail() != null) {
                    response.append(" (").append(member.getEmail()).append(")");
                }
                response.append("\n");
            }
        }

        return response.toString();
    }

    private String formatGroupEvents(List<GroupDetails> groups, QueryContext context) {
        // This would need access to events filtered by group
        return "Events for " + groups.get(0).getName() + " can be viewed in your main calendar with group filtering.";
    }

    private String formatGroupRole(List<GroupDetails> groups, QueryContext context) {
        GroupDetails group = groups.get(0);

        StringBuilder response = new StringBuilder();
        response.append("🔑 **Your Role in ").append(group.getName()).append("**\n\n");
        response.append("Role: **").append(group.getRole() != null ? group.getRole() : "Member").append("**\n\n");

        if (group.getRole() != null && group.getRole().toLowerCase().contains("admin")) {
            response.append("As an admin, you can:\n");
            response.append("• Add or remove members\n");
            response.append("• Create and manage events\n");
            response.append("• Change group settings\n");
            response.append("• Assign roles to other members\n");
        } else {
            response.append("As a member, you can:\n");
            response.append("• View group events and projects\n");
            response.append("• Participate in group discussions\n");
            response.append("• Create tasks and contribute to projects\n");
        }

        return response.toString();
    }
}