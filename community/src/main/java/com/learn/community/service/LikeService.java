package com.learn.community.service;

import com.learn.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

@Service
public class LikeService {

    @Autowired
    private RedisTemplate redisTemplate;

    // 点赞
    public void like(int userId, int entityType, int entityId, int entityUserId) { //点赞的用户id，被点赞的实体类型和实体id，发布该实体的用户id
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId); // 点赞/取消赞的key
                String userLikeKey = RedisKeyUtil.getUserLikeKey(entityUserId); //获取该发布内容对应的用户id

                boolean isMember = operations.opsForSet().isMember(entityLikeKey, userId); //检验该用户的点赞是否在数据库中，若在说明已点赞，此时操作为取消点赞

                operations.multi(); //编程式事务开始

                if (isMember) {//取消点赞
                    operations.opsForSet().remove(entityLikeKey, userId);
                    operations.opsForValue().decrement(userLikeKey); //被点赞内容的拥有者，它的被赞数+1
                } else {//点赞
                    operations.opsForSet().add(entityLikeKey, userId);
                    operations.opsForValue().increment(userLikeKey);
                }

                return operations.exec(); //编程式事务提交
            }
        });
    }

    // 查询某实体被点赞的数量
    public long findEntityLikeCount(int entityType, int entityId) {
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
        return redisTemplate.opsForSet().size(entityLikeKey); //返回该key对应的数据条数
    }

    // 查询某人对某实体的点赞状态，返回整数具有扩展性，以后可以增加点踩等功能
    public int findEntityLikeStatus(int userId, int entityType, int entityId) {
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
        return redisTemplate.opsForSet().isMember(entityLikeKey, userId) ? 1 : 0;
    }

    // 查询某个用户发布的内容获得的赞
    public int findUserLikeCount(int userId) {
        String userLikeKey = RedisKeyUtil.getUserLikeKey(userId);
        Integer count = (Integer) redisTemplate.opsForValue().get(userLikeKey); //将String转为Integer
        return count == null ? 0 : count.intValue(); //注意count为空的情况，要严谨
    }

}
