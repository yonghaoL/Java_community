package com.learn.community.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) { //string自动注入了一个RedisConnectionFactory bean，即连接工厂bean，可以用于创建连接
        RedisTemplate<String, Object> template = new RedisTemplate<>(); //键为string，值为object
        template.setConnectionFactory(factory); //factory赋值给template使之可以访问数据库

        // 序列化即 将java对象序列化，以字符串或者json等等格式存储在数据库中
        // 设置key的序列化方式
        template.setKeySerializer(RedisSerializer.string());
        // 设置value的序列化方式
        template.setValueSerializer(RedisSerializer.json());
        // 设置hash的key的序列化方式
        template.setHashKeySerializer(RedisSerializer.string());
        // 设置hash的value的序列化方式
        template.setHashValueSerializer(RedisSerializer.json());

        template.afterPropertiesSet(); // 使template的设置生效
        return template;
    }

}
