package com.louis296.miaoshaservice.service;

public interface UserService {
    String getVerifyHash(Integer sid,Integer userId) throws Exception;
}
