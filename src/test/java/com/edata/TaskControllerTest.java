package com.edata;

import com.edata.scheduler.SchedulerApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * 测试拉取任务
 *
 * @author yugt 2020/7/27
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = SchedulerApplication.class)
public class TaskControllerTest {
    @Autowired
    private TestRestTemplate testRestTemplate;

    /**
     * 模拟：2个设备执行3个城市的任务
     *
     * n个设备 去完成m个城市的任务
     * 尽可能保证每个设备每次拉的任务是同一个城市的
     * 每个设备每天最多4个任务
     * 每个设备每天最多切换2个城市
     * 处理设备和城市的对应关系 即一个设备今天是A城市 明天也是A城市 （第二天有A城市的任务的话）
     * 设备数 和 城市数以及城市下的任务数都是动态的 不是固定的
     */
    @Test
    public void testFetchTask() {
        List<String> devices = IntStream.range(0, 2).mapToObj(i -> "dev" + i).toList();;
        String taskData = """
                    2023-06-17,重庆,3
                    2023-06-17,广州,1
                    2023-06-17,杭州,4
                """;
        // 线程池模拟并发拉取任务
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.initialize();

        for (int i = 0; i < devices.size(); i++) {
            final int m = i;
            executor.execute(() -> {
                int j = 0;
                while (j++ <= 4) {
                    // 若要并发执行，请将synchronized注释掉，这里只是为了方便演示
                    synchronized (this.getClass()){
                        System.out.println(devices.get(m)+"拉取任务:");
                        String ret = testRestTemplate.getForObject("/fetchTask?deviceId="+devices.get(m), String.class);
                        System.out.println(ret);
                    }
                    //Assertions.assertEquals(ret, ret);
                }
            });
        }
        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
