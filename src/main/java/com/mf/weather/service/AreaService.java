package com.mf.weather.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mf.weather.entity.Area;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * 地区接口
 */
public interface AreaService extends IService<Area> {
    /**
     * 导入Excel
     *
     * @param file
     */
    void importExcel(MultipartFile file) throws IOException;

    /**
     * 导出excel
     *
     * @param response
     */
    void exportExcel(HttpServletResponse response) throws IOException;

    void exportExcelFromMongo(HttpServletResponse response) throws IOException;

    void exportExcelFromMongoSheet(HttpServletResponse response) throws IOException;

    void exportExceltest(HttpServletResponse response, HttpServletRequest request) throws IOException;




    Page getAreaList(Page page);


    /**
     * mongo
     * 批量添加文档数据
     */
    void insertBatch();

    /**
     * mogo
     * 分页获取数据
     * @param page
     * @param size
     * @return
     */
    Map<String, Object> getAreaPageFromMongo(int page, int size);

}
