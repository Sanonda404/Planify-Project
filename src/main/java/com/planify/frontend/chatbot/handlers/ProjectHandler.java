// handlers/ProjectHandler.java
package com.planify.frontend.chatbot.handlers;

import com.planify.frontend.chatbot.models.QueryContext;
import com.planify.frontend.chatbot.utils.DateUtils;
import com.planify.frontend.chatbot.utils.ResponseFormatter;
import com.planify.frontend.models.project.MilestoneDetails;
import com.planify.frontend.models.project.ProjectDetails;
import com.planify.frontend.models.tasks.TaskDetails;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class ProjectHandler {

    private final List<ProjectDetails> projects;

    public ProjectHandler(List<ProjectDetails> projects) {
        this.projects = projects;
    }

    public String handle(QueryContext context) {
        List<ProjectDetails> filteredProjects = filterProjects(context);

        if (filteredProjects.isEmpty()) {
            return "No projects found.";
        }

        switch (context.getIntent()) {
            case PROJECT_LIST:
                return formatProjectList(filteredProjects);
            case PROJECT_PROGRESS:
                return formatProjectProgress(filteredProjects, context);
            case PROJECT_DETAILS:
                return formatProjectDetails(filteredProjects, context);
            case PROJECT_MILESTONES:
                return formatProjectMilestones(filteredProjects, context);
            case PROJECT_VELOCITY:
                return formatProjectVelocity(filteredProjects, context);
            case PROJECT_COMPLETION_DATE:
                return formatProjectCompletionDate(filteredProjects, context);
            default:
                return formatProjectList(filteredProjects);
        }
    }

    private List<ProjectDetails> filterProjects(QueryContext context) {
        List<ProjectDetails> result = new ArrayList<>(projects);

        // Filter by name
        if (context.getTargetProject() != null) {
            result = result.stream()
                    .filter(p -> p.getName().toLowerCase().contains(context.getTargetProject().toLowerCase()))
                    .collect(Collectors.toList());
        }

        // Filter by group
        if (context.getTargetGroup() != null) {
            result = result.stream()
                    .filter(p -> p.getGroupName() != null &&
                            p.getGroupName().toLowerCase().contains(context.getTargetGroup().toLowerCase()))
                    .collect(Collectors.toList());
        }

        return result;
    }

    private String formatProjectList(List<ProjectDetails> projects) {
        List<ProjectDetails> activeProjects = projects.stream()
                .filter(p -> !p.isCompleted())
                .collect(Collectors.toList());

        List<ProjectDetails> completedProjects = projects.stream()
                .filter(ProjectDetails::isCompleted)
                .collect(Collectors.toList());

        StringBuilder response = new StringBuilder();
        response.append("📁 **Projects Overview**\n\n");

        if (!activeProjects.isEmpty()) {
            response.append("**Active Projects**\n");
            for (ProjectDetails project : activeProjects) {
                response.append("• **").append(project.getName()).append("**\n");
                response.append("  Progress: ").append(project.getProgress()).append("%\n");
                response.append("  Tasks: ").append(project.getCompletedTasks()).append("/")
                        .append(project.getTotalTasks()).append(" completed\n");
                response.append("  Deadline: ").append(DateUtils.formatDate(project.getDeadline())).append("\n");
                response.append("\n");
            }
        }

        if (!completedProjects.isEmpty()) {
            response.append("**Completed Projects** ✓\n");
            for (ProjectDetails project : completedProjects) {
                response.append("• ").append(project.getName()).append("\n");
            }
        }

        return response.toString();
    }

    private String formatProjectProgress(List<ProjectDetails> projects, QueryContext context) {
        ProjectDetails project = projects.get(0);

        StringBuilder response = new StringBuilder();
        response.append("📈 **Project Progress: ").append(project.getName()).append("**\n");
        response.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");

        // Progress bar
        int progress = project.getProgress();
        response.append("Overall Completion: **").append(progress).append("%**\n");
        response.append(ResponseFormatter.createProgressBar(progress)).append("\n\n");

        // Statistics
        response.append("**Statistics**\n");
        response.append("• Milestones: ").append(project.getCompletedMilestones()).append("/")
                .append(project.getTotalMilestones()).append(" completed\n");
        response.append("• Tasks: ").append(project.getCompletedTasks()).append("/")
                .append(project.getTotalTasks()).append(" completed\n");
        response.append("• Members: ").append(project.getTotalMembers()).append("\n\n");

        // Deadline status
        LocalDate deadline = DateUtils.parseDate(project.getDeadline());
        if (deadline != null) {
            LocalDate today = LocalDate.now();
            long daysUntil = today.until(deadline).getDays();

            if (project.isCompleted()) {
                response.append("✅ **Project Completed!** Great work! 🎉\n");
            } else if (daysUntil < 0) {
                response.append("⚠️ **OVERDUE by ").append(Math.abs(daysUntil)).append(" days!**\n");
            } else if (daysUntil <= 3) {
                response.append("⚠️ **CRITICAL: ").append(daysUntil).append(" days remaining!**\n");
            } else if (daysUntil <= 7) {
                response.append("⚠️ **Deadline approaching in ").append(daysUntil).append(" days**\n");
            } else {
                response.append("📅 Deadline: ").append(DateUtils.formatDate(project.getDeadline()));
                response.append(" (").append(daysUntil).append(" days remaining)\n");
            }
        }

        return response.toString();
    }

    private String formatProjectDetails(List<ProjectDetails> projects, QueryContext context) {
        ProjectDetails project = projects.get(0);

        StringBuilder response = new StringBuilder();
        response.append("📋 **Project Details: ").append(project.getName()).append("**\n");
        response.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");

        response.append("**Description**\n");
        response.append(project.getDescription() != null ? project.getDescription() : "No description provided");
        response.append("\n\n");

        response.append("**Timeline**\n");
        response.append("• Start Date: ").append(DateUtils.formatDate(project.getStartDate())).append("\n");
        response.append("• Deadline: ").append(DateUtils.formatDate(project.getDeadline())).append("\n\n");

        response.append("**Team**\n");
        response.append("• Group: ").append(project.getGroupName() != null ? project.getGroupName() : "Personal Project").append("\n");
        response.append("• Members: ").append(project.getTotalMembers()).append("\n\n");

        return response.toString();
    }

    private String formatProjectMilestones(List<ProjectDetails> projects, QueryContext context) {
        ProjectDetails project = projects.get(0);
        List<MilestoneDetails> milestones = project.getMilestones();

        if (milestones.isEmpty()) {
            return "No milestones defined for this project yet.";
        }

        StringBuilder response = new StringBuilder();
        response.append("🏆 **Milestones: ").append(project.getName()).append("**\n\n");

        for (MilestoneDetails milestone : milestones) {
            String status = milestone.isCompleted() ? "✅" : "⏳";
            response.append(status).append(" **").append(milestone.getTitle()).append("**\n");
            response.append("  Completion: ").append(milestone.getCompletionRate()).append("%\n");
            response.append("  Tasks: ").append(milestone.getCompletedTasks()).append("/")
                    .append(milestone.getTotalTasks()).append(" completed\n");
            if (milestone.getDeadline() != null) {
                response.append("  Deadline: ").append(DateUtils.formatDate(milestone.getDeadline())).append("\n");
            }
            response.append("\n");
        }

        return response.toString();
    }

    private String formatProjectVelocity(List<ProjectDetails> projects, QueryContext context) {
        ProjectDetails project = projects.get(0);

        // Calculate velocity based on task completion rate
        int totalTasks = project.getTotalTasks();
        int completedTasks = project.getCompletedTasks();
        LocalDate deadline = DateUtils.parseDate(project.getDeadline());
        LocalDate startDate = DateUtils.parseDate(project.getStartDate());

        if (deadline == null || startDate == null || totalTasks == 0) {
            return "Unable to calculate velocity for this project. Missing timeline or task data.";
        }

        LocalDate today = LocalDate.now();
        long totalDays = startDate.until(deadline).getDays();
        long daysElapsed = startDate.until(today).getDays();
        long daysRemaining = today.until(deadline).getDays();

        double tasksPerDay = (double) totalTasks / totalDays;
        double completedTasksPerDay = (double) completedTasks / Math.max(1, daysElapsed);
        double expectedProgress = tasksPerDay * daysElapsed;
        double requiredRate = (double) (totalTasks - completedTasks) / Math.max(1, daysRemaining);

        StringBuilder response = new StringBuilder();
        response.append("🚀 **Project Velocity: ").append(project.getName()).append("**\n\n");

        response.append("**Metrics**\n");
        response.append("• Total Tasks: ").append(totalTasks).append("\n");
        response.append("• Completed: ").append(completedTasks).append("\n");
        response.append("• Remaining: ").append(totalTasks - completedTasks).append("\n\n");

        response.append("**Velocity Analysis**\n");
        response.append("• Average completion rate: ").append(String.format("%.1f", completedTasksPerDay));
        response.append(" tasks/day\n");
        response.append("• Required completion rate: ").append(String.format("%.1f", requiredRate));
        response.append(" tasks/day\n\n");

        // Prediction
        if (requiredRate > tasksPerDay) {
            response.append("⚠️ **Warning**: You need to work ").append(String.format("%.1f", requiredRate / tasksPerDay * 100));
            response.append("% faster to meet the deadline!\n");
        } else if (completedTasks >= expectedProgress) {
            response.append("✅ **On track!** You're making good progress.\n");
        } else {
            response.append("⚠️ **Behind schedule**: You're ");
            response.append(String.format("%.1f", (expectedProgress - completedTasks) / tasksPerDay * 100));
            response.append("% behind the ideal pace.\n");
        }

        return response.toString();
    }

    private String formatProjectCompletionDate(List<ProjectDetails> projects, QueryContext context) {
        ProjectDetails project = projects.get(0);

        if (project.isCompleted()) {
            return "Project '" + project.getName() + "' is already completed! 🎉";
        }

        // Calculate estimated completion date based on current velocity
        int totalTasks = project.getTotalTasks();
        int completedTasks = project.getCompletedTasks();
        int remainingTasks = totalTasks - completedTasks;

        LocalDate startDate = DateUtils.parseDate(project.getStartDate());
        if (startDate == null) {
            return "Unable to estimate completion date. Project start date not set.";
        }

        LocalDate today = LocalDate.now();
        long daysElapsed = startDate.until(today).getDays();
        double tasksPerDay = (double) completedTasks / Math.max(1, daysElapsed);

        if (tasksPerDay <= 0) {
            return "Not enough data to estimate completion date. Start working on tasks!";
        }

        long estimatedDaysRemaining = (long) Math.ceil(remainingTasks / tasksPerDay);
        LocalDate estimatedCompletion = today.plusDays(estimatedDaysRemaining);

        StringBuilder response = new StringBuilder();
        response.append("🔮 **Estimated Completion for ").append(project.getName()).append("**\n\n");
        response.append("Based on your current progress rate of ");
        response.append(String.format("%.1f", tasksPerDay)).append(" tasks/day:\n");
        response.append("• Estimated completion: **").append(DateUtils.formatDate(String.valueOf(estimatedCompletion))).append("**\n");

        LocalDate deadline = DateUtils.parseDate(project.getDeadline());
        if (deadline != null) {
            if (estimatedCompletion.isAfter(deadline)) {
                long daysOver = deadline.until(estimatedCompletion).getDays();
                response.append("⚠️ **Warning**: Projection shows it will be ");
                response.append(daysOver).append(" days past the deadline!\n");
                response.append("Consider increasing your pace or adjusting scope.\n");
            } else if (estimatedCompletion.isBefore(deadline)) {
                response.append("✅ You're on track to finish early!\n");
            }
        }

        return response.toString();
    }
}