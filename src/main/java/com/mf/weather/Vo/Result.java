package com.mf.weather.Vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;

@Data
public class Result<T> implements Serializable {

    private int code;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String msg;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String request;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T data;


    //构造函数
    private Result(int code,String msg){
        this.code = code;
        this.msg = msg;
    }
    private Result(T data){
        this.data = data;
    }

    public static <T> Result<T> success() {
        return new Result<T>(200,"操作成功");
    }
    public static <T> Result<T> success(T data) {
        return new Result<T>(data);
    }

}
