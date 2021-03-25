package com.louis296.miaoshaservice.service;

public interface UserService {
    String getVerifyHash(Integer sid,Integer userId) throws Exception;
    int addUserCount(Integer userId) throws Exception;
    boolean getUserIsBanned(Integer userId);
}
