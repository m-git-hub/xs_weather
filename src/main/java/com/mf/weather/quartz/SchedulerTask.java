package com.mf.weather.quartz;

import com.mf.weather.service.AreaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Component
public class SchedulerTask {

    private static final Logger logger = LoggerFactory.getLogger(SchedulerTask.class);

    @Autowired
    private AreaService areaService;

    @Scheduled(fixedRate = 300000)
    public void startQuartz(){
        logger.info("定时任务执行了",System.currentTimeMillis());
        areaService.insertBatch();
    }
}
