package com.learn.community.service;

import com.learn.community.dao.CommentMapper;
import com.learn.community.entity.Comment;
import com.learn.community.util.CommunityConstant;
import com.learn.community.util.SensitiveFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

@Service
public class CommentService implements CommunityConstant {

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private SensitiveFilter sensitiveFilter;

    @Autowired
    private DiscussPostService discussPostService;

    //返回评论或回复（两者在数据库中是同一类型，只是entityType字段不同）
    public List<Comment> findCommentsByEntity(int entityType, int entityId, int offset, int limit) {
        return commentMapper.selectCommentsByEntity(entityType, entityId, offset, limit);
    }

    //返回评论数量或者回复数量
    public int findCommentCount(int entityType, int entityId) {
        return commentMapper.selectCountByEntity(entityType, entityId);
    }

    //增加评论或回复
    //设置事务处理的隔离级别和传播机制
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
    public int addComment(Comment comment) {
        if (comment == null) {
            throw new IllegalArgumentException("参数不能为空!");
        }

        // 添加评论
        //首先过滤敏感词以及html标签
        comment.setContent(HtmlUtils.htmlEscape(comment.getContent()));
        comment.setContent(sensitiveFilter.filter(comment.getContent()));
        int rows = commentMapper.insertComment(comment);

        // 更新帖子的评论数量值
        if (comment.getEntityType() == ENTITY_TYPE_POST) {//只有给帖子评论才增加帖子的评论数量属性，因为只有帖子这一数据类型才有commentCount字段，而评论类型没有replyCount字段（因为评论和回复在数据库中为同一类型，不好单独为评论增加该字段）
            int count = commentMapper.selectCountByEntity(comment.getEntityType(), comment.getEntityId());
            //int count1 = discussPostService.findDiscussPostById(comment.getEntityId()).getCommentCount();//这种方法可以吗？
            //即查询添加后的评论数量，然后将该数量赋值给post的属性（数据库中）
            discussPostService.updateCommentCount(comment.getEntityId(), count);
        }

        //注意添加评论和更新数量是事务

        return rows;
    }
}
