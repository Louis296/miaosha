package com.louis296.miaoshadao.mapper;

import com.louis296.miaoshadao.dao.Stock;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StockMapper {

    Stock selectByPrimaryKey(int id);
    Stock selectByPrimaryKeyForUpdate(int id);
    int updateByOptimistic(Stock stock);
    int updateByPrimaryKeySelective(Stock stock);

}
