package com.learn.community;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.BoundValueOperations;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.TimeUnit;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class RedisTests {

    @Autowired
    private RedisTemplate redisTemplate; //注入

    //存一个value值
    @Test
    public void testStrings() {
        String redisKey = "test:count";

        redisTemplate.opsForValue().set(redisKey, 1); //存入键值对

        System.out.println(redisTemplate.opsForValue().get(redisKey)); //由key获取value
        System.out.println(redisTemplate.opsForValue().increment(redisKey)); //该值++
        System.out.println(redisTemplate.opsForValue().decrement(redisKey)); //该值--
    }

    //存一个hash表
    @Test
    public void testHashes() {
        String redisKey = "test:user";

        redisTemplate.opsForHash().put(redisKey, "id", 1); // ("id", 1)表示一个hash型数据，它的hashkey是id，hashvalue是1
        redisTemplate.opsForHash().put(redisKey, "username", "zhangsan");

        System.out.println(redisTemplate.opsForHash().get(redisKey, "id"));
        System.out.println(redisTemplate.opsForHash().get(redisKey, "username"));
    }

    //存放一个list
    @Test
    public void testLists() {
        String redisKey = "test:ids";

        //放入101，102，103三个数据，注意后插的索引为0，先插的索引大
        redisTemplate.opsForList().leftPush(redisKey, 101);
        redisTemplate.opsForList().leftPush(redisKey, 102);
        redisTemplate.opsForList().leftPush(redisKey, 103);

        System.out.println(redisTemplate.opsForList().size(redisKey));
        System.out.println(redisTemplate.opsForList().index(redisKey, 0)); //获取索引0的数据（结果为103）
        System.out.println(redisTemplate.opsForList().range(redisKey, 0, 2)); //获取0-2的数据（103，102，101）

        System.out.println(redisTemplate.opsForList().leftPop(redisKey)); //弹出左边的数据（弹出103）
        System.out.println(redisTemplate.opsForList().leftPop(redisKey)); //弹出左边的数据（弹出102）
        System.out.println(redisTemplate.opsForList().leftPop(redisKey)); //弹出左边的数据（弹出101）
    }

    //存放一个set数据（无序的，不可重复的）
    @Test
    public void testSets() {
        String redisKey = "test:teachers";

        redisTemplate.opsForSet().add(redisKey, "刘备", "关羽", "张飞", "赵云", "诸葛亮");

        System.out.println(redisTemplate.opsForSet().size(redisKey));
        System.out.println(redisTemplate.opsForSet().pop(redisKey)); //随机pop一个数据
        System.out.println(redisTemplate.opsForSet().members(redisKey));
    }

    //存放一个有序set（按存入时的分数排序由小到大）
    @Test
    public void testSortedSets() {
        String redisKey = "test:students";

        redisTemplate.opsForZSet().add(redisKey, "唐僧", 80);
        redisTemplate.opsForZSet().add(redisKey, "悟空", 90);
        redisTemplate.opsForZSet().add(redisKey, "八戒", 50);
        redisTemplate.opsForZSet().add(redisKey, "沙僧", 70);
        redisTemplate.opsForZSet().add(redisKey, "白龙马", 60);

        System.out.println(redisTemplate.opsForZSet().zCard(redisKey));
        System.out.println(redisTemplate.opsForZSet().score(redisKey, "八戒")); //返回该数据分数
        System.out.println(redisTemplate.opsForZSet().reverseRank(redisKey, "八戒")); //返回该数据的排名（反转即由大到小拍）
        System.out.println(redisTemplate.opsForZSet().reverseRange(redisKey, 0, 2));//返回由大到小排序的前三名
    }

    @Test
    public void testKeys() {
        redisTemplate.delete("test:user"); //删除了"test:user"这一key对应的键值数据

        System.out.println(redisTemplate.hasKey("test:user"));

        redisTemplate.expire("test:students", 10, TimeUnit.SECONDS); //设置"test:students"这一key对应的所有键值数据的自动删除倒计时
    }

    // 多次访问同一个key时，先把该key传进去redisTemplate再开始访问
    @Test
    public void testBoundOperations() {
        String redisKey = "test:count";
        BoundValueOperations operations = redisTemplate.boundValueOps(redisKey); //说明我们要多次访问test:count这个key并进行操作
        operations.increment();
        operations.increment();
        operations.increment();
        operations.increment();
        operations.increment();
        //执行了5次+操作
        System.out.println(operations.get());
    }

    // 编程式事务，注意它的事务不完全满足ACID四个特性
    @Test
    public void testTransactional() {
        //编程式事务，实现该事务接口（与mysql类似）
        Object obj = redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException { //传入操作对象operations
                String redisKey = "test:tx";

                operations.multi();//启动事务（开始添加操作）

                operations.opsForSet().add(redisKey, "zhangsan");
                operations.opsForSet().add(redisKey, "lisi");
                operations.opsForSet().add(redisKey, "wangwu");

                System.out.println(operations.opsForSet().members(redisKey)); //注意该查询无效！sout会输出空列表（啥也没有）

                return operations.exec(); //提交事务并返回该操作的返回值
            }
        });
        System.out.println(obj);//输出发现返回的是 每个操作的返回值列表： [1,1,1,[zhangsan,lisi,wangwu]]  ,返回的1表示向集合内插入了一个(行)数据
    }

}
