//Service是controller调用的用来处理业务的代码，尽管在本项目中该代码似乎只是简单调用dao的方法
//但这些代码不要写在dao中，我们要尽量保证代码层次的分明，降低耦合性
package com.learn.community.service;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.learn.community.dao.DiscussPostMapper;
import com.learn.community.entity.DiscussPost;
import com.learn.community.util.SensitiveFilter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service //加入该注解使得它可以被容器扫描到
public class DiscussPostService {
    private static final Logger logger = LoggerFactory.getLogger(DiscussPostService.class);

    @Value("${caffeine.posts.max-size}") // 缓存的帖子页面最大数量（注意我们缓存是以页为单位！）
    private int maxSize;

    @Value("${caffeine.posts.expire-seconds}") // 缓存过期的时间为3mins（注意这也就是说用户对缓存中的更新有延迟！）
    private int expireSeconds;

    @Autowired
    private DiscussPostMapper discussPostMapper; //注入DiscussPostMapper,尽管我们只定义了接口和对应sql语句（写在xml文件中），mybatis会帮我们实现类并创建实例

    @Autowired
    private SensitiveFilter sensitiveFilter;

    // Caffeine核心接口: Cache, LoadingCache, AsyncLoadingCache,其中LoadingCache是同步缓存，不能并发从数据库取数据加载到缓存

    // 帖子列表的缓存
    private LoadingCache<String, List<DiscussPost>> postListCache; //所有的缓存都需要key和value，然后按照key来缓存value

    // 帖子总数的缓存
    private LoadingCache<Integer, Integer> postRowsCache;

    @PostConstruct //服务启用或者首次调用service的时候初始化缓存（@PostConstruct注解的作用）
    public void init() {
        // 初始化帖子列表缓存
        postListCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<String, List<DiscussPost>>() { //匿名实现该接口
                    @Nullable
                    @Override
                    public List<DiscussPost> load(@NonNull String key) throws Exception { //告诉caffeine当缓存没有数据时，去哪里取得数据返回,并且将该数据存进缓存中
                        if (key == null || key.length() == 0) {
                            throw new IllegalArgumentException("参数错误!");
                        }

                        String[] params = key.split(":");
                        if (params == null || params.length != 2) {
                            throw new IllegalArgumentException("参数错误!");
                        }

                        int offset = Integer.valueOf(params[0]);
                        int limit = Integer.valueOf(params[1]);

                        logger.debug("load post list from DB.");
                        return discussPostMapper.selectDiscussPosts(0, offset, limit, 1);
                    }
                });
        // 初始化帖子总数缓存
        postRowsCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<Integer, Integer>() {
                    @Nullable
                    @Override
                    public Integer load(@NonNull Integer key) throws Exception { //告诉caffeine当缓存没有数据时，去哪里取得数据返回
                        logger.debug("load post rows from DB.");
                        return discussPostMapper.selectDiscussPostRows(key);
                    }
                });
    }

    //声明一个业务方法，查询post集合
    public List<DiscussPost> findDiscussPosts(int userId, int offset, int limit, int orderMode){
        if (userId == 0 && orderMode == 1) { //先从缓存里面找（如果是要访问主页的热门帖子页数的话）
            return postListCache.get(offset + ":" + limit);
        }
        logger.debug("load post rows from DB.");
        return discussPostMapper.selectDiscussPosts(userId, offset, limit, orderMode);
    }

    //根据id查询post数量
    public int findDiscussPostRows(int userId){
        if (userId == 0) {
            return postRowsCache.get(userId);
        }

        logger.debug("load post rows from DB.");
        return discussPostMapper.selectDiscussPostRows(userId);
    }

    public int addDiscussPost(DiscussPost post) {
        if (post == null) {
            throw new IllegalArgumentException("参数不能为空!");
        }

        // 转义HTML标记，避免用户在标题和内容中使用html语法破坏网页内容
        post.setTitle(HtmlUtils.htmlEscape(post.getTitle()));
        post.setContent(HtmlUtils.htmlEscape(post.getContent()));
        // 过滤敏感词
        post.setTitle(sensitiveFilter.filter(post.getTitle()));
        post.setContent(sensitiveFilter.filter(post.getContent()));

        return discussPostMapper.insertDiscussPost(post);
    }

    public DiscussPost findDiscussPostById(int id) {
        return discussPostMapper.selectDiscussPostById(id);
    }

    public int updateCommentCount(int id, int commentCount) {
        return discussPostMapper.updateCommentCount(id, commentCount);
    }

    public int updateType(int id, int type) {
        return discussPostMapper.updateType(id, type);
    }

    public int updateStatus(int id, int status) {
        return discussPostMapper.updateStatus(id, status);
    }

    //更新帖子分数
    public int updateScore(int id, double score) {
        return discussPostMapper.updateScore(id, score);
    }
}
