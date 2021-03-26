package com.louis296.miaoshaservice.service;



public interface OrderService {

    int createOptimisticOrder(int sid) throws Exception;
    int createPessimisticOrder(int sid);
    int createWrongOrder(int sid);
    int createVerifiedOrder(Integer sid,Integer userId,String verifyHash) throws Exception;
    void createOrderByMq(Integer sid,Integer userID) throws Exception;
    Boolean checkUserOrderInfoInCache(Integer sid,Integer userId) throws Exception;

}
