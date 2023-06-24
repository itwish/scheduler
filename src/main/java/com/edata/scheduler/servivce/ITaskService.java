package com.edata.scheduler.servivce;

import com.edata.scheduler.vo.TaskVO;

/**
 * @author yugt 2023/6/23
 */
public interface ITaskService {
    TaskVO getTask(String deviceId);
}
