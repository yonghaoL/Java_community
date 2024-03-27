package com.learn.community.util;

import com.learn.community.entity.User;
import org.springframework.stereotype.Component;

/**
 * 持有用户信息,用于代替session对象.如果用session存储当前用户，不可用于分布式系统
 */
@Component
public class HostHolder {

    private ThreadLocal<User> users = new ThreadLocal<>(); //实现线程隔离，ThreadLocal里面set()方法存值时，会根据当前线程来获取map对象存值

    public void setUser(User user) {
        users.set(user);
    } //存users

    public User getUser() {
        return users.get();
    }

    public void clear() {
        users.remove();
    }

}
