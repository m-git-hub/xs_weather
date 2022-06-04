package com.mf.weather.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mf.weather.Vo.Result;
import com.mf.weather.entity.Area;
import com.mf.weather.service.AreaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
public class ExcelController {

    @Autowired
    private AreaService areaService;

    /**
     * 导入Excel
     *
     * @param file
     * @return
     * @throws IOException
     */
    @PostMapping("/import")
    @ResponseBody
    public Result importArea(@RequestParam("file") MultipartFile file) throws IOException {
        areaService.importExcel(file);
        return Result.success();
    }


    /**
     * 导出Excel
     */
    @GetMapping("/exportArea")
    public void exportArea(HttpServletResponse response) throws IOException {
        areaService.exportExcel(response);
//        return Result.success();
    }

    /**
     * 获取数据列表
     */
    @PostMapping ("/getAreaList")
    public Result getAreaList(@RequestParam(required = true, defaultValue = "1") Integer pageNumber,
                              @RequestParam(required = true, defaultValue = "1") Integer size) {
        Page<Area> page = new Page<>(pageNumber, size);
        Page areaList = areaService.getAreaList(page);
        return Result.success(areaList);
    }


}


