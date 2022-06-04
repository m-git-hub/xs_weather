package com.mf.weather.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@TableName(value = "t_area")
@ColumnWidth(10)
@Document("weather")
public class Area {
    @Id //映射mongodb文档的_id
    private Long id;
    @ColumnWidth(20)
    @ExcelProperty(order = 0,value = "地区编码")
    @Field("areaCode")
    private String areaCode;
    @ColumnWidth(20)
    @Field("areaName")
    @ExcelProperty(order = 1,value = "地区名称")
    private String areaName;
    @ExcelProperty(order = 2,value = "天气")
    @Field("areaWeather")
    private String areaWeather;

    @Override
    public String toString() {
        return "Area{" +
                "id=" + id +
                ", areaCode='" + areaCode + '\'' +
                ", areaName='" + areaName + '\'' +
                ", areaWeather='" + areaWeather + '\'' +
                '}';
    }
}
