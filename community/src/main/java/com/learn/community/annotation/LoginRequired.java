package com.learn.community.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD) //说明该注解写在方法上面
@Retention(RetentionPolicy.RUNTIME) //该注解在运行时有效
public @interface LoginRequired {
    //这个注解标识那些只有用户登录了才能访问的请求方法，例如修改个人设置的setting
}
