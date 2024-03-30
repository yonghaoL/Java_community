package com.learn.community.dao;

import com.learn.community.entity.LoginTicket;
import org.apache.ibatis.annotations.*;

@Mapper
@Deprecated
public interface LoginTicketMapper {

    //可以用注解来实现对应sql语句（也可以用xml文件配置，之前的DiscussPostMapper就是用xml文件）
    @Insert({
            "insert into login_ticket(user_id,ticket,status,expired) ", //会把这多行sql语句拼一起，注意上一句末尾加空格
            "values(#{userId},#{ticket},#{status},#{expired})"
    })
    //配置文件中给mybatis设置插入时主键自动生成
    int insertLoginTicket(LoginTicket loginTicket); //插入凭证

    @Select({
            "select id,user_id,ticket,status,expired ",
            "from login_ticket where ticket=#{ticket}"
    })
    LoginTicket selectByTicket(String ticket); //查询登录凭证

    //注解里面也可以写动态sql（即不确定的sql语句，用if标签等）
    @Update({
            "<script>",
            "update login_ticket set status=#{status} where ticket=#{ticket} ",
            "<if test=\"ticket!=null\"> ",
            "and 1=1 ",
            "</if>",
            "</script>"
    })
    int updateStatus(String ticket, int status); //修改登录凭证，可以退出（修改status）

}
