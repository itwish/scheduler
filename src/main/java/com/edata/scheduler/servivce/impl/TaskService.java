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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
        // 获取当天城市任务列表
        LocalDate today = LocalDate.now();
        List<City> allCityList = CityTaskPool.getCityTaskByDate(today);
        // 尽可能保证每个设备每次拉的任务是同一个城市的
        String nowDeviceId = deviceId + today;
        List<City> deviceCityList = DeviceCityCache.getDeviceCityByDeviceId(nowDeviceId);
        // 初始状态需要分配城市
        if (deviceCityList == null) {
            deviceCityList = new ArrayList<>();
            DeviceCityCache.putDeviceCity(nowDeviceId, deviceCityList);
        }
        // 设备和城市要有对应关系 即一个设备今天是A城市 明天也是A城市 （第二天有A城市的任务的话）
        TaskVO taskVO = getTaskFromYesterdayCity(deviceId, allCityList, deviceCityList);
        // 设备数和城市数以及城市下的任务数都是动态的,因此可能会有更新处理
        taskVO = taskVO == null ? getTaskFromCityList(deviceCityList, allCityList) : taskVO;
        return Optional.ofNullable(taskVO).orElse(new TaskVO("", ""));
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
        LocalDate yesterday = LocalDate.now().minusDays(1);
        String lastDeviceId = deviceId + yesterday;
        List<City> lastDeviceCityList = DeviceCityCache.getDeviceCityByDeviceId(lastDeviceId);
        // 处理场景：假设设备1第一天城市A 第二天由于各种原因没有跑（假设随机休息一天） 第三天过来也得优先城市A
        int n = 1;
        while (n <= maxIdleDays && lastDeviceCityList == null) {
            lastDeviceId = deviceId + LocalDate.now().minusDays(++n);
            lastDeviceCityList = DeviceCityCache.getDeviceCityByDeviceId(lastDeviceId);
        }
        Map<String, City> allCityMap = allCityList.stream().collect(Collectors.toMap(City::getCityName, City -> City));
        TaskVO taskVO = null;
        if (lastDeviceCityList != null) {
            for (City city : lastDeviceCityList) {
                City lastCity = allCityMap.get(city.getCityName());
                if (lastCity != null) {
                    taskVO = getTaskFromCity(deviceCityList, lastCity);
                }
                if (taskVO != null) {
                    break;
                }
            }
        }
        return taskVO;
    }

    /**
     * 注意：设备数和城市数以及城市下的任务数都是动态的,不是固定的,设备可以基于任务数来扩缩容
     * 因此每次拉取任务时都需要判断设备的已有城市列表中是否有新任务
     *
     * @param deviceCityList 设备关联的城市列表
     * @param sourceCityList 源城市列表
     */
    private TaskVO getTaskFromCityList(List<City> deviceCityList, List<City> sourceCityList) {
        Map<String, City> allCityMap = sourceCityList.stream().collect(Collectors.toMap(City::getCityName, City -> City));
        TaskVO taskVO = null;
        // 尽可能保证每个设备每次拉的任务是同一个城市的
        for (City deviceCity : deviceCityList) {
            City city = allCityMap.get(deviceCity.getCityName());
            if (city != null) {
                taskVO = getTaskFromCity(deviceCityList, city);
                if (taskVO != null) {
                    break;
                }
            }
        }
        // 从新的城市中获取任务
        if (taskVO == null) {
            for (City city : sourceCityList) {
                taskVO = getTaskFromCity(deviceCityList, city);
                if (taskVO != null) {
                    break;
                }
            }
        }
        return taskVO;
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
