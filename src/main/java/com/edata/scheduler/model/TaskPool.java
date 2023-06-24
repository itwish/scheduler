package com.edata.scheduler.model;

import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 模拟任务计划库
 * @author yugt 2023/6/23
 */
@Data
public class TaskPool {
    private static List<TaskPlan> taskPlanList = new ArrayList<>();

    private static List<TaskPlan> getTaskPlanList(){
        return taskPlanList;
    }

    public static void addTaskPlan(TaskPlan taskPlan){
        taskPlanList.add(taskPlan);
    }

    public static TaskPlan getTaskPlanByDate(LocalDate date){
        if(date==null && taskPlanList.size()>0){
            return taskPlanList.get(taskPlanList.size()-1);
        } else {
            return taskPlanList.stream()
                    .filter(x->x.getTaskDate().equals(date))
                    .findFirst()
                    .get();
        }
    }

}
