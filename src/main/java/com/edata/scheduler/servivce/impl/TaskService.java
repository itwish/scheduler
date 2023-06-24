package com.edata.scheduler.servivce.impl;

import com.edata.scheduler.model.*;
import com.edata.scheduler.servivce.ITaskService;
import com.edata.scheduler.vo.TaskVO;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

import static com.edata.scheduler.cache.DeviceStateCache.getDeviceState;

/**
 * 任务拉取服务
 *
 * @author yugt 2023/6/23
 */
@Service
public class TaskService implements ITaskService {
    Map<String, List<City>> deviceCityMap = new HashMap<>();

    /**
     * 尽可能保证每个设备每次拉的任务是同一个城市的
     * 每个设备每天最多4个任务
     * 每个设备每天最多切换2个城市
     * 设备和城市要有对应关系 即一个设备今天是A城市 明天也是A城市 （第二天有A城市的任务的话）
     *
     * @param deviceId 设备Id
     * @return
     */
    @Override
    public TaskVO getTask(String deviceId) {
        // 获取当天任务计划
        LocalDate today = LocalDate.now();
        TaskPlan taskPlan = TaskPool.getTaskPlanByDate(today);
        // 获取所有城市列表
        List<City> allCityList = taskPlan.getCityList();
        // 尽可能保证每个设备每次拉的任务是同一个城市的：采用预分配方案，先从设备关联的城市中取
        List<City> deviceCityList = deviceCityMap.get(deviceId);
        // 初始状态需要分配城市
        if (deviceCityList == null) {
            deviceCityList = new ArrayList<>();
            deviceCityMap.put(deviceId,deviceCityList);
        }
        // 设备数和城市数以及城市下的任务数都是动态的,因此每次拉取任务时都需要判断设备的已有城市列表中是否有新任务
        for (City city : allCityList) {
            putTaskFromCity(city,deviceCityList);
        }
        TaskVO taskVO = deviceCityList.stream()
                .flatMap(x->x.getTaskList().stream())
                .filter(x->!x.getDone())
                .findFirst()
                .map(x->new TaskVO(x.getCityName(),x.getTaskId()))
                .get();
        for(City city:deviceCityList){
            for(Task task:city.getTaskList()){
                if(task.getDone() == null || !task.getDone()){
                    task.setDone(true);
                    taskVO = new TaskVO(task.getCityName(),task.getTaskId());
                    break;
                }
            }
        }

        // 获取任务列表
        for (City city : allCityList) {
            String cityName = city.getCityName();
            List<Task> taskList = city.getTaskList();
            if (taskList.size() > 0) {
                //
                String key = deviceId + today;
                DeviceState deviceState = getDeviceState(key);
                Task task;
                List<String> cityNames;
                List<String> taskIds;
                if (deviceState == null) {
                    deviceState = new DeviceState();
                    deviceState.setDeviceId(deviceId);
                    cityNames = new ArrayList<>();
                    cityNames.add(cityName);
                    taskIds = new ArrayList<>();
                    deviceState.setCityNames(cityNames);
                    deviceState.setTaskIds(taskIds);
                } else {
                    cityNames = deviceState.getCityNames();
                    taskIds = deviceState.getTaskIds();
                    String cachedCity = cityNames.stream().filter(x -> x.equals(cityName)).findFirst().get();
                    if (cachedCity == null) {
                        // 每个设备每天最多切换2个城市
                        if (cityNames.size() < 3) {
                            cityNames.add(cityName);
                        } else {
                            break;
                        }
                    }
                }
                task = taskList.remove(0);
                // 每个设备每天最多4个任务
                if (taskIds.size() < 4) {
                    taskIds.add(task.getTaskId());
                } else {
                    break;
                }

                taskVO = new TaskVO(cityName, task.getTaskId());

            }
        }
        return taskVO;
    }

    /**
     * 设备数和城市数以及城市下的任务数都是动态的,不是固定的,设备可以基于任务数来扩缩容
     * 因此每次拉取任务时都需要判断设备的已有城市列表中是否有新任务
     *
     * @param city
     * @param deviceCityList
     */
    private void putTaskFromCity(City city,List<City> deviceCityList){
        List<Task> taskList = city.getTaskList();
        int cityTaskNum = taskList.size();
        // 每个设备每天最多切换2个城市
        if (cityTaskNum > 0 && deviceCityList.size() <= 3) {
            // 当前设备已分配的任务总数
            int nowTaskNum = deviceCityList.stream().map(x -> x.getTaskList().size()).reduce(0, Integer::sum);
            // 可领取的任务数量
            int newTaskNum = 4 - nowTaskNum;
            if (newTaskNum > 0) {
                String cityName = city.getCityName();
                // 由于设备数和城市数以及城市下的任务数都是动态的，因此每次拉取任务时都需要判断设备的已有城市列表中是否有新任务
                City deviceCity = deviceCityList.stream().filter(x->x.getCityName().equals(cityName)).findFirst().get();
                // 若设备中无此城市，则创建该城市的新实例
                if(deviceCity==null){
                    deviceCity = new City();
                    deviceCity.setCityName(cityName);
                    deviceCity.setTaskList(new ArrayList<>());
                }
                // 每个设备每天最多4个任务
                for (int i = 0; i < newTaskNum; i++) {
                    if (taskList.size() > 0) {
                        // 将任务从源列表移除并分配给设备
                        deviceCity.getTaskList().add(taskList.remove(0));
                    } else {
                        break;
                    }
                }
                deviceCityList.add(deviceCity);
            }
        }
    }

}
