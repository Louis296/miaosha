package com.louis296.miaoshaservice.service;

import com.louis296.miaoshadao.dao.Stock;
import com.louis296.miaoshadao.dao.StockOrder;
import com.louis296.miaoshadao.dao.User;
import com.louis296.miaoshadao.mapper.StockOrderMapper;
import com.louis296.miaoshadao.mapper.UserMapper;
import com.louis296.miaoshadao.utils.CacheKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderServiceImpl implements OrderService{

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderServiceImpl.class);

    @Autowired
    private StockService stockService;

    @Autowired
    private StockOrderMapper orderMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private UserMapper userMapper;

    @Override
    public int createOptimisticOrder(int sid) throws Exception {
        //校验库存
        Stock stock = checkStock(sid);
        //乐观锁更新库存
        saleStockOptimistic(stock);
        //创建订单
        int id = createOrder(stock);
        return stock.getCount() - (stock.getSale()+1);
    }

    @Transactional(rollbackFor = Exception.class,propagation = Propagation.REQUIRED)
    @Override
    public int createPessimisticOrder(int sid) {
        Stock stock=checkStockForUpdate(sid);
        saleStock(stock);
        createOrder(stock);
        return stock.getCount()-stock.getSale()-1;
    }

    private void saleStockOptimistic(Stock stock) {
        LOGGER.info("查询数据库，尝试更新库存");
        int count = stockService.updateStockByOptimistic(stock);
        if (count == 0){
            throw new RuntimeException("并发更新库存失败，version不匹配");
        }
    }

    @Override
    public int createWrongOrder(int sid) {
        //校验库存
        Stock stock = checkStock(sid);
        //扣库存
        saleStock(stock);
        //创建订单
        int id = createOrder(stock);
        return id;
    }

    @Override
    public int createVerifiedOrder(Integer sid, Integer userId, String verifyHash) throws Exception {
        LOGGER.info("验证是否在抢购时间内");

        String hashKey= CacheKey.HASH_KEY.getKey()+"_"+sid+"_"+userId;
        String verifyHashInRedis=stringRedisTemplate.opsForValue().get(hashKey);
        if(!verifyHashInRedis.equals(verifyHash)){
            throw new Exception("hash值与Redis中存储的不匹配");
        }
        LOGGER.info("验证hash值成功");

        User user=userMapper.selectByPrimaryKey(userId.longValue());
        if (user==null){
            throw new Exception("用户不存在");
        }
        LOGGER.info("用户信息验证成功:[{}]",user.toString());

        Stock stock=stockService.getStockById(sid);
        if (stock==null){
            throw new Exception("商品不存在");
        }
        LOGGER.info("商品信息验证成功:[{}]",stock.toString());

        saleStockOptimistic(stock);
        LOGGER.info("乐观锁更新库存成功");

        createOrderWithUserInfoInDB(stock,userId);
        LOGGER.info("创建订单成功");

        return stock.getCount()-stock.getSale()-1;
    }

    @Override
    public void createOrderByMq(Integer sid, Integer userId) throws Exception {
        Stock stock;
        try{
            stock=checkStock(sid);
        }catch (Exception e){
            LOGGER.info("库存不足！");
            return;
        }

        saleStockOptimistic(stock);

        LOGGER.info("扣减库存成功，剩余库存：[{}]",stock.getCount()-stock.getSale()-1);
        stockService.delStockCountCache(sid);
        LOGGER.info("删除库存缓存");

        LOGGER.info("写入订单至数据库");
        createOrderWithUserInfoInDB(stock,userId);
        LOGGER.info("写入订单至缓存");
        createOrderWithUserInfoInCache(stock,userId);
        LOGGER.info("下单完成");

    }

    @Override
    public Boolean checkUserOrderInfoInCache(Integer sid, Integer userId) throws Exception {
        String key=CacheKey.USER_HAS_ORDER.getKey()+"_"+sid;
        LOGGER.info("检查用户Id:[{}]是否抢购过商品Id:[{}]，检查的key为:[{}]",userId,sid,key);
        return stringRedisTemplate.opsForSet().isMember(key,userId.toString());
    }

    private Stock checkStock(int sid) {
        Stock stock = stockService.getStockById(sid);
        if (stock.getSale().equals(stock.getCount())) {
            throw new RuntimeException("库存不足");
        }
        return stock;
    }

    private int saleStock(Stock stock) {
        stock.setSale(stock.getSale() + 1);
        return stockService.updateStockById(stock);
    }

    private int createOrder(Stock stock) {
        StockOrder order = new StockOrder();
        order.setSid(stock.getId());
        order.setName(stock.getName());
        int id = orderMapper.insertSelective(order);
        return id;
    }

    private int createOrderWithUserInfoInDB(Stock stock,Integer uid){
        StockOrder order=new StockOrder();
        order.setSid(stock.getId());
        order.setName(stock.getName());
        order.setUserId(uid);
        return orderMapper.insertSelective(order);
    }

    private Stock checkStockForUpdate(int sid){
        Stock stock=stockService.getStockByIdForUpdate(sid);
        if(stock.getSale().equals(stock.getCount())){
            throw new RuntimeException("库存不足");
        }
        return stock;
    }

    private Long createOrderWithUserInfoInCache(Stock stock,Integer userId){
        String key=CacheKey.USER_HAS_ORDER.getKey()+"_"+stock.getId().toString();
        LOGGER.info("写入用户订单数据SET:[{}][{}]",key,userId.toString());
        return stringRedisTemplate.opsForSet().add(key,userId.toString());
    }
}
