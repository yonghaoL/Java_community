package com.learn.community.actuator;

import com.learn.community.util.CommunityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Component
@Endpoint(id = "database") //自定义了一个端口，该端口名字是“database”，可以通过该名字来访问该端口
public class DatabaseEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseEndpoint.class);

    @Autowired
    private DataSource dataSource;

    @ReadOperation //该注解说明这个方法我们是通过get请求来访问的
    //注意由于actuator是查看系统数据的，且可以通过浏览器访问，所有务必设置权限！
    public String checkConnection() {
        try (
                Connection conn = dataSource.getConnection(); //用该端点测试是否能成功连接数据库（注意该方法是get请求，可以在服务器启动期间进行检查测试）
        ) {
            return CommunityUtil.getJSONString(0, "获取连接成功!");
        } catch (SQLException e) {
            logger.error("获取连接失败:" + e.getMessage());
            return CommunityUtil.getJSONString(1, "获取连接失败!");
        }
    }

}
