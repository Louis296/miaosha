package com.louis296.miaoshaweb.controller;

import com.google.common.util.concurrent.RateLimiter;
import com.louis296.miaoshaservice.service.OrderService;
import com.louis296.miaoshaservice.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

@Controller
public class OrderController {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderController.class);

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserService userService;

    RateLimiter rateLimiter=RateLimiter.create(10);

    @RequestMapping("/createWrongOrder/{sid}")
    @ResponseBody
    public String createWrongOrder(@PathVariable int sid) {
        LOGGER.info("购买物品编号sid=[{}]", sid);
        int id = 0;
        try {
            id = orderService.createWrongOrder(sid);
            LOGGER.info("创建订单id: [{}]", id);
        } catch (Exception e) {
            LOGGER.error("Exception", e);
        }
        return String.valueOf(id);
    }

    /*
        使用令牌桶＋乐观锁
     */
    @RequestMapping("/createOptimisticOrder/{sid}")
    @ResponseBody
    public String createOptimisticOrder(@PathVariable int sid) {
//         阻塞式获取令牌
         LOGGER.info("等待时间"+rateLimiter.acquire());
//         非阻塞式获取令牌
//        if(!rateLimiter.tryAcquire(1000, TimeUnit.MILLISECONDS)){
//            LOGGER.warn("对不起，您已被限流");
//            return "购买失败，库存不足";
//        }
        int id;
        try {
            id = orderService.createOptimisticOrder(sid);
            LOGGER.info("购买成功，剩余库存为: [{}]", id);
        } catch (Exception e) {
            LOGGER.error("购买失败：[{}]", e.getMessage());
            return "购买失败，库存不足";
        }
        return String.format("购买成功，剩余库存为：%d", id);
    }

    /*
        通过redis存储验证值实现接口隐藏
     */
    @RequestMapping(value = "/createOrderWithVerifiedUrl",method = {RequestMethod.GET})
    @ResponseBody
    public String createOrderWithVerifiedUrl(@RequestParam(value = "sid") Integer sid,
                                             @RequestParam(value = "userId") Integer userId,
                                             @RequestParam(value = "verifyHash") String verifyHash){
        int stockLeft;
        try{
            stockLeft=orderService.createVerifiedOrder(sid,userId,verifyHash);
        } catch (Exception e){
            LOGGER.error("购买失败，原因：[{}]",e.getMessage());
            return e.getMessage();
        }
        return String.format("购买成功，剩余库存为:%d",stockLeft);
    }

    /*
        获取验证值
     */
    @RequestMapping(value = "/getVerifyHash",method = {RequestMethod.GET})
    @ResponseBody
    public String getVerifyHash(@RequestParam(value = "sid") Integer sid,
                                @RequestParam(value = "userId") Integer userId){
        String hash;
        try{
            hash=userService.getVerifyHash(sid,userId);
        }catch (Exception e) {
            LOGGER.error("获取验证hash失败，原因为：[{}]", e.getMessage());
            return "获取验证hash失败";
        }
        return String.format("请求抢购验证hash值为:%s",hash);
    }
}
