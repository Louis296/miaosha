package com.louis296.miaoshaservice.service;



public interface OrderService {

    int createOptimisticOrder(int sid) throws Exception;
    int createWrongOrder(int sid);
    int createVerifiedOrder(Integer sid,Integer userId,String verifyHash) throws Exception;


}
