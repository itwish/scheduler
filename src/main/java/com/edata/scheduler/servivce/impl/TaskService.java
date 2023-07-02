package com.edata.scheduler.servivce.impl;

import com.edata.scheduler.cache.DeviceCityCache;
import com.edata.scheduler.model.City;
import com.edata.scheduler.model.CityTaskPool;
import com.edata.scheduler.model.Task;
import com.edata.scheduler.servivce.ITaskService;
import com.edata.scheduler.vo.TaskVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
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
     * 最多可拉取任务数
     */
    @Value("${device.task.maxNum}")
    private int maxTaskNum;
    /**
     * 最多可关联城市数
     */
    @Value("${device.city.maxNum}")
    private int maxCityNum;
    /**
     * 设备最多可空闲天数，用于查找设备关联城市的历史记录
     */
    @Value("${device.maxIdleDays}")
    private int maxIdleDays;

    /**
     * 采用实时分配方案（将设备与城市及其指定数量的任务关联），每次拉取任务后，缓存任务，返回任务
     * <p>
     * 尽可能保证每个设备每次拉的任务是同一个城市的
     * 每个设备每天最多4个任务
     * 每个设备每天最多切换2个城市
     * 设备和城市要有对应关系 即一个设备今天是A城市 明天也是A城市 （第二天有A城市的任务的话）
     * 设备数和城市数以及城市下的任务数都是动态的,不是固定的,设备可以基于任务数来扩缩容
     *
     * @param deviceId 设备Id
     * @return
     */
    @Override
    public TaskVO getTask(String deviceId) {
        // 获取当天所有城市任务列表
        List<City> allCityList = CityTaskPool.getTodayCityTask();
        // 获取当天设备已分配的城市任务列表
        List<City> deviceCityList = getTodayAssinedCityList(deviceId);
        // 根据昨天设备已分配的城市拉取今天的任务
        TaskVO taskVO = getTaskFromYesterdayCity(deviceId, allCityList, deviceCityList);
        // 拉取当天新任务
        taskVO = taskVO == null ? getTaskFromCityList(deviceCityList, allCityList) : taskVO;
        return Optional.ofNullable(taskVO).orElse(new TaskVO("", ""));
    }

    /**
     * 获取今天已分配的城市列表
     *
     * @return
     */
    private List<City> getTodayAssinedCityList(String deviceId) {
        String nowDeviceId = deviceId + LocalDate.now();
        List<City> deviceCityList = DeviceCityCache.getDeviceCityByDeviceId(nowDeviceId);
        // 初始状态需要分配城市
        if (deviceCityList == null) {
            deviceCityList = new ArrayList<>();
            DeviceCityCache.putDeviceCity(nowDeviceId, deviceCityList);
        }
        return deviceCityList;
    }

    /**
     * 优先从昨天关联的城市中拉取任务，即：
     * 设备和城市要有对应关系 即一个设备今天是A城市 明天也是A城市 （第二天有A城市的任务的话）
     *
     * @param deviceId
     * @param allCityList
     * @param deviceCityList
     */
    private TaskVO getTaskFromYesterdayCity(String deviceId, List<City> allCityList, List<City> deviceCityList) {
        String lastDeviceId;
        List<City> lastDeviceCityList = null;
        // 处理场景：假设设备1第一天城市A 第二天由于各种原因没有跑（假设随机休息一天） 第三天过来也得优先城市A
        int n = 1;
        while (n <= maxIdleDays && lastDeviceCityList == null) {
            lastDeviceId = deviceId + LocalDate.now().minusDays(n++);
            lastDeviceCityList = DeviceCityCache.getDeviceCityByDeviceId(lastDeviceId);
        }
        TaskVO taskVO = lastDeviceCityList == null ? null : getTaskFromSameCities(lastDeviceCityList, deviceCityList, allCityList);
        return taskVO;
    }

    /**
     * 注意：设备数和城市数以及城市下的任务数都是动态的,不是固定的,设备可以基于任务数来扩缩容
     * 因此每次拉取任务时都需要判断设备的已有城市列表中是否有新任务
     *
     * @param deviceCityList 设备关联的城市列表
     * @param allCityList    源城市列表
     */
    private TaskVO getTaskFromCityList(List<City> deviceCityList, List<City> allCityList) {
        TaskVO taskVO = getTaskFromSameCities(null, deviceCityList, allCityList);
        // 从新的城市中获取任务
        if (taskVO == null) {
            taskVO = allCityList.stream()
                    .map(city -> getTaskFromCity(deviceCityList, city))
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
        }
        return taskVO;
    }

    /**
     * 尽可能保证每个设备每次拉的任务是同一个城市的
     *
     * @param lastDeviceCityList 昨天或上一次设备分配的城市列表
     * @param deviceCityList 当天设备分配的城市列表
     * @param allCityList 总的城市列表
     * @return
     */
    private TaskVO getTaskFromSameCities(List<City> lastDeviceCityList, List<City> deviceCityList, List<City> allCityList) {
        Map<String, City> allCityMap = allCityList.stream().collect(Collectors.toMap(City::getCityName, City -> City));
        if (lastDeviceCityList == null) {
            lastDeviceCityList = deviceCityList;
        }
        return lastDeviceCityList.stream()
                .map(d -> allCityMap.get(d.getCityName()))
                .filter(Objects::nonNull)
                .map(c -> getTaskFromCity(deviceCityList, c))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    /**
     * 从城市中获取任务，并记录到缓存中
     *
     * @param deviceCityList
     * @param city
     * @return
     */
    private synchronized TaskVO getTaskFromCity(List<City> deviceCityList, City city) {
        List<Task> taskList = city.getTaskList();
        int cityTaskNum = taskList.size();
        TaskVO taskVO = null;
        // 每个设备每天最多切换2个城市
        if (cityTaskNum > 0 && deviceCityList.size() <= maxCityNum) {
            // 当前设备已分配的任务总数
            int nowTaskNum = deviceCityList.stream().map(x -> x.getTaskList().size()).reduce(0, Integer::sum);
            // 每个设备每天最多4个任务(可领取的任务数量)
            int newTaskNum = maxTaskNum - nowTaskNum;
            if (newTaskNum > 0) {
                String cityName = city.getCityName();
                // 由于设备数和城市数以及城市下的任务数都是动态的，因此每次拉取任务时都需要判断设备的已有城市列表中是否有新任务
                City deviceCity = deviceCityList.stream()
                        .filter(x -> x.getCityName().equals(cityName))
                        .findFirst()
                        .orElse(null);
                // 若设备中无此城市，则创建该城市的新实例,否则直接更新
                if (deviceCity == null) {
                    deviceCity = new City();
                    deviceCity.setCityName(cityName);
                    deviceCity.setTaskList(new ArrayList<>());
                    deviceCityList.add(deviceCity);
                }
                // 每个设备每天最多4个任务
                if (taskList.size() > 0) {
                    // 将任务从源列表移除并分配(更新)给设备
                    Task task = taskList.remove(0);
                    deviceCity.getTaskList().add(task);
                    taskVO = new TaskVO(task.getCityName(), task.getTaskId());
                }
            }
        }
        return taskVO;
    }

}
