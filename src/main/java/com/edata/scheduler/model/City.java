package com.edata.scheduler.model;

import lombok.Data;

import java.util.List;

/**
 * 城市任务实体类
 * @author yugt 2023/6/23
 */
@Data
public class City {
    private String cityName;
    /** 当前任务列表 */
    private List<Task> taskList;
}
