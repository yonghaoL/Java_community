package com.learn.community.service;

import com.learn.community.entity.User;
import com.learn.community.util.CommunityConstant;
import com.learn.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class FollowService implements CommunityConstant {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private UserService userService;

    //关注
    public void follow(int userId, int entityType, int entityId) {
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType); //获取想要关注实体的用户的key
                String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId); //获取被关注的实体key

                //编程式事务
                operations.multi();

                operations.opsForZSet().add(followeeKey, entityId, System.currentTimeMillis()); //输入key即被关注的实体id
                operations.opsForZSet().add(followerKey, userId, System.currentTimeMillis()); //输入key和准备关注实体的用户id

                return operations.exec();
            }
        });
    }

    //取消关注
    public void unfollow(int userId, int entityType, int entityId) {
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
                String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);

                operations.multi();

                operations.opsForZSet().remove(followeeKey, entityId); //remove即可
                operations.opsForZSet().remove(followerKey, userId);

                return operations.exec();
            }
        });
    }

    // 查询某用户关注的实体的数量
    public long findFolloweeCount(int userId, int entityType) {
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
        return redisTemplate.opsForZSet().zCard(followeeKey);
    }

    // 查询实体的粉丝的数量
    public long findFollowerCount(int entityType, int entityId) {
        String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);
        return redisTemplate.opsForZSet().zCard(followerKey);
    }

    // 查询当前用户是否已关注该实体，在前端可以显示按钮
    public boolean hasFollowed(int userId, int entityType, int entityId) { //查找当前用户的关注目标中是否有该实体（可以用分数来查询是否为空）
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
        return redisTemplate.opsForZSet().score(followeeKey, entityId) != null;
    }

    // 查询某用户关注的人（todo:查询关注的帖子和评论）
    public List<Map<String, Object>> findFollowees(int userId, int offset, int limit) { //注意分页
        //返回一个map集合，返回user对象和关注时间等
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, ENTITY_TYPE_USER);
        //注意redis的range方法返回的set是它自己重写的set实现类，里面是有序的数据集合。这里的赋值可能是一个多态场景
        Set<Integer> targetIds = redisTemplate.opsForZSet().reverseRange(followeeKey, offset, offset + limit - 1); //查offset到offset + limit - 1页

        if (targetIds == null) {//没有关注的人
            return null;
        }

        //根据关注的人的id得到user对象和关注时间并作为map插入List
        List<Map<String, Object>> list = new ArrayList<>();
        for (Integer targetId : targetIds) {
            Map<String, Object> map = new HashMap<>();
            User user = userService.findUserById(targetId);
            map.put("user", user);
            Double score = redisTemplate.opsForZSet().score(followeeKey, targetId);
            map.put("followTime", new Date(score.longValue()));
            list.add(map);
        }

        return list;
    }

    // 查询某用户的粉丝，与上面同理，不再赘述
    public List<Map<String, Object>> findFollowers(int userId, int offset, int limit) {
        String followerKey = RedisKeyUtil.getFollowerKey(ENTITY_TYPE_USER, userId);

        //注意redis的range方法返回的set是它自己重写的set实现类，里面是有序的数据集合。这里的赋值可能是一个多态场景
        Set<Integer> targetIds = redisTemplate.opsForZSet().reverseRange(followerKey, offset, offset + limit - 1);

        if (targetIds == null) {
            return null;
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (Integer targetId : targetIds) {
            Map<String, Object> map = new HashMap<>();
            User user = userService.findUserById(targetId);
            map.put("user", user);
            Double score = redisTemplate.opsForZSet().score(followerKey, targetId);
            map.put("followTime", new Date(score.longValue()));
            list.add(map);
        }

        return list;
    }

}
