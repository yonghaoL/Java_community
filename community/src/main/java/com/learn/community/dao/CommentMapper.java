package com.learn.community.dao;

import com.learn.community.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

//mapper是spring提供的mybatis相关的注解
@Mapper
public interface CommentMapper {

    //根据实体类型和实体id查询对应的回复
    List<Comment> selectCommentsByEntity(int entityType, int entityId, int offset, int limit);

    int selectCountByEntity(int entityType, int entityId);

    int insertComment(Comment comment);

    Comment selectCommentById(int id);

}
