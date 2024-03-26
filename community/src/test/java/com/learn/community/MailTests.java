package com.learn.community;

import com.learn.community.util.MailClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class MailTests {

    @Autowired
    private MailClient mailClient;

    @Autowired
    private TemplateEngine templateEngine; //模板引擎类，我们用它来调用要发送的html文件

    @Test
    public void testTextMail() {
        mailClient.sendMail("1053903176@qq.com", "TEST", "Welcome.");
    }

    @Test
    public void testHtmlMail() {
        //thymeleaf模板引擎下的context类
        Context context = new Context();
        context.setVariable("username", "sunday");

        String content = templateEngine.process("/mail/demo", context); //生成了一个动态模板构造的网页作为邮件主题内容（字符串格式），该模板在templates下，会自动扫描到
        System.out.println(content);

        mailClient.sendMail("1053903176@qq.com", "HTML", content);
    }

}
