package com.learn.community.service;

import com.learn.community.dao.UserMapper;
import com.learn.community.entity.User;
import com.learn.community.util.CommunityUtil;
import com.learn.community.util.MailClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class UserService {
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MailClient mailClient;

    @Autowired
    private TemplateEngine templateEngine;

    @Value("${community.path.domain}") //@Value用于注入值(这里注入的是配置文件中的值)，@Autowired用于注入bean
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    public User findUserById(int id) {
        return userMapper.selectById(id);
    }

    public Map<String, Object> register(User user) {
        Map<String, Object> map = new HashMap<>(); //返回值，返回给LoginCotroller？

        // 空值处理
        if (user == null) {
            throw new IllegalArgumentException("参数不能为空!");
        }
        if (StringUtils.isBlank(user.getUsername())) {
            map.put("usernameMsg", "账号不能为空!");
            return map;
        }
        if (StringUtils.isBlank(user.getPassword())) {
            map.put("passwordMsg", "密码不能为空!");
            return map;
        }
        if (StringUtils.isBlank(user.getEmail())) {
            map.put("emailMsg", "邮箱不能为空!");
            return map;
        }

        // 验证账号，去数据库里查找是否已存在
        User u = userMapper.selectByName(user.getUsername());
        if (u != null) {
            map.put("usernameMsg", "该账号已存在!");
            return map;
        }

        // 验证邮箱，去数据库里查找是否已存在
        u = userMapper.selectByEmail(user.getEmail());
        if (u != null) {
            map.put("emailMsg", "该邮箱已被注册!");
            return map;
        }

        // 注册用户
        user.setSalt(CommunityUtil.generateUUID().substring(0, 5)); //生成5位的数据库salt字段
        user.setPassword(CommunityUtil.md5(user.getPassword() + user.getSalt())); //加密密码存到数据库中！！！注意不存原始密码
        user.setType(0);
        user.setStatus(0);
        user.setActivationCode(CommunityUtil.generateUUID()); //生成激活码，也是用util中写的的随机字符串方法
        user.setHeaderUrl(String.format("http://images.nowcoder.com/head/%dt.png", new Random().nextInt(1000))); //随机分配头像
        //http://images.nowcoder.com/head 存了1000个图片，%dt.png表示第%d个图片，该参数为后面的值
//        user.setHeaderUrl("http://images.nowcoder.com/head/" + new Random().nextInt(1000) + "t.png");//可以这样写吗？

        user.setCreateTime(new Date());//账户创建时间
        userMapper.insertUser(user); //调用userMapper向数据库中插入用户，此过程中自动生成主键id（配置文件中有写）

        // 激活邮件发送
        Context context = new Context();
        context.setVariable("email", user.getEmail());
        // http://localhost:8080/community/activation/101/code
        String url = domain + contextPath + "/activation/" + user.getId() + "/" + user.getActivationCode();
        context.setVariable("url", url);
        String content = templateEngine.process("/mail/activation", context); //生成了一个动态模板构造的网页作为邮件主题内容（字符串格式），该模板在templates下，会自动扫描到
        mailClient.sendMail(user.getEmail(), "激活账号", content);

        return map;
    }
}


