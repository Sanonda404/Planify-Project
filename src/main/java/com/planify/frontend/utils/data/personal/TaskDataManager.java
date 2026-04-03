package com.planify.frontend.utils.data.personal;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.planify.frontend.models.auth.MemberInfo;
import com.planify.frontend.models.tasks.Category;
import com.planify.frontend.models.tasks.PersonalTaskResponse;
import com.planify.frontend.models.tasks.TaskDetails;
import com.planify.frontend.utils.UserSession;
import com.planify.frontend.utils.helpers.AlertCreator;
import javafx.scene.control.Alert;

import java.io.*;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TaskDataManager {
    private static String DATA_PATH;
    private static String FILE_NAME;
    private static String CATEGORY_FILE_NAME;
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static List<TaskDetails>personalTasks = new ArrayList<>();

    // --- Core JSON Helpers ---
    public static void init(){
        DATA_PATH = System.getProperty("user.home") + "/.planify/"+UserSession.getInstance().getName()+"/personal/tasks";
        FILE_NAME = DATA_PATH + "/tasks.json";
        CATEGORY_FILE_NAME  = DATA_PATH + "/categories.json";
        System.out.println(UserSession.getInstance().getName()+ "for tasks");
        personalTasks = loadAll();
    }

    public static void clearData(){
        personalTasks = new ArrayList<>();
    }

    private static List<TaskDetails> loadAll() {
        File file = new File(FILE_NAME);
        if (!file.exists()) return new ArrayList<>();
        try (Reader reader = new FileReader(file)) {
            Type listType = new TypeToken<ArrayList<TaskDetails>>() {}.getType();
            List<TaskDetails> data = gson.fromJson(reader, listType);
            return data != null ? data : new ArrayList<>();
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    private static void saveAll() {
        File directory = new File(DATA_PATH);
        if (!directory.exists()) directory.mkdirs();
        try (Writer writer = new FileWriter(FILE_NAME)) {
            gson.toJson(personalTasks, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<Category> loadAllCategories(){
        File file = new File(CATEGORY_FILE_NAME);
        if (!file.exists()) return new ArrayList<>();
        try (Reader reader = new FileReader(file)) {
            Type listType = new TypeToken<ArrayList<Category>>() {}.getType();
            List<Category> data = gson.fromJson(reader, listType);
            return data != null ? data : new ArrayList<>();
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    private static void saveAllCategories(List<Category> categories) {
        File directory = new File(DATA_PATH);
        if (!directory.exists()) directory.mkdirs();
        try (Writer writer = new FileWriter(CATEGORY_FILE_NAME)) {
            gson.toJson(categories, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // --- Creators ---

    public static void saveTask(String title, String description, String category, String dueDataTime, String attachmentUrl,
                                int weight, String priority, boolean isDaily){
        MemberInfo self = new MemberInfo(UserSession.getInstance().getName(), UserSession.getInstance().getEmail());
        List<MemberInfo>assignees = new ArrayList<>();
        assignees.add(self);
        TaskDetails task = new TaskDetails("",title,description,dueDataTime,"PENDING",category, isDaily, weight,
                priority.toUpperCase(),"","","","","",self,assignees,attachmentUrl);
        personalTasks.add(task);
        saveAll();
        AlertCreator.showSuccessAlert("Task Created Successfully");
    }

    public static void saveCategory(String projectName, String categoryName){
        List<Category> categories = loadAllCategories();
        boolean found = false;
        for(Category c: categories){
            if(c.getProjectName().equals(projectName)){
                c.addCategory(categoryName);
                found = true;
            }
        }
        if(!found){
            categories.add(new Category(projectName, categoryName));
        }
        saveAllCategories(categories);
    }

    // --- Detailed Getters ---

    /**
     * Gets the full ProjectDetails for a specific project name
     */
    public static List<TaskDetails> getAllPersonalTasks() {
        return personalTasks;
    }

    public static List<String> getProjectCategories(String projectName){
        List<Category> categories = loadAllCategories();
        List<String>categoryNames = new ArrayList<>();
        for(Category c: categories){
            if(c.getProjectName().equals(projectName)){
                categoryNames.addAll(c.getCategoryNames());
            }
        }
        return categoryNames;
    }


    // --- Updates ---

    public static void updatePersonalTaskStatus(String taskName, String status) {
        for(TaskDetails task: personalTasks){
            if(task.getTitle().equals(taskName)){
                task.setStatus(status.toUpperCase());
                if(status.equalsIgnoreCase("COMPLETED")){
                    task.setCompletedAt(LocalDateTime.now().toString());
                }
                AlertCreator.showSuccessAlert("Task Status Updated Successfully");
            }
        }
        saveAll();
    }

    public static void updatePersonalTask(String prevTitle, TaskDetails task) {
        for(TaskDetails t: personalTasks){
            if(t.getTitle().equals(prevTitle)){
                if(!task.getTitle().trim().isEmpty())t.setTitle(task.getTitle());
                t.setDescription(task.getDescription());
                if(!task.getStatus().trim().isEmpty())t.setStatus(task.getStatus());
                t.setAttachmentUrl(task.getAttachmentUrl());
                if(task.getCategory()!=null && !task.getCategory().trim().isEmpty())t.setCategory(task.getCategory());
                if(!task.getTitle().trim().isEmpty())t.setDueDate(task.getDueDate());
                AlertCreator.showSuccessAlert("Task Updated Successfully");
            }
        }
        saveAll();
    }

    // --- Deletes ---


    public static void deletePersonalTask(String taskName) {
        personalTasks.removeIf(taskDetails -> taskDetails.getTitle().equals(taskName));
        saveAll();
    }
}