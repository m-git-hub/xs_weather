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
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.util.*;


/**
 * 地区接口实现
 */
@Service
public class AreaServiceImpl extends ServiceImpl<AreaMapper, Area> implements AreaService {

    private static final String CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String ENCODING = "utf-8";
    private static final int PAGE_SIZE = 100;
    private static final String URL = "http://www.weather.com.cn/data/sk/";


    @Autowired
    private AreaMapper areaMapper;


    @Autowired
    private MongoTemplate mongoTemplate;

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
                    JSONObject jsonResult = HttpUtil.sendGet(URL + list.get(y).getAreaCode() + ".html");
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


    /**
     * mysql中分页获取
     *
     * @param page
     * @return
     */
    @Override
    public Page getAreaList(Page page) {
        LambdaQueryWrapper wrapper = Wrappers.lambdaQuery();
        return areaMapper.selectPage(page, wrapper);
    }


    /**
     * mongo 批量添加数据
     */
    @Override
    public void insertBatch() {
        LambdaQueryWrapper queryWrapper = Wrappers.lambdaQuery();
        List<Area> list = areaMapper.selectList(queryWrapper);
        for (Area area : list) {
            JSONObject jsonResult = HttpUtil.sendGet(URL + area.getAreaCode() + ".html");
            JSONObject weatherInfo = (JSONObject) jsonResult.get("weatherinfo");
            String temp = (String) weatherInfo.get("temp");
            area.setAreaWeather(temp);
        }
        list.forEach(System.out :: println);
        mongoTemplate.remove(new Query(), Area.class);
        mongoTemplate.insert(list, Area.class);
    }

    /**
     * mongo 中分页获取
     *
     * @param page
     * @param size
     * @return
     */
    @Override
    public Map<String, Object> getAreaPageFromMongo(int page, int size) {
        Query query = new Query();
        long count = mongoTemplate.count(query, Area.class);
        int totalPage = (int) Math.ceil((double) count / (double) size);
        int pageNum = (page*size)-size;
        query.skip(pageNum).limit(size);
        List<Area> areas = mongoTemplate.find(query, Area.class);
        Map<String, Object> map = new HashMap<>();
        map.put("total", count);
        map.put("totalPage", totalPage);
        map.put("currentPage", page);
        map.put("list", areas);
        System.out.println(map.toString());
        return map;
    }


    /**
     * 导出Excel(mongo)
     *
     * @param response
     * @throws IOException
     */
    @Override
    public void exportExcelFromMongo(HttpServletResponse response) throws IOException {
        long beforeTime = System.currentTimeMillis();
        Query query = new Query();
        long count = mongoTemplate.count(query, Area.class);
        int totalPage = (int) Math.ceil((double) count / (double) PAGE_SIZE);
        response.setContentType(CONTENT_TYPE);
        response.setCharacterEncoding(ENCODING);
        String fileName = URLEncoder.encode("output", ENCODING).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        Set<String> exclude = new HashSet<String>();
        exclude.add("id");
        EasyExcel.write(response.getOutputStream(), Area.class).sheet("sheet").excludeColumnFieldNames(exclude).doWrite(() -> {
            List<Area> list = new ArrayList<>();
            for (int i = 1; i <= totalPage; i++) {
                int pageNum = (i*PAGE_SIZE)-PAGE_SIZE;
                query.skip(pageNum).limit(PAGE_SIZE);
                List<Area> areas = mongoTemplate.find(query, Area.class);
                list = areas;
            }
            return list;
        });
        long afterTime = System.currentTimeMillis();
        System.out.println("mongo导出耗时：" + (afterTime - beforeTime));
    }



}
