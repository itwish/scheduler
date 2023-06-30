package com.edata.scheduler.listener;

import com.edata.scheduler.model.City;
import com.edata.scheduler.model.CityTaskPool;
import com.edata.scheduler.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 模拟生成当天任务列表
 *
 * Created by yugt on 2019/2/13.
 */
@WebListener
public class InitCityTaskPool implements ServletContextListener{
    private static final Logger logger = LoggerFactory.getLogger(InitCityTaskPool.class);

    /**
     * 测试任务
     * @param sce
     */
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // 测试数据 2023-06-17,杭州,4
        String cityTaskData = """
                2023-06-17,重庆,3
                2023-06-17,广州,1
                2023-06-17,杭州,4
            """;
        List<City> cityList = new ArrayList<>();
        cityTaskData.lines().forEach(s->{
            String[] fields = s.split(",");
            City city = new City();
            city.setCityName(fields[1]);
            List<Task> taskList = new ArrayList();
            for(int i=0;i<Integer.parseInt(fields[2]);i++){
                String taskId = city.getCityName()+i;
                Task task = new Task();
                task.setCityName(fields[1]);
                task.setTaskId(taskId);
                taskList.add(task);
            }
            city.setTaskList(taskList);
            cityList.add(city);
        });
        LocalDate today = LocalDate.now();
        CityTaskPool.addCityTask(today, cityList);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        logger.info("Shutdown and clear CityTaskPool.");
    }
}
