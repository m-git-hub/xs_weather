package com.mf.weather.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mf.weather.entity.Area;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface AreaMapper extends BaseMapper<Area> {

   void updateBatch(List<Area> areas);

}
