package com.planify.frontend.utils.helpers;

import com.planify.frontend.models.project.MilestoneDetails;
import com.planify.frontend.models.project.ProjectDetails;
import com.planify.frontend.models.tasks.TaskDetails;
import javafx.concurrent.Task;

import java.util.*;
import java.util.stream.Collectors;

public class ProjectSorter {

    public static void sortProject(ProjectDetails projectDetails) {
        // 1. Sort milestones alphabetically
        projectDetails.getMilestones().sort(
                Comparator.comparing(MilestoneDetails::getTitle, String.CASE_INSENSITIVE_ORDER)
        );

        // 2. Sort tasks inside each milestone
        for (MilestoneDetails milestone : projectDetails.getMilestones()) {
            milestone.getTasks().sort(
                    Comparator
                            .comparing(TaskDetails::getStatus, Comparator.comparingInt(ProjectSorter::statusPriority))
                            .thenComparing(TaskDetails::getTitle, String.CASE_INSENSITIVE_ORDER)
            );
        }
    }

    // Helper: assign numeric priority to statuses
    private static int statusPriority(String status) {
        switch (status) {
            case "IN_PROGRESS": return 1;
            case "PENDING" :     return 2;
            case "COMPLETED":   return 3;
            default:          return 4;
        }
    }
}
