package com.learn.community.service;

import com.learn.community.dao.DiscussPostMapper;
import com.learn.community.dao.UserMapper;
import com.learn.community.entity.DiscussPost;
import com.learn.community.entity.User;
import com.learn.community.util.CommunityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
//@Scope("prototype")
public class AlphaService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Autowired
    private TransactionTemplate transactionTemplate; //这个bean是sping自动创建并自动装配到容器里

    private static final Logger logger = LoggerFactory.getLogger(AlphaService.class);

    //声明式事务//
    // REQUIRED: 支持当前事务(外部事务),如果不存在则创建新事务.
    // REQUIRES_NEW: 创建一个新事务,并且暂停当前事务(外部事务).
    // NESTED: 如果当前存在事务(外部事务),则嵌套在该事务中执行(独立的提交和回滚),否则就和REQUIRED一样（即创建新事务）.
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED) //该注解可以进行事务隔离
    public Object save1() {
        // 新增用户
        User user = new User();
        user.setUsername("alpha");
        user.setSalt(CommunityUtil.generateUUID().substring(0, 5));
        user.setPassword(CommunityUtil.md5("123" + user.getSalt()));
        user.setEmail("alpha@qq.com");
        user.setHeaderUrl("http://image.learn.com/head/99t.png");
        user.setCreateTime(new Date());
        userMapper.insertUser(user);

        // 新增帖子
        DiscussPost post = new DiscussPost();
        post.setUserId(user.getId());
        post.setTitle("Hello");
        post.setContent("新人报道!");
        post.setCreateTime(new Date());
        discussPostMapper.insertDiscussPost(post);

        Integer.valueOf("abc"); //制造错误，若不进行任何事务处理，则前面的操作依然会生效，进行事务处理则回滚

        return "ok";
    }


    //编程式事务//
    public Object save2() {
        //类似声明式事务，设置事务级别和传播机制
        transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

        //实现一个接口的匿名实现类，并返回该类的匿名对象，该接口里面的抽象方法里面写事务（有点像一种创建线程的方法）
        return transactionTemplate.execute(new TransactionCallback<Object>() { //这里为了演示，泛型就用object了，该泛型指定返回值类型
            @Override
            public Object doInTransaction(TransactionStatus status) { //回调方法，这个参数是自动传进来的，我们目前用不上它，回调方法见笔记
                //代码和save1一样
                // 新增用户
                User user = new User();
                user.setUsername("beta");
                user.setSalt(CommunityUtil.generateUUID().substring(0, 5));
                user.setPassword(CommunityUtil.md5("123" + user.getSalt()));
                user.setEmail("beta@qq.com");
                user.setHeaderUrl("http://image.learn.com/head/999t.png");
                user.setCreateTime(new Date());
                userMapper.insertUser(user);

                // 新增帖子
                DiscussPost post = new DiscussPost();
                post.setUserId(user.getId());
                post.setTitle("你好");
                post.setContent("我是新人!");
                post.setCreateTime(new Date());
                discussPostMapper.insertDiscussPost(post);

                Integer.valueOf("abc");

                return "ok";
            }
        });
    }

    @Async //有该注解后，让该方法在多线程环境下被异步调用（即和主线程并发执行）
    public void execute1() {
        logger.debug("execute1");
    }

//    @Scheduled(initialDelay = 10000, fixedRate = 1000) //10000ms延迟，间隔为1000ms，只要程序启动，它就自动触发，故需要注释掉
//    public void execute2() {
//        logger.debug("execute2");
//    }

}
