package com.planify.frontend.models.tasks;

import java.util.ArrayList;
import java.util.List;

public class Category {
    private String projectName;
    private List<String>categoryNames;
    public Category(String projectName, String categoryName){
        this.projectName = projectName;
        categoryNames = new ArrayList<>();
        this.categoryNames.add(categoryName);
    }

    public Category(String projectName){
        this.projectName=  projectName;
        categoryNames = new ArrayList<>();
    }

    public void addCategory(String categoryName){
        categoryNames.add(categoryName);
    }

    public String getProjectName() {
        return projectName;
    }

    public List<String> getCategoryNames() {
        return categoryNames;
    }
}
