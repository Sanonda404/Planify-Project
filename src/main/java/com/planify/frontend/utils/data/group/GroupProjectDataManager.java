package com.planify.frontend.utils.data.group;

import com.planify.frontend.controllers.auth.DashboardController;
import com.planify.frontend.controllers.events.SchedulesController;
import com.planify.frontend.controllers.group.GroupController;
import com.planify.frontend.controllers.group.GroupDetailsController;
import com.planify.frontend.controllers.project.ProjectController;
import com.planify.frontend.controllers.project.ProjectDetailsController;
import com.planify.frontend.controllers.task.TodoController;
import com.planify.frontend.models.auth.MemberInfo;
import com.planify.frontend.models.project.*;
import com.planify.frontend.models.resources.ResourceDetails;
import com.planify.frontend.models.tasks.TaskDetails;
import com.planify.frontend.utils.managers.LocalDataManager;
import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class GroupProjectDataManager {
    private static List<ProjectDetails>projects = new ArrayList<>();

    // --- Core JSON Helpers ---

    public static void init() {
        projects = GroupDataManager.getAllGroupProjects();
    }

    // --- Creators ---

    public static void saveGroupProject(Object data, Object refresher){
        if(data instanceof ProjectDetails projectDetails){
            GroupDataManager.saveNewGroupProject(projectDetails, projectDetails.getGroupUuid());
            refresh(refresher);
        }
    }

    public static void refresh(Object refresher){
        if(refresher instanceof DashboardController)((DashboardController)refresher).refresh();
        else if(refresher instanceof GroupController)((GroupController)refresher).refresh();
        else if(refresher instanceof GroupDetailsController)((GroupDetailsController)refresher).refresh();
        else if(refresher instanceof ProjectController)((ProjectController)refresher).refresh();
        else if(refresher instanceof ProjectDetailsController)((ProjectDetailsController)refresher).refresh();
        else if(refresher instanceof SchedulesController)((SchedulesController)refresher).refresh();
        else if(refresher instanceof TodoController)((TodoController)refresher).refresh();
    }


    public static void saveGroupProjectMilestone(MilestoneDetails milestone) {
        projects.stream().filter(p -> p.getUuid().equals(milestone.getProjectUuid())).findFirst().ifPresent(p -> {
            // 2. Find the "Uncategorized" milestone
            if(!milestone.getTasks().isEmpty()){
                Optional<MilestoneDetails> uncategorizedOpt = p.getMilestones().stream()
                        .filter(m -> m.getTitle().equalsIgnoreCase("Uncategorized"))
                        .findFirst();

                if (uncategorizedOpt.isPresent()) {
                    MilestoneDetails uncategorized = uncategorizedOpt.get();
                    List<TaskDetails>taskDetails = new ArrayList<>();
                    for(TaskDetails t: uncategorized.getTasks()) {
                        if(!milestone.getTasks().contains(t))
                            taskDetails.add(t);
                    }
                    uncategorized.setTasks(taskDetails);
                }
            }
            p.getMilestones().add(milestone);
            GroupDataManager.updateGroupProject(p.getGroupUuid(), p);
        });
    }

    public static void saveGroupProjectMilestone(Object data, Object refresher) {
        if(data instanceof MilestoneDetails milestone){
            projects.stream().filter(p -> p.getUuid().equals(milestone.getProjectUuid())).findFirst().ifPresent(p -> {
                // 2. Find the "Uncategorized" milestone
                if(!milestone.getTasks().isEmpty() && milestone.getTasks()!=null){
                    Optional<MilestoneDetails> uncategorizedOpt = p.getMilestones().stream()
                            .filter(m -> m.getTitle().equalsIgnoreCase("Uncategorized"))
                            .findFirst();

                    if (uncategorizedOpt.isPresent()) {
                        MilestoneDetails uncategorized = uncategorizedOpt.get();
                        List<TaskDetails>taskDetails = new ArrayList<>();
                        for(TaskDetails t: uncategorized.getTasks()) {
                            if(!milestone.getTasks().contains(t))
                                taskDetails.add(t);
                        }
                        uncategorized.setTasks(taskDetails);
                    }
                }
                p.getMilestones().add(milestone);
                GroupDataManager.updateGroupProject(p.getGroupUuid(), p);
            });
            refresh(refresher);
        }

    }

    public static void saveGroupProjectTask(Object data, Object refresher) {
        if(data instanceof TaskDetails task){
            System.out.println(task.getTitle()+" "+task.getProjectName());
            projects.stream().filter(p -> p.getUuid().equals(task.getProjectUuid())).findFirst().ifPresent(p -> {

                if(task.getMilestoneName().isEmpty()){
                    boolean done = false;
                    for(MilestoneDetails m: p.getMilestones()){
                        if(m.getTitle().equals("Uncategorized")){
                            m.getTasks().add(task);
                            done = true;
                        }
                    }
                    List<TaskDetails>tasks = new ArrayList<>();
                    tasks.add(task);
                    if(!done){
                        MilestoneDetails m = new MilestoneDetails("Uncategorized","", task.getMilestoneUuid(),
                                task.getProjectUuid(), false, "", 0, tasks);
                        saveGroupProjectMilestone(m);
                    }
                }
                else{
                    for (MilestoneDetails m: p.getMilestones()){
                        if(m.getUuid().equals(task.getMilestoneUuid())){
                            m.getTasks().add(task);
                        }
                    }
                }
                GroupDataManager.updateGroupProject(p.getGroupUuid(), p);
            });
            refresh(refresher);
        }

    }

    public static void saveGroupProjectResource(Object data, Object refresher){
        if(data instanceof ResourceDetails resource){
            System.out.println("Resource: "+resource.getName());
            projects.stream().filter(p -> p.getUuid().equals(resource.getProjectUuid())).findFirst().ifPresent(p -> {
                p.getResources().add(resource);
                System.out.println("resource size: "+p.getResources().size());
                GroupDataManager.updateGroupProject(p.getGroupUuid(), p);
            });
            refresh(refresher);
        }
    }

    // --- Detailed Getters ---

    /**
     * Gets the full ProjectDetails for a specific project name
     */
    public static ProjectDetails getGroupProjectDetails(String projectUuid) {
        return projects.stream()
                .filter(p -> p.getUuid().equals(projectUuid))
                .findFirst()
                .orElse(null);
    }

    /**
     * Gets all milestones for a project
     */
    public static List<MilestoneDetails> getGroupProjectMilestones(String projectUuid) {
        ProjectDetails project = getGroupProjectDetails(projectUuid);
        return project != null ? project.getMilestones() : new ArrayList<>();
    }

    public static List<TaskDetails> getGroupProjectTasks(String projectUuid) {
        ProjectDetails project = getGroupProjectDetails(projectUuid);
        List<TaskDetails>tasks = new ArrayList<>();
        for(MilestoneDetails m: project.getMilestones()){
            tasks.addAll(m.getTasks());
        }
        System.out.println("Size of tasks: "+tasks.size());
        return tasks;
    }

    /**
     * Gets all tasks for a specific milestone within a project
     */

    public static List<TaskDetails> getAllTasks(){
        List<TaskDetails>tasks = new ArrayList<>();
        for(ProjectDetails p: projects){
            for(MilestoneDetails m: p.getMilestones()){
                tasks.addAll(m.getTasks());
            }
        }
        return tasks;
    }

    public static List<ProjectSummary> getGroupProjectSummary() {
        MemberInfo self = new MemberInfo(LocalDataManager.getUserName(), LocalDataManager.getUserEmail());

        return projects.stream().map(p -> {
            ProjectSummary summary = new ProjectSummary(p.getName(), p.getDescription(), p.getGroupName(), p.getUuid(), 0,1, 0, new ArrayList<>(), List.of(self));

            List<MilestoneSummary> msList = p.getMilestones().stream().map(md -> {
                MilestoneSummary ms = new MilestoneSummary(md.getTitle(), md.getDescription(), "", md.getDeadline(),md.isCompleted()) ;
                return ms;
            }).collect(Collectors.toList());

            int totalMilestone = msList.size();
            for(MilestoneSummary m: msList)if(m.getTitle().equalsIgnoreCase("Uncategorized"))totalMilestone--;
            summary.setMilestones(msList);
            summary.setTotalMilestones(totalMilestone);

            List<TaskDetails> allTasks = p.getMilestones().stream()
                    .flatMap(m -> m.getTasks().stream()).collect(Collectors.toList());
            long total = allTasks.size();
            long done = allTasks.stream().filter(t -> t.getStatus().equals("COMPLETED")).count();
            summary.setProgress(total == 0 ? 0 : (int)(done * 100 / total));

            return summary;
        }).collect(Collectors.toList());
    }

    public static List<ProjectDetails> getAllGroupProjects(){
        return projects;
    }

    public static String getMilestoneName(String taskUuid){
        for(ProjectDetails p: projects){
            for (MilestoneDetails m: p.getMilestones()){
                for(TaskDetails t: m.getTasks()){
                    if(t.getUuid().equals(taskUuid))return m.getTitle();
                }
            }
        }
        return null;
    }
    // --- Updates ---

    public static void updateGroupProjectTaskStatus(Object data, Object refresher) {
        if(data instanceof TaskDetails task){
            Platform.runLater(() -> {
                for (ProjectDetails p : projects) {
                    for (MilestoneDetails m : p.getMilestones()) {
                        m.getTasks().stream()
                                .filter(t -> t.getUuid().equals(task.getUuid()))
                                .forEach(t -> t.setStatus(task.getStatus()));

                        boolean allTasksDone = m.getTasks().stream()
                                .allMatch(t -> t.getStatus().equals("COMPLETED"));
                        m.setCompleted(allTasksDone);
                        GroupDataManager.updateGroupProject(p.getGroupUuid(), p);
                    }
                }
                refresh(refresher);
            });
        }

    }

    public static void updateGroupProjectTask(Object data, Object refresher) {
        if(data instanceof TaskDetails updatedTask){
            for (ProjectDetails p : projects) {
                // 1. Locate the correct project
                if (p.getUuid().equals(updatedTask.getProjectUuid())) {

                    // 2. Remove the task from wherever it was previously
                    for (MilestoneDetails m : p.getMilestones()) {
                        m.getTasks().removeIf(t -> t.getUuid().equals(updatedTask.getUuid()));
                    }

                    // 3. Find the NEW target milestone and add the updated task
                    p.getMilestones().stream()
                            .filter(m -> m.getUuid().equals(updatedTask.getMilestoneUuid()))
                            .findFirst()
                            .ifPresent(targetMilestone -> {
                                targetMilestone.getTasks().add(updatedTask);
                            });
                }
            }
            // 4. Trigger UI Refresh
            refresh(refresher);
        }

    }

    public static void updateGroupProjectMilestone(Object data, Object refresher) {
        if(data instanceof MilestoneDetails updatedMilestone){
            projects.stream().filter(p -> p.getUuid().equals(updatedMilestone.getProjectUuid())).findFirst().ifPresent(p -> {
                for (MilestoneDetails m : p.getMilestones()) {
                    if (m.getUuid().equals(updatedMilestone.getUuid())) {
                        m=updatedMilestone;
                    }
                }
                GroupDataManager.updateGroupProject(p.getGroupUuid(),p);
            });
            refresh(refresher);
        }
    }

    // --- Deletes ---

    public static void deleteGroupProject(ProjectDetails projectDetails) {
        GroupDataManager.deleteGroupProject(projectDetails.getGroupUuid(), projectDetails.getUuid());
    }

    public static void deleteGroupProjectMilestone(String milestoneUuid, Object refresher) {
        Platform.runLater(() -> {
            for (ProjectDetails p : projects) {
                boolean removed = p.getMilestones().removeIf(m -> m.getUuid().equals(milestoneUuid));
                if (removed) {
                    System.out.println("Milestone deleted: " + milestoneUuid + " from project: " + p.getName());
                    GroupDataManager.updateGroupProject(p.getGroupUuid(), p);
                    break;
                }
            }
            refresh(refresher);
        });
    }
    public static void deleteGroupProjectTask(String uuid,  Object refresher) {
            Platform.runLater(() -> {
                boolean taskFoundAndRemoved = false;
                for (ProjectDetails p : projects) {
                        for (MilestoneDetails m : p.getMilestones()) {
                            boolean removed = m.getTasks().removeIf(t -> t.getUuid().equals(uuid));

                            if (removed) {
                                System.out.println("Task removed from UI: " + uuid);
                                taskFoundAndRemoved = true;
                                GroupDataManager.updateGroupProject(p.getGroupUuid(), p);
                                break;
                            }
                        }
                    if (taskFoundAndRemoved) break;
                }
                refresh(refresher);
            });
    }

    private static void showErrorAlert(String error) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Validation Error");
        alert.setHeaderText("Got into this error:");
        alert.setContentText(error);
        alert.showAndWait();
    }

    private static void showSuccessAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}