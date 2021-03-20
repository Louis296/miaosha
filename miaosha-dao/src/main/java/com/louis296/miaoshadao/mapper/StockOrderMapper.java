package com.louis296.miaoshadao.mapper;

import com.louis296.miaoshadao.dao.StockOrder;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StockOrderMapper {
    int insertSelective(StockOrder order);
}
