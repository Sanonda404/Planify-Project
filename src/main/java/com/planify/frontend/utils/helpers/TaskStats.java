package com.planify.frontend.utils.helpers;

import com.planify.frontend.models.tasks.PersonalTaskResponse;
import com.planify.frontend.models.tasks.TaskDetails;
import com.planify.frontend.utils.data.group.GroupProjectDataManager;
import com.planify.frontend.utils.data.personal.TaskDataManager;

import java.time.LocalDateTime;
import java.util.List;

public class TaskStats {
    private static List<TaskDetails> projectTasks = GroupProjectDataManager.getAllTasks();
    private static List<TaskDetails> personalTasks = TaskDataManager.getAllPersonalTasks();

    /*public static int getNumberOfTasksCompletedAt(LocalDateTime dateTime){

    }

    public static int getNumberOfTasksCompletedAt(LocalDateTime dateTime){

    }*/

    public static class TaskStat{
        public String status;
        public LocalDateTime start;
        public LocalDateTime end;
    }
}
