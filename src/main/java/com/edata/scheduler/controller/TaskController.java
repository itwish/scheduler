package com.edata.scheduler.controller;

import com.edata.scheduler.servivce.ITaskService;
import com.edata.scheduler.vo.TaskVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 任务拉取服务
 * @author yugt 2023/6/23
 */
@Controller
public class TaskController {
    @Autowired
    private ITaskService taskService;

    /**
     * 任务拉取接口
     *
     * @param deviceId 设备Id
     * @return
     */
    @RequestMapping("/fetchTask")
    public TaskVO fetchTask(@RequestParam String deviceId){
        return taskService.getTask(deviceId);
    }

}
