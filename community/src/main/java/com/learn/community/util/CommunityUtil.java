package com.learn.community.util;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.DigestUtils;

import java.util.UUID;

//此工具类功能简单，就不使用注解让容器托管了
public class CommunityUtil {

    // 生成随机字符串
    public static String generateUUID() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }
    //replaceAll("-", "")将随机字符串中的横杠全部替换为空字符

    // MD5加密
    // hello -> abc123def456 //不加“盐”：容易被破解
    // hello + 3e4a8 -> abc123def456abc ，一般都要加“盐”使得MD5不能被破解，所有数据库中有salt字段
    public static String md5(String key) {
        if (StringUtils.isBlank(key)) {
            return null;
        }
        return DigestUtils.md5DigestAsHex(key.getBytes()); //该方法可以根据传进来的key进行MD5加密
    }

}
