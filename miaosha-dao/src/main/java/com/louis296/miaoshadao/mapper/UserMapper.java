package com.louis296.miaoshadao.mapper;


import com.louis296.miaoshadao.dao.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper {
    User selectByPrimaryKey(long uid);
}
