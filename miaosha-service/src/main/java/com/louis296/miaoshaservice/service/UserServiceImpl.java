package com.louis296.miaoshaservice.service;

import com.louis296.miaoshadao.dao.Stock;
import com.louis296.miaoshadao.dao.User;
import com.louis296.miaoshadao.mapper.UserMapper;
import com.louis296.miaoshadao.utils.CacheKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl implements UserService{

    private static final Logger LOGGER= LoggerFactory.getLogger(UserServiceImpl.class);
    private static final String SALT="aRandomString";
    private static final int ALLOW_COUNT=10;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private StockService stockService;

    @Override
    public String getVerifyHash(Integer sid, Integer userId) throws Exception {
        LOGGER.info("验证是否在抢购时间内");

        User user=userMapper.selectByPrimaryKey(userId.longValue());
        if (user==null){
            throw new Exception("用户不存在");
        }
        LOGGER.info("用户信息：[{}]",user.toString());

        Stock stock=stockService.getStockById(sid);
        if(stock==null){
            throw new Exception("商品不存在");
        }
        LOGGER.info("商品信息：[{}]",stock.toString());

        String verify=SALT+sid+userId;
        String verifyHash= DigestUtils.md5DigestAsHex(verify.getBytes());

        String hashKey= CacheKey.HASH_KEY.getKey()+"_"+sid+"_"+userId;
        stringRedisTemplate.opsForValue().set(hashKey,verifyHash,3600, TimeUnit.SECONDS);
        LOGGER.info("Redis写入:[{}][{}]",hashKey,verifyHash);
        return verifyHash;
    }

    @Override
    public int addUserCount(Integer userId) throws Exception {
        String limitKey=CacheKey.LIMIT_KEY.getKey()+"_"+userId;
        String limitNum=stringRedisTemplate.opsForValue().get(limitKey);
        int limit=-1;
        if(limitNum==null){
            stringRedisTemplate.opsForValue().set(limitKey,"0",3600,TimeUnit.SECONDS);
        }else{
            limit=Integer.parseInt(limitNum)+1;
            stringRedisTemplate.opsForValue().set(limitKey,String.valueOf(limit),3600,TimeUnit.SECONDS);
        }
        return limit;
    }

    @Override
    public boolean getUserIsBanned(Integer userId) {
        String limitKey=CacheKey.LIMIT_KEY.getKey()+"_"+userId;
        String limitNum=stringRedisTemplate.opsForValue().get(limitKey);
        if(limitNum==null){
            LOGGER.error("该用户没有访问申请验证值记录");
            return true;
        }
        return Integer.parseInt(limitNum)>ALLOW_COUNT;

    }
}
