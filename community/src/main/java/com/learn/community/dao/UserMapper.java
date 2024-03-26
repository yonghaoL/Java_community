package com.learn.community.dao;

import com.learn.community.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper //打开这个注解，才能被容器去扫描装配它
public interface UserMapper {

    User selectById(int id);

    User selectByName(String username);

    User selectByEmail(String email);

    int insertUser(User user);

    int updateStatus(int id, int status);

    int updateHeader(int id, String headerUrl);

    int updatePassword(int id, String password);

}
