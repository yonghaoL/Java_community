package com.learn.community.controller;

import com.learn.community.annotation.LoginRequired;
import com.learn.community.entity.Comment;
import com.learn.community.entity.DiscussPost;
import com.learn.community.entity.Event;
import com.learn.community.event.EventProducer;
import com.learn.community.service.CommentService;
import com.learn.community.service.DiscussPostService;
import com.learn.community.util.CommunityConstant;
import com.learn.community.util.HostHolder;
import com.learn.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

@Controller
@RequestMapping("/comment")
public class CommentController implements CommunityConstant {

    @Autowired
    private CommentService commentService;

    @Autowired
    private HostHolder hostHolder;

    //注入生产者
    @Autowired
    private EventProducer eventProducer;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private RedisTemplate redisTemplate;

//    @LoginRequired需要登录
    @RequestMapping(path = "/add/{discussPostId}", method = RequestMethod.POST)
    public String addComment(@PathVariable("discussPostId") int discussPostId, Comment comment) { //sping直接注入一个comment实体（将前台传输的值按名字赋予其属性）
        //继续对comment的属性进行补充
        comment.setUserId(hostHolder.getUser().getId());
        comment.setStatus(0);
        comment.setCreateTime(new Date());
        commentService.addComment(comment);

        // 触发评论事件
        Event event = new Event()
                .setTopic(TOPIC_COMMENT) //主题为comment
                .setUserId(hostHolder.getUser().getId()) //获取触发事件的登录者id
                .setEntityType(comment.getEntityType()) //事件类型
                .setEntityId(comment.getEntityId()) //事件id
                .setData("postId", discussPostId); //收到通知时，为了能够链接到帖子详情，需要存下帖子id
        if (comment.getEntityType() == ENTITY_TYPE_POST) { //若评论的是帖子，要查发帖人id
            DiscussPost target = discussPostService.findDiscussPostById(comment.getEntityId());
            event.setEntityUserId(target.getUserId()); //加入事件发布者id
        } else if (comment.getEntityType() == ENTITY_TYPE_COMMENT) { //若评论的是评论，要查发评论的人的id
            Comment target = commentService.findCommentById(comment.getEntityId());
            event.setEntityUserId(target.getUserId());
        }
        eventProducer.fireEvent(event); //调用kafka模板发送消息


        if (comment.getEntityType() == ENTITY_TYPE_POST) { //需要判定是否是评论了帖子（而不是回复了评论）
            // 触发发帖事件
            event = new Event()
                    .setTopic(TOPIC_PUBLISH)
                    .setUserId(comment.getUserId())
                    .setEntityType(ENTITY_TYPE_POST)
                    .setEntityId(discussPostId);
            eventProducer.fireEvent(event);
            // 计算帖子分数
            String redisKey = RedisKeyUtil.getPostScoreKey();
            redisTemplate.opsForSet().add(redisKey, discussPostId);
        }

        return "redirect:/discuss/detail/" + discussPostId; //发表完回复后又跳转回帖子页面
    }

}
