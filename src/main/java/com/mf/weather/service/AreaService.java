package com.mf.weather.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mf.weather.Vo.Result;
import com.mf.weather.entity.Area;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

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


    Page getAreaList(Page page);

}
