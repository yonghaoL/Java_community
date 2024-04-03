package com.learn.community.controller;

import com.learn.community.entity.Event;
import com.learn.community.entity.User;
import com.learn.community.event.EventProducer;
import com.learn.community.service.LikeService;
import com.learn.community.util.CommunityConstant;
import com.learn.community.util.CommunityUtil;
import com.learn.community.util.HostHolder;
import com.learn.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

@Controller
public class LikeController implements CommunityConstant {

    @Autowired
    private LikeService likeService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private EventProducer eventProducer;

    @Autowired
    private RedisTemplate redisTemplate;

    //处理异步请求的方法
    @RequestMapping(path = "/like", method = RequestMethod.POST)
    @ResponseBody
    //要求点赞的时候也传进当前帖子详情的id
    public String like(int entityType, int entityId, int entityUserId, int postId) { //参数为被点赞的实体类型和id，以及被点赞的内容发布的用户的id
        User user = hostHolder.getUser(); //获取点赞的用户id
        if (user == null) {
            return CommunityUtil.getJSONString(403, "你还没有登录哦!");
        }

        // 点赞
        likeService.like(user.getId(), entityType, entityId, entityUserId);

        // 统计赞的数量以显示在网页上
        long likeCount = likeService.findEntityLikeCount(entityType, entityId);
        // 显示点赞的状态（已点赞，未点赞）
        int likeStatus = likeService.findEntityLikeStatus(user.getId(), entityType, entityId);
        // 返回的结果
        Map<String, Object> map = new HashMap<>();
        map.put("likeCount", likeCount);
        map.put("likeStatus", likeStatus);

        // 触发点赞事件
        if (likeStatus == 1) { //取消赞就不发通知了，点赞才通知
            Event event = new Event()
                    .setTopic(TOPIC_LIKE)
                    .setUserId(hostHolder.getUser().getId())
                    .setEntityType(entityType)
                    .setEntityId(entityId)
                    .setEntityUserId(entityUserId)
                    .setData("postId", postId);
            eventProducer.fireEvent(event);
        }

        if(entityType == ENTITY_TYPE_POST) {
            // 加入帖子分数计算任务缓存
            String redisKey = RedisKeyUtil.getPostScoreKey();
            redisTemplate.opsForSet().add(redisKey, postId);
        }


        return CommunityUtil.getJSONString(0, null, map); //0表示正确，null表示返回给页面的提示，map是返回给页面的数据
    }

}
