package com.edata.scheduler.model;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * @author yugt 2023/6/23
 */
@Data
public class TaskPlan {
    private LocalDate taskDate;

    private List<City> cityList;

}
