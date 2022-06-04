package com.mf.weather.listener;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.alibaba.excel.util.ListUtils;
import com.mf.weather.entity.Area;
import com.mf.weather.service.AreaService;
import com.mf.weather.service.impl.AreaServiceImpl;

import java.util.List;

public class ImportExcelListener implements ReadListener<Area> {
    private static final int BATCH_COUNT = 10;
    private List<Area> cachedDataList = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);

    private AreaService areaService;
    public ImportExcelListener(){
        this.areaService = new AreaServiceImpl();
    }
    public ImportExcelListener(AreaService areaService){
        this.areaService = areaService;
    }

    /**
     * 每一个数据解析都会调用
     * @param area
     * @param analysisContext
     */
    @Override
    public void invoke(Area area, AnalysisContext analysisContext) {
        System.out.println("读取到数据:"+area);
        cachedDataList.add(area);
        if (cachedDataList.size() >= BATCH_COUNT) {
            batchInsert();
            cachedDataList = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);
        }
    }

    /**
     * 所有数据解析完成都会来调用
     * @param analysisContext
     */
    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
        batchInsert();
        System.out.println("所有数据解析完成");
    }

    /**
     * 批量存储
     */
    public void batchInsert() {
        areaService.saveBatch(cachedDataList);
        System.out.println("存储数据库成功!");
    }
}
