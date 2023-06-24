package com.edata.scheduler.cache;

import com.edata.scheduler.model.DeviceState;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 设备任务状态缓存
 * @author yugt 2023/6/23
 */
public class DeviceStateCache {
    private static Map<String, DeviceState> deviceStateMap = new ConcurrentHashMap<>();

    public static void addDeviceState(String key,DeviceState deviceState){
        deviceStateMap.put(key, deviceState);
    }

    public static DeviceState getDeviceState(String key){
        return deviceStateMap.get(key);
    }

    public static void delDeviceState(String key){
        deviceStateMap.remove(key);
    }

}
