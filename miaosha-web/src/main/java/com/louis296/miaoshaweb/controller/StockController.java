package com.louis296.miaoshaweb.controller;

import com.louis296.miaoshaservice.service.StockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class StockController {

    public static final Logger LOGGER=LoggerFactory.getLogger(StockController.class);

    @Autowired
    private StockService stockService;

    @RequestMapping("/getStockCountByCache/{sid}")
    @ResponseBody
    public String getStockByCache(@PathVariable int sid){
        Integer count;
        try{
            count=stockService.getStockCount(sid);
        }catch (Exception e){
            LOGGER.error("查询库存失败：[{}]",e.getMessage());
            return "查询库存失败";
        }
        LOGGER.info("商品id:[{}] 剩余库存为:[{}]",sid,count);
        return String.format("商品id:%d 剩余库存为:%d",sid,count);
    }

    @RequestMapping("/getStockCountByDB/{sid}")
    @ResponseBody
    public String getStockByDB(@PathVariable int sid){
        Integer count;
        try{
            count=stockService.getStockCountByDB(sid);
        }catch (Exception e){
            LOGGER.error("查询库存失败：[{}]",e.getMessage());
            return "查询库存失败";
        }
        LOGGER.info("商品id:[{}] 剩余库存为:[{}]",sid,count);
        return String.format("商品id:%d 剩余库存为:%d",sid,count);
    }
}
