package com.learn.community;

import com.learn.community.entity.DiscussPost;
import com.learn.community.service.DiscussPostService;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class SpringBootTests {

    @Autowired
    private DiscussPostService discussPostService;

    private DiscussPost data;

    @BeforeClass //该注解修饰的方法在类初始化之前执行
    public static void beforeClass() {
        System.out.println("beforeClass");
    }

    @AfterClass //类销毁时执行
    public static void afterClass() {
        System.out.println("afterClass");
    }

    @Before //在调用的方法执行前，先执行这个方法（可以用来初始化测试数据）
    public void before() {
        System.out.println("before");

//        // 初始化测试数据
//        data = new DiscussPost();
//        data.setUserId(111);
//        data.setTitle("Test Title");
//        data.setContent("Test Content");
//        data.setCreateTime(new Date());
//        discussPostService.addDiscussPost(data);
    }

    @After //在调用的方法执行后，执行这个方法（可以用来删除测试数据）
    public void after() {
        System.out.println("after");

//        // 删除测试数据
//        discussPostService.updateStatus(data.getId(), 2);
    }

    @Test
    public void test1() {
        System.out.println("test1");
    }

    @Test
    public void test2() {
        System.out.println("test2");
    }

    @Test
    public void testFindById() {
        DiscussPost post = discussPostService.findDiscussPostById(data.getId());
        Assert.assertNotNull(post); //断言判断是否非空
        Assert.assertEquals(data.getTitle(), post.getTitle()); //断言判断title是否相等
        Assert.assertEquals(data.getContent(), post.getContent()); //断言判断content是否相等
    }

    @Test
    public void testUpdateScore() {
        int rows = discussPostService.updateScore(data.getId(), 2000.00);
        Assert.assertEquals(1, rows);

        DiscussPost post = discussPostService.findDiscussPostById(data.getId());
        Assert.assertEquals(2000.00, post.getScore(), 2); //比较小数时需要设置精度delta，这里设置为2
    }

}
