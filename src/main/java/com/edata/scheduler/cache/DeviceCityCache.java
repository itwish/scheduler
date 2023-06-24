package com.edata.scheduler.cache;

import com.edata.scheduler.model.City;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 设备-城市缓存
 * @author yugt 2023/6/23
 */
public class DeviceCityCache {
    private static Map<String, List<City>> deviceCityMap = new ConcurrentHashMap<>();

    /**
     * 添加设备城市
     * @param key deviceId+date
     * @param deviceCity
     */
    public static void putDeviceCity(String key,List<City> deviceCity){
        deviceCityMap.put(key, deviceCity);
    }

    /**
     * 获取设备城市
     * @param key deviceId+date
     * @return
     */
    public static List<City> getDeviceCityByDeviceId(String key){
        return deviceCityMap.get(key);
    }

    public static void delDeviceCity(String key){
        deviceCityMap.remove(key);
    }

}
