package com.edata.scheduler.model;

import lombok.Data;

/**
 * @author yugt 2023/6/23
 */
@Data
public class Task {
    private String taskId;

    private String shop;
    private String shopUrl;

    private String cityName;
    private String platform;

    /** 是否已执行 */
    private Boolean done;
}
