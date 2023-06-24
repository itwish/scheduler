package com.edata.scheduler.model;

import lombok.Data;

import java.util.List;

/**
 * 设备执行任务状态
 * @author yugt 2023/6/23
 */
@Data
public class DeviceState {
    private String deviceId;
    private List<String> cityNames;
    private List<String> taskIds;
}
