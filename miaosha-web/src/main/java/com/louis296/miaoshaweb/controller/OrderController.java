package com.louis296.miaoshaweb.controller;

import com.google.common.util.concurrent.RateLimiter;
import com.louis296.miaoshaservice.service.OrderService;
import com.louis296.miaoshaservice.service.StockService;
import com.louis296.miaoshaservice.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Controller
public class OrderController {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderController.class);
    private static ExecutorService cachedThreadPool=new ThreadPoolExecutor(0,Integer.MAX_VALUE,60L,TimeUnit.SECONDS,new SynchronousQueue<Runnable>());
    private static final int DELAY_MILLSECONDS=1000;
    @Autowired
    private OrderService orderService;

    @Autowired
    private UserService userService;

    @Autowired
    private StockService stockService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

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

    /*
        要求验证的抢购接口＋单用户访问频率限制
     */
    @RequestMapping(value = "/createOrderWithVerifiedUrlAndLimit",method = {RequestMethod.GET})
    @ResponseBody
    public String createOrderWithVerifiedUrlAndLimit(@RequestParam(value = "sid") Integer sid,
                                                     @RequestParam(value = "userId") Integer userId,
                                                     @RequestParam(value = "verifyHash") String verifyHash){
        LOGGER.info("等待时间"+rateLimiter.acquire());
        int stockLeft;
        try{
            int count=userService.addUserCount(userId);
            LOGGER.info("用户截至该次的访问次数为:[{}]",count);
            boolean isBanned= userService.getUserIsBanned(userId);
            if (isBanned){
                return "购买频率超过限制";
            }
            stockLeft=orderService.createVerifiedOrder(sid,userId,verifyHash);
            LOGGER.info("购买成功，剩余库存为:[{}]",stockLeft);
        }catch (Exception e){
            LOGGER.error("购买失败，原因:[{}]",e.getMessage());
            return e.getMessage();
        }
        return String.format("购买成功，剩余库存为:%d",stockLeft);
    }

    /*
        先删除缓存，再更新数据库
     */
    @RequestMapping("/createOrderWithCacheV1/{sid}")
    @ResponseBody
    public String createOrderWithCacheV1(@PathVariable int sid){
        int count=0;
        try{
            stockService.delStockCountCache(sid);
            count=orderService.createPessimisticOrder(sid);
        }catch (Exception e){
            LOGGER.error("购买失败:[{}]",e.getMessage());
            return "购买失败，库存不足";
        }
        LOGGER.info("购买成功，剩余库存为:[{}]",count);
        return String.format("购买成功，剩余库存为:%d",count);
    }

    /*
        先更新数据库，再删除缓存
     */
    @RequestMapping("/createOrderWithCacheV2/{sid}")
    @ResponseBody
    public String createOrderWithCacheV2(@PathVariable int sid){
        int count=0;
        try{
            count=orderService.createPessimisticOrder(sid);
            stockService.delStockCountCache(sid);
        }catch (Exception e){
            LOGGER.error("购买失败:[{}]",e.getMessage());
            return "购买失败，库存不足";
        }
        LOGGER.info("购买成功，剩余库存为:[{}]",count);
        return String.format("购买成功，剩除库存为:%d",count);
    }

    /**
     * 延时双删：先删缓存，再更新数据库，延时一定时间为再删缓存
     * @param sid
     * @return
     */
    @RequestMapping("/createOrderWithCacheV3/{sid}")
    @ResponseBody
    public String createOrderWithCacheV3(@PathVariable int sid){
        int count;
        try{
            stockService.delStockCountCache(sid);
            count=orderService.createPessimisticOrder(sid);
            cachedThreadPool.execute(new delCacheByThread(sid));
        }catch (Exception e){
            LOGGER.error("购买失败:[{}]",e.getMessage());
            return "购买失败，库存不足";
        }
        LOGGER.info("购买成功，剩余库存为:[{}]",count);
        return String.format("购买成功，剩余库存为:%d",count);
    }

    /**
     * 缓存再删除线程
     */
    private class delCacheByThread implements Runnable{

        private int sid;
        public delCacheByThread(int sid){
            this.sid=sid;
        }

        @Override
        public void run() {
            try{
                LOGGER.info("异步执行缓存再删除，商品id:[{}]，首先休眠:[{}]毫秒",sid,DELAY_MILLSECONDS);
                Thread.sleep(DELAY_MILLSECONDS);
                stockService.delStockCountCache(sid);
                LOGGER.info("再次删除商品id:[{}]缓存",sid);
            }catch (Exception e){
                LOGGER.error("delCacheByThread执行出错",e);
            }
        }
    }

    /**
     * 基于消息队列的双删机制
     * @param sid
     * @return
     */
    @RequestMapping("/createOrderWithCacheV4/{sid}")
    @ResponseBody
    public String createOrderWithCacheV4(@PathVariable Integer sid){
        int count;
        try{
            count=orderService.createPessimisticOrder(sid);
            stockService.delStockCountCache(sid);
            sendToDelCache(String.valueOf(sid));
        }catch (Exception e){
            LOGGER.error("购买失败：[{}]",e.getMessage());
            return "购买失败，库存不足";
        }
        LOGGER.info("购买成功，剩余库存为:[{}]",count);
        return String.format("购买成功，剩余库存为：%d",count);
    }

    public void sendToDelCache(String message){
        LOGGER.info("通知消息队列开始重试删除缓存：[{}]",message);
        this.rabbitTemplate.convertAndSend("delCache",message);
    }
}
