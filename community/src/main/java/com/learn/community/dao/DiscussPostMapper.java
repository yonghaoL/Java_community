package com.learn.community.dao;

import com.learn.community.entity.DiscussPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DiscussPostMapper {
    //userId是为了查询该用户的帖子功能
    //offset是分页查询时该页的起始行号
    //limit是分页查询时该页限制多少行
    //以上三者都是要传入sql语句的参数，sql语句支持这些功能
    List<DiscussPost> selectDiscussPosts(int userId, int offset, int limit);

    // @Param注解用于给参数取别名,这个别名可以在sql语句中使用
    // 如果只有一个参数,并且在sql的<if>里（动态条件）使用,则必须加别名.（上面的方法有三个参数，所以可以不用起别名）
    int selectDiscussPostRows(@Param("userId") int userId);//返回有多少行数据，支持查询给定用户id的查询

    int insertDiscussPost(DiscussPost discussPost);

    DiscussPost selectDiscussPostById(int id);

    int updateCommentCount(int id, int commentCount);//更新该帖子的评论数量

}
