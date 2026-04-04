package com.planify.frontend.utils.data.personal;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.planify.frontend.models.auth.MemberInfo;
import com.planify.frontend.models.project.MilestoneDetails;
import com.planify.frontend.models.project.MilestoneSummary;
import com.planify.frontend.models.project.ProjectDetails;
import com.planify.frontend.models.project.ProjectSummary;
import com.planify.frontend.models.resources.ResourceDetails;
import com.planify.frontend.models.tasks.TaskDetails;
import com.planify.frontend.utils.UserSession;
import com.planify.frontend.utils.data.group.GroupProjectDataManager;
import com.planify.frontend.utils.managers.LocalDataManager;

import java.io.*;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ProjectDataManager {
    private static String DATA_PATH;
    private static String FILE_NAME;
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static List<ProjectDetails> projects = new ArrayList<>();

    // --- Core JSON Helpers ---
    public static void init(){
       DATA_PATH = System.getProperty("user.home") + "/.planify/"+ UserSession.getInstance().getName()+"personal/projects";
        FILE_NAME  = DATA_PATH + "/projects.json";
       projects = loadAll();
    }

    private static List<ProjectDetails> loadAll() {
        File file = new File(FILE_NAME);
        if (!file.exists()) return new ArrayList<>();
        try (Reader reader = new FileReader(file)) {
            Type listType = new TypeToken<ArrayList<ProjectDetails>>() {}.getType();
            List<ProjectDetails> data = gson.fromJson(reader, listType);
            return data != null ? data : new ArrayList<>();
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    public static void clearData(){
        projects = new ArrayList<>();
    }

    private static void saveAll() {
        File directory = new File(DATA_PATH);
        if (!directory.exists()) directory.mkdirs();
        try (Writer writer = new FileWriter(FILE_NAME)) {
            gson.toJson(projects, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // --- Creators ---

    public static void savePersonalProject(String name, String description, String start, String deadline) {
        List<MemberInfo> memberInfos = new ArrayList<>();
        memberInfos.add(new MemberInfo(LocalDataManager.getUserName(), LocalDataManager.getUserEmail()));
        ProjectDetails project = new ProjectDetails(name, description, start, deadline, "Personal", "", "", 0, 0, 1, 0, new ArrayList<>(), new ArrayList<>(), memberInfos, false);
        projects.add(project);
        saveAll();
    }

    public static void savePersonalProjectMilestone(String title, String description, String deadline, String projectName) {
        projects.stream().filter(p -> p.getName().equals(projectName)).findFirst().ifPresent(p -> {
            MilestoneDetails m = new MilestoneDetails(title, description, "", "", false, deadline, 0, new ArrayList<>());
            p.getMilestones().add(m);
        });
        saveAll();
    }

    public static void savePersonalProjectMilestone(String title, String description, String deadline, String projectName, List<String> taskNamesToAssign) {

        projects.stream().filter(p -> p.getName().equals(projectName)).findFirst().ifPresent(p -> {
            // 1. Create the new milestone
            MilestoneDetails newMilestone = new MilestoneDetails(title, description, "", "",false, deadline, 0, new ArrayList<>());

            // 2. Find the "Uncategorized" milestone
            Optional<MilestoneDetails> uncategorizedOpt = p.getMilestones().stream()
                    .filter(m -> m.getTitle().equalsIgnoreCase("Uncategorized"))
                    .findFirst();

            if (uncategorizedOpt.isPresent()) {
                MilestoneDetails uncategorized = uncategorizedOpt.get();

                // 3. Extract the tasks that SHOULD be moved to the new milestone
                List<TaskDetails> tasksToMove = uncategorized.getTasks().stream()
                        .filter(t -> taskNamesToAssign.contains(t.getTitle()))
                        .peek(t -> t.setMilestoneName(title)) // Update their parent name pointer
                        .toList();

                // 4. Update the new milestone with these tasks
                newMilestone.setTasks(new ArrayList<>(tasksToMove));

                // 5. REWRITE Uncategorized tasks: Keep only those NOT in the name list
                List<TaskDetails> remainingTasks = uncategorized.getTasks().stream()
                        .filter(t -> !taskNamesToAssign.contains(t.getTitle()))
                        .toList();

                uncategorized.setTasks(new ArrayList<>(remainingTasks));
            }

            // 6. Add the new milestone to the project
            p.getMilestones().add(newMilestone);
        });

        saveAll();
    }

    public static void savePersonalProjectTask(String title, String description, String category, String dueDate, boolean isDaily, int weight, String priority, String projectName, String milestoneName, String attachmentUrl) {
        List<MemberInfo> memberInfos = new ArrayList<>();
        memberInfos.add(new MemberInfo(LocalDataManager.getUserName(), LocalDataManager.getUserEmail()));
        projects.stream().filter(p -> p.getName().equals(projectName)).findFirst().ifPresent(p -> {
            TaskDetails t = new TaskDetails("", title, description, dueDate, "PENDING", category, isDaily, weight, priority, "", "", "",
                    (milestoneName == null || milestoneName.isEmpty() ? "Uncategorized" : milestoneName), projectName,
                    new MemberInfo(LocalDataManager.getUserName(), LocalDataManager.getUserEmail()), memberInfos, attachmentUrl);

            String targetMilestone = t.getMilestoneName();
            p.getMilestones().stream()
                    .filter(m -> m.getTitle().equals(targetMilestone))
                    .findFirst()
                    .ifPresentOrElse(m -> m.getTasks().add(t), () -> {
                        // Create Uncategorized milestone if it doesn't exist
                        MilestoneDetails uncategorized = new MilestoneDetails("Uncategorized", "", "", "",false, "", 0, new ArrayList<>());
                        uncategorized.getTasks().add(t);
                        p.getMilestones().add(uncategorized);
                    });
        });
        saveAll();
    }

    public static void savePersonalProjectResource(String title, String description, String type, String url, String projectName, String soutceName){
        for(ProjectDetails p: projects){
            if(p.getName().equals(projectName)){
                MemberInfo self = new MemberInfo(UserSession.getInstance().getName(), UserSession.getInstance().getEmail());
                ResourceDetails resourceDetails = new ResourceDetails("",title,description,type,url,self,
                        LocalDateTime.now().toString(),"","",soutceName);
                p.getResources().add(resourceDetails);
            }
        }
    }

    // --- Detailed Getters ---

    /**
     * Gets the full ProjectDetails for a specific project name
     */
    public static ProjectDetails getPersonalProjectDetails(String projectName) {
        return projects.stream()
                .filter(p -> p.getName().equals(projectName))
                .findFirst()
                .orElse(null);
    }

    /**
     * Gets all milestones for a project
     */
    public static List<MilestoneDetails> getPersonalMilestones(String projectName) {
        ProjectDetails project = getPersonalProjectDetails(projectName);
        return project != null ? project.getMilestones() : new ArrayList<>();
    }

    /**
     * Gets all tasks for a specific milestone within a project
     */
    public static List<TaskDetails> getPersonalProjectTasks(String projectName, String milestoneName) {
        return getPersonalMilestones(projectName).stream()
                .filter(m -> m.getTitle().equals(milestoneName))
                .findFirst()
                .map(MilestoneDetails::getTasks)
                .orElse(new ArrayList<>());
    }

    public static List<TaskDetails> getAllTasks(){
        List<TaskDetails>tasks = new ArrayList<>();
        if(projects==null)return tasks;
        for(ProjectDetails p: projects){
            for(MilestoneDetails m: p.getMilestones()){
                tasks.addAll(m.getTasks());
            }
        }
        return tasks;
    }

    public static List<ProjectSummary> getPersonalProjectSummary() {
        MemberInfo self = new MemberInfo(LocalDataManager.getUserName(), LocalDataManager.getUserEmail());

        return projects.stream().map(p -> {
            ProjectSummary summary = new ProjectSummary(p.getName(), p.getDescription(), "Personal", "", 0,1, 0, new ArrayList<>(), List.of(self));

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

    public static List<ProjectDetails> getAllPersonalProjects(){
        return projects;
    }

    public static List<TaskDetails> getAllPersonalProjectTasks(String projectName){
        List<TaskDetails>tasks = new ArrayList<>();
        for(ProjectDetails p: projects){
            for(MilestoneDetails m: p.getMilestones()){
                tasks.addAll(m.getTasks());
            }
        }
        return tasks;
    }
    // --- Updates ---

    public static void updatePersonalTaskStatus(String projectName, String milestoneName, String taskName, String status) {
        for (ProjectDetails p : projects) {
            if (p.getName().equals(projectName)) {
                for (MilestoneDetails m : p.getMilestones()) {
                    if (m.getTitle().equals(milestoneName)) {
                        m.getTasks().stream()
                                .filter(t -> t.getTitle().equals(taskName))
                                .forEach(t -> t.setStatus(status));

                        boolean allTasksDone = m.getTasks().stream()
                                .allMatch(t -> t.getStatus().equals("COMPLETED"));
                        m.setCompleted(allTasksDone);
                        p.setProgress(GroupProjectDataManager.calculateProjectProgress(p));
                    }
                }
            }
        }
        saveAll();
    }

    public static void updatePersonalTask(String prevTitle, TaskDetails updatedTask) {

        for (ProjectDetails project : projects) {

            for (MilestoneDetails milestone : project.getMilestones()) {

                TaskDetails taskToUpdate = milestone.getTasks().stream()
                        .filter(t -> t.getTitle().equals(prevTitle))
                        .findFirst()
                        .orElse(null);


                if (taskToUpdate != null) {
                    // Update fields
                    if (updatedTask.getDescription() != null) taskToUpdate.setDescription(updatedTask.getDescription());
                    if (updatedTask.getCategory() != null) taskToUpdate.setCategory(updatedTask.getCategory());
                    if (updatedTask.getDueDate() != null) taskToUpdate.setDueDate(updatedTask.getDueDate());
                    if (updatedTask.getStatus() != null) taskToUpdate.setStatus(updatedTask.getStatus());
                    if (updatedTask.getAttachmentUrl() != null) taskToUpdate.setAttachmentUrl(updatedTask.getAttachmentUrl());

                    // Check milestone change
                    if (!updatedTask.getMilestoneName().equals(taskToUpdate.getMilestoneName())) {
                        // Remove from old milestone
                        milestone.getTasks().remove(taskToUpdate);

                        // Find new milestone and add
                        project.getMilestones().stream()
                                .filter(m -> m.getTitle().equals(updatedTask.getMilestoneName()))
                                .findFirst()
                                .ifPresent(newMilestone -> {
                                    taskToUpdate.setMilestoneName(updatedTask.getMilestoneName());
                                    newMilestone.getTasks().add(taskToUpdate);
                                });
                    }
                }
            }
        }

        saveAll();
    }

    public static void updatePersonalMilestone(MilestoneDetails updatedMilestone) {
        String projectName = updatedMilestone.getUuid();
        projects.stream().filter(p -> p.getName().equals(projectName)).findFirst().ifPresent(p -> {
            for (MilestoneDetails m : p.getMilestones()) {
                if (m.getTitle().equals(updatedMilestone.getTitle())) {
                    if (updatedMilestone.getDescription() != null) m.setDescription(updatedMilestone.getDescription());
                    if (updatedMilestone.getDeadline() != null) m.setDeadline(updatedMilestone.getDeadline());
                    m.setCompleted(updatedMilestone.isCompleted());
                }
            }
        });
        saveAll();
    }

    // --- Deletes ---

    public static void deletePersonalProject(String projectName) {
        projects.removeIf(p -> p.getName().equals(projectName));
        saveAll();
    }

    public static void deletePersonalProjectMilestone(String projectName, String milestoneName) {
        projects.stream().filter(p -> p.getName().equals(projectName)).findFirst().ifPresent(p -> {
            p.getMilestones().removeIf(m -> m.getTitle().equals(milestoneName));
            p.setProgress(GroupProjectDataManager.calculateProjectProgress(p));
        });
        saveAll();
    }

    public static void deletePersonalProjectTask(String projectName, String milestoneName, String taskName) {
        projects.stream().filter(p -> p.getName().equals(projectName)).findFirst().ifPresent(p -> {
            p.getMilestones().stream().filter(m -> m.getTitle().equals(milestoneName)).findFirst().ifPresent(m -> {
                m.getTasks().removeIf(t -> t.getTitle().equals(taskName));
            });
            p.setProgress(GroupProjectDataManager.calculateProjectProgress(p));
        });
        saveAll();
    }

}