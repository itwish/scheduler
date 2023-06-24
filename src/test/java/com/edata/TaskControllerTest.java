package com.edata;

import com.edata.scheduler.SchedulerApplication;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;

/**
 * @author yugt 2020/7/27
 */

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = SchedulerApplication.class)
public class TaskControllerTest {
    @Autowired
    private TestRestTemplate testRestTemplate;

    /**
     * 测试拉取任务
     */
    @Test
    public void testFetchTask() {

        String ret = testRestTemplate.postForObject("/price/min", null, String.class);

        Assertions.assertEquals(null, Double.parseDouble(ret));

    }

}
