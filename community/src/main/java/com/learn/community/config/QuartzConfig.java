package com.learn.community.config;

import com.learn.community.quartz.AlphaJob;
//import com.learn.community.quartz.PostScoreRefreshJob;
import com.learn.community.quartz.PostScoreRefreshJob;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;

// 配置 -> 数据库 -> 调用
@Configuration //spring读取该配置,发现有两个bean，于是spring会初始化这两个bean用于配置job信息
public class QuartzConfig {

    // FactoryBean可简化Bean的实例化过程:
    // 1.通过FactoryBean封装Bean的实例化过程.（就像该例子中的JobDetail的实例化是非常麻烦的，而quartz为我们封装了实例化过程，使得我们可以非常简便的实例化）
    // 2.将FactoryBean装配到Spring容器里.
    // 3.将FactoryBean注入给其他的Bean.
    // 4.该Bean得到的是FactoryBean所管理的对象实例.(注意不是FactoryBean类本身的对象！)
    // 我们用这种方式初始化alphaJob要比直接初始化方便得多

    //一旦启动服务器，该配置文件会被默认加载（@Configuration），然后Quartz会根据这两个配置向数据库里面插入配置信息
    //数据库一旦有了数据，Quartz底层就会根据数据库中的配置信息去调度

    // 配置JobDetail
    //@Bean 仅仅做测试用，服务上线时将@bean注释掉，spring不会扫描它了
    public JobDetailFactoryBean alphaJobDetail() {
        JobDetailFactoryBean factoryBean = new JobDetailFactoryBean();
        factoryBean.setJobClass(AlphaJob.class); //声明这个FactoryBean管理的类型是AlphaJob.class
        factoryBean.setName("alphaJob"); //任务的名字
        factoryBean.setGroup("alphaJobGroup"); //该任务所在的组，组名
        factoryBean.setDurability(true); //任务是持久保存的
        factoryBean.setRequestsRecovery(true); //任务是可恢复的
        return factoryBean;
    }

    // 配置Trigger(SimpleTriggerFactoryBean, CronTriggerFactoryBean) ,SimpleTriggerFactoryBean是简单触发器，CronTriggerFactoryBean是复杂触发器
//    @Bean //这个FactoryBean的参数需要注入上一个bean
    public SimpleTriggerFactoryBean alphaTrigger(JobDetail alphaJobDetail) { //alphaTrigger里面注入alphaJobDetail，注意我们得到的不是JobDetailFactoryBean对象，而是一个由他管理的JobDetail对象
        SimpleTriggerFactoryBean factoryBean = new SimpleTriggerFactoryBean();
        factoryBean.setJobDetail(alphaJobDetail); //声明它是对哪个job设置的触发器
        factoryBean.setName("alphaTrigger");
        factoryBean.setGroup("alphaTriggerGroup");
        factoryBean.setRepeatInterval(3000); //该触发器的执行频率
        factoryBean.setJobDataMap(new JobDataMap()); //底层用JobDataMap来存储job的状态
        return factoryBean;
    }

    // 刷新帖子分数任务（具体操作和上面类似，改变job名字即可）
    @Bean
    public JobDetailFactoryBean postScoreRefreshJobDetail() {
        JobDetailFactoryBean factoryBean = new JobDetailFactoryBean();
        factoryBean.setJobClass(PostScoreRefreshJob.class);
        factoryBean.setName("postScoreRefreshJob");
        factoryBean.setGroup("communityJobGroup");
        factoryBean.setDurability(true);
        factoryBean.setRequestsRecovery(true);
        return factoryBean;
    }

    @Bean
    public SimpleTriggerFactoryBean postScoreRefreshTrigger(JobDetail postScoreRefreshJobDetail) {
        SimpleTriggerFactoryBean factoryBean = new SimpleTriggerFactoryBean();
        factoryBean.setJobDetail(postScoreRefreshJobDetail);
        factoryBean.setName("postScoreRefreshTrigger");
        factoryBean.setGroup("communityTriggerGroup");
        factoryBean.setRepeatInterval(1000 * 60 * 5); //5分钟间隔执行一次任务（刷新帖子分数）
        factoryBean.setJobDataMap(new JobDataMap());
        return factoryBean;
    }

}
