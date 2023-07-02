package com.edata.scheduler.model;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 模拟任务计划库
 * @author yugt 2023/6/23
 */
@Data
public class CityTaskPool {
    private static Map<LocalDate,List<City>> cityTaskMap = new ConcurrentHashMap<>();

    public static void addCityTask(LocalDate dateKey, List<City> cityList){
        cityTaskMap.put(dateKey,cityList);
    }

    public static List<City> getCityTaskByDate(LocalDate dateKey){
        return cityTaskMap.get(dateKey);
    }

    public static List<City> getTodayCityTask(){
        LocalDate today = LocalDate.now();
        return cityTaskMap.get(today);
    }

}
