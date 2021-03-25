package com.louis296.miaoshaservice.service;

import com.louis296.miaoshadao.dao.Stock;

public interface StockService {
    Stock getStockById(int id);
    Stock getStockByIdForUpdate(int id);
    int updateStockById(Stock stock);

    int updateStockByOptimistic(Stock stock);
    Integer getStockCount(int sid);
    int getStockCountByDB(int id);
    Integer getStockCountByCache(int id);
    void setStockCountCache(int id,int count);
    void delStockCountCache(int id);

}
