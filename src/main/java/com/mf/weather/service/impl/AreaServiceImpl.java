package com.mf.weather.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.read.metadata.ReadSheet;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mf.weather.dao.AreaMapper;
import com.mf.weather.dto.AreaDo;
import com.mf.weather.entity.Area;
import com.mf.weather.listener.ImportExcelListener;
import com.mf.weather.service.AreaService;
import com.mf.weather.utils.HttpUtil;
import org.apache.poi.ss.formula.functions.T;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * 地区接口实现
 */
@Service
public class AreaServiceImpl extends ServiceImpl<AreaMapper, Area> implements AreaService {

    private static final String CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String ENCODING = "utf-8";
    private static final int PAGE_SIZE = 10;
    private static final String URL = "http://www.weather.com.cn/data/sk/";


    @Autowired
    private AreaMapper areaMapper;

    /**
     * 导入Excel
     *
     * @param file
     * @throws IOException
     */
    @Override
    public void importExcel(MultipartFile file) throws IOException {
        long beforeTime = System.currentTimeMillis();
        if (!file.isEmpty()) {
            ExcelReader excelReader = EasyExcel.read(file.getInputStream(), Area.class, new ImportExcelListener()).build();
            ReadSheet sheet = EasyExcel.readSheet().build();
            excelReader.read(sheet);
        }
        long afterTime = System.currentTimeMillis();
        System.out.println("导入耗时：" + (afterTime - beforeTime));
    }


    /**
     * 导出Excel
     *
     * @param response
     * @throws IOException
     */
    @Override
    public void exportExcel(HttpServletResponse response) throws IOException {
        long beforeTime = System.currentTimeMillis();
        LambdaQueryWrapper wrapper = Wrappers.lambdaQuery();
        Integer number = Math.toIntExact(areaMapper.selectCount(wrapper));
        int pageNumber = (int) Math.ceil((double) number / (double) PAGE_SIZE);
        response.setContentType(CONTENT_TYPE);
        response.setCharacterEncoding(ENCODING);
        String fileName = URLEncoder.encode("output", ENCODING).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        Set<String> exclude = new HashSet<String>();
        exclude.add("id");
        EasyExcel.write(response.getOutputStream(), Area.class).sheet("sheet").excludeColumnFieldNames(exclude).doWrite(() -> {
            List<Area> list = new ArrayList<>();
            for (int i = 1; i <= pageNumber; i++) {
                Page page = new Page(i, PAGE_SIZE);
                IPage areaPage = areaMapper.selectPage(page, wrapper);
                list.addAll(areaPage.getRecords());
                // 获取天气
                for (int y = 0; y < list.size(); y++) {
                    JSONObject jsonResult = HttpUtil.sendGet(URL + list.get(y).getAreaCode()+ ".html");
                    JSONObject weatherInfo = (JSONObject) jsonResult.get("weatherinfo");
                    String temp = (String) weatherInfo.get("temp");
                    list.get(y).setAreaWeather(temp);
                }
//                System.out.println("第" + i + "批");
            }
            return list;
        });
        long afterTime = System.currentTimeMillis();
        System.out.println("导出耗时：" + (afterTime - beforeTime));
    }

    @Override
    public Page getAreaList(Page page) {
        LambdaQueryWrapper wrapper = Wrappers.lambdaQuery();
        return areaMapper.selectPage(page, wrapper);
    }


}
