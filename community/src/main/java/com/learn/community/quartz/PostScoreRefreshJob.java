package com.learn.community.quartz;

import com.learn.community.entity.DiscussPost;
import com.learn.community.service.DiscussPostService;
import com.learn.community.service.ElasticsearchService;
import com.learn.community.service.LikeService;
import com.learn.community.util.CommunityConstant;
import com.learn.community.util.RedisKeyUtil;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PostScoreRefreshJob implements Job, CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(PostScoreRefreshJob.class);

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private LikeService likeService;

    @Autowired
    private ElasticsearchService elasticsearchService; //最新数据要放到搜索引擎中

    // 网站建立时间
    private static final Date epoch;

    static { //代码块初始化全局变量epoch
        try {
            epoch = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2022-08-01 00:00:00");
        } catch (ParseException e) {
            throw new RuntimeException("初始化网站建立时间失败!", e);
        }
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException { //定时任务的操作流程
        String redisKey = RedisKeyUtil.getPostScoreKey();
        BoundSetOperations operations = redisTemplate.boundSetOps(redisKey); //定义operations集合来依次处理任务，每次调用pop会弹出该任务（从redis中删除！）

        if (operations.size() == 0) {
            logger.info("[任务取消] 没有需要刷新的帖子!");
            return;
        }

        logger.info("[任务开始] 正在刷新帖子分数: " + operations.size());
        while (operations.size() > 0) {
            this.refresh((Integer) operations.pop()); //将object转为integer
        }
        logger.info("[任务结束] 帖子分数刷新完毕!");
    }

    private void refresh(int postId) {
        DiscussPost post = discussPostService.findDiscussPostById(postId);

        if (post == null) { //若帖子不存在
            logger.error("该帖子不存在: id = " + postId);
            return;
        }

        // 是否精华
        boolean wonderful = post.getStatus() == 1;
        // 评论数量
        int commentCount = post.getCommentCount();
        // 点赞数量
        long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, postId);

        // 计算权重
        double w = (wonderful ? 75 : 0) + commentCount * 10 + likeCount * 2; //计算公署
        // 分数 = 帖子权重 + 距离天数
        double score = Math.log10(Math.max(w, 1)) //避免w=0
                + (post.getCreateTime().getTime() - epoch.getTime()) / (1000 * 3600 * 24); //按天数来计算分数
        // 更新帖子分数（数据库中）
        discussPostService.updateScore(postId, score);
        // 同步搜索数据存入ES中
        post.setScore(score);
        elasticsearchService.saveDiscussPost(post);
    }

}
