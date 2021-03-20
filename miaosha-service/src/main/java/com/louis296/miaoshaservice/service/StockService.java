package com.louis296.miaoshaservice.service;

import com.louis296.miaoshadao.dao.Stock;

public interface StockService {
    Stock getStockById(int id);
    int updateStockById(Stock stock);

    int updateStockByOptimistic(Stock stock);
}
