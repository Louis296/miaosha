package com.louis296.miaoshaservice.service;



public interface OrderService {

    int createOptimisticOrder(int sid) throws Exception;
    int createWrongOrder(int sid);


}
