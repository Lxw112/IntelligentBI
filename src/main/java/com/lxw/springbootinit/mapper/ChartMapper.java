package com.lxw.springbootinit.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lxw.springbootinit.model.entity.Chart;

import java.util.List;
import java.util.Map;


/**
 * @Entity com.yupi.springbootinit.model.entity.Chart
 */
public interface ChartMapper extends BaseMapper<Chart> {
    List<Map<String,Object>> queryChartData(String querySql);

}




