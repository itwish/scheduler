package com.edata.scheduler.servivce.impl;

import com.edata.scheduler.cache.DeviceCityCache;
import com.edata.scheduler.model.City;
import com.edata.scheduler.model.CityTaskPool;
import com.edata.scheduler.model.Task;
import com.edata.scheduler.servivce.ITaskService;
import com.edata.scheduler.vo.TaskVO;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 任务拉取服务
 * 1.给设备分配城市任务，优先选取昨天已分配的城市
 * 2.更新设备-城市列表中任务
 * 3.从设备-城市列表中获取任务
 *
 * @author yugt 2023/6/23
 */
@Service
public class TaskService implements ITaskService {
    /**
     * 采用预分配方案，将设备与城市及其指定数量的任务关联(分配)，然后从设备管理的任务集合中拉取
     *
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
        // 获取当天城市任务列表
        LocalDate today = LocalDate.now();
        List<City> allCityList = CityTaskPool.getCityTaskByDate(today);
        // 尽可能保证每个设备每次拉的任务是同一个城市的：采用预分配方案，先从设备关联的城市中取
        String nowDeviceId = deviceId + today;
        List<City> deviceCityList = DeviceCityCache.getDeviceCityByDeviceId(nowDeviceId);
        // 初始状态需要分配城市
        if (deviceCityList == null) {
            deviceCityList = new ArrayList<>();
            DeviceCityCache.putDeviceCity(nowDeviceId,deviceCityList);
        }
        // 设备和城市要有对应关系 即一个设备今天是A城市 明天也是A城市 （第二天有A城市的任务的话）
        putTaskFromLastCity(deviceId,allCityList,deviceCityList);
        // 设备数和城市数以及城市下的任务数都是动态的,因此可能会有更新处理
        for (City city : allCityList) {
            putTaskFromCity(city,deviceCityList);
        }
        // 从设备已分配的城市列表中获取任务
        return getTaskFromAssignedCity(deviceCityList);
    }

    /**
     * 从已分配的城市列表中获取任务
     *
     * @param deviceCityList 设备已分配的城市列表
     * @return
     */
    private TaskVO getTaskFromAssignedCity(List<City> deviceCityList){
        return deviceCityList.stream()
                .flatMap(x->x.getTaskList().stream())
                .filter(x->!x.getDone())
                .findFirst()
                .map(x-> {x.setDone(true); return new TaskVO(x.getCityName(),x.getTaskId());})
                .orElseGet(()->new TaskVO("",""));
    }

    /**
     * 优先从昨天关联的城市中拉取任务，即：
     * 设备和城市要有对应关系 即一个设备今天是A城市 明天也是A城市 （第二天有A城市的任务的话）
     * @param deviceId
     * @param allCityList
     * @param deviceCityList
     */
    private void putTaskFromLastCity(String deviceId,List<City> allCityList,List<City> deviceCityList) {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        String lastDeviceId = deviceId + yesterday;
        List<City> lastDeviceCityList = DeviceCityCache.getDeviceCityByDeviceId(lastDeviceId);
        Map<String,City> allCityMap = allCityList.stream().collect(Collectors.toMap(City::getCityName,City->City));
        if(lastDeviceCityList!=null){
            for(City city : lastDeviceCityList){
                City lastCity = allCityMap.get(city.getCityName());
                if(lastCity!=null){
                    putTaskFromCity(lastCity,deviceCityList);
                }
            }
        }
    }

    /**
     * 设备数和城市数以及城市下的任务数都是动态的,不是固定的,设备可以基于任务数来扩缩容
     * 因此每次拉取任务时都需要判断设备的已有城市列表中是否有新任务
     *
     * @param city
     * @param deviceCityList
     */
    private synchronized void putTaskFromCity(City city,List<City> deviceCityList){
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
                City deviceCity = deviceCityList.stream()
                        .filter(x->x.getCityName().equals(cityName))
                        .findFirst()
                        .orElse(null);
                // 若设备中无此城市，则创建该城市的新实例,否则直接更新
                if(deviceCity==null){
                    deviceCity = new City();
                    deviceCity.setCityName(cityName);
                    deviceCity.setTaskList(new ArrayList<>());
                }
                // 每个设备每天最多4个任务
                for (int i = 0; i < newTaskNum; i++) {
                    if (taskList.size() > 0) {
                        // 将任务从源列表移除并分配(更新)给设备
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
