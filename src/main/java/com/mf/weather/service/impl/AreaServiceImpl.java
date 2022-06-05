package com.mf.weather.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.read.metadata.ReadSheet;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import com.alibaba.excel.write.metadata.style.WriteFont;
import com.alibaba.excel.write.style.HorizontalCellStyleStrategy;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mf.weather.dao.AreaMapper;
import com.mf.weather.entity.Area;
import com.mf.weather.listener.ImportExcelListener;
import com.mf.weather.service.AreaService;
import com.mf.weather.utils.HttpUtil;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
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
        list.forEach(System.out::println);
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
        int pageNum = (page * size) - size;
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
                int pageNum = (i * PAGE_SIZE) - PAGE_SIZE;
                query.skip(pageNum).limit(PAGE_SIZE);
                List<Area> areas = mongoTemplate.find(query, Area.class);
                list = areas;
            }
            return list;
        });
        long afterTime = System.currentTimeMillis();
        System.out.println("mongo导出耗时：" + (afterTime - beforeTime));
    }


    /**
     * 多sheet导出Excel(mongo)
     *
     * @param response
     * @throws IOException
     */
    @Override
    public void exportExcelFromMongoSheet(HttpServletResponse response) throws IOException {
        long beforeTime = System.currentTimeMillis();
        Query query = new Query();
        long count = mongoTemplate.count(query, Area.class);
        int totalPage = (int) Math.ceil((double) count / (double) 10);
        response.setContentType(CONTENT_TYPE);
        response.setCharacterEncoding(ENCODING);
        String fileName = URLEncoder.encode("output", ENCODING).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        Set<String> exclude = new HashSet<String>();
        exclude.add("id");
        ExcelWriter excelWriter = EasyExcel.write(response.getOutputStream()).build();
        WriteSheet writeSheet;
        for (int i = 1; i <= totalPage; i++) {
            int pageNum = (i * 10) - 10;
            query.skip(pageNum).limit(10);
            List<Area> areas = mongoTemplate.find(query, Area.class);
            writeSheet = EasyExcel.writerSheet(i - 1, "page" + i).head(Area.class).build();
            excelWriter.write(areas, writeSheet);

        }
        excelWriter.finish();
        long afterTime = System.currentTimeMillis();
        System.out.println("mongo导出耗时：" + (afterTime - beforeTime));
    }

    /**
     * 测试分片下载
     * @param response
     * @param request
     * @throws IOException
     */
    @Override
    public void exportExceltest(HttpServletResponse response, HttpServletRequest request) throws IOException {

        Query query = new Query();
        List<Area> areas = mongoTemplate.find(query, Area.class);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();//字节流
        EasyExcel.write(bos, Area.class).sheet("sheet").doWrite(areas);


        byte[] bytes = bos.toByteArray();

        String filepath = "output.xlsx";
        File file = new File(filepath);
        if (file.exists()) {
            file.delete();
        }
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(bytes, 0, bytes.length);
        fos.flush();
        fos.close();


        File file2 = new File(filepath);
        response.setCharacterEncoding("utf-8");
        InputStream is = null;
        OutputStream os = null;

        try {

            long fSize = file2.length();
            response.setContentType("application/x-download");
            String fileName = URLEncoder.encode(file.getName(),"utf-8");
            response.addHeader("Content-Disposition", "attachment;filename=output.xlsx");
            response.setHeader("Accept-Range", "bytes");

            response.setHeader("fSize",String.valueOf(fSize));
            response.setHeader("fName",fileName);

            long pos = 0,last = fSize-1,sum = 0;
            if(null != request.getHeader("Range")){
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);

                String numRange = request.getHeader("Range").replaceAll("bytes=", "");
                String[] starRange  = numRange.split("-");
                if(starRange.length == 2){
                    pos = Long.parseLong(starRange[0].trim());
                    last = Long.parseLong(starRange[1].trim());
                    if(last > fSize-1){
                        last = fSize-1;
                    }
                }else {
                    pos = Long.parseLong(numRange.replaceAll("-","").trim());
                }
            }

            long rangeLength = last - pos +1;
            String contentRange = new StringBuffer("byte").append(pos).append("-").append(last).append("/").append(fSize).toString();
            response.setHeader("Content-Range",contentRange);
            response.setHeader("Content-length", String.valueOf(rangeLength));


            os = new BufferedOutputStream(response.getOutputStream());
            is = new BufferedInputStream(new FileInputStream(file2));
            is.skip(pos);
            byte[] buffer = new byte[1024];
            int length = 0;
            while (sum < rangeLength){
                length = is.read(buffer,0, (rangeLength-sum)<= buffer.length? (int) (rangeLength - sum) : buffer.length);
                sum = sum + length;
                os.write(buffer,0,length);
            }
            System.out.println("下载完成");
        } finally {
            if (is != null) {
                is.close();
            }
            if (os != null) {
                os.close();
            }
        }


    }


}
