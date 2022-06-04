package com.mf.weather.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

@Data
@ColumnWidth(10)
public class AreaDo {
    private long id;
    @ColumnWidth(20)
    @ExcelProperty(order = 0,value = "地区编码")
    private String areaCode;
    @ColumnWidth(20)
    @ExcelProperty(order = 1,value = "地区名称")
    private String areaName;
    @ExcelProperty(order = 2,value = "天气")
    private String areaWeather;
}
