package com.learn.community.service;

import com.learn.community.dao.LoginTicketMapper;
import com.learn.community.dao.UserMapper;
import com.learn.community.entity.LoginTicket;
import com.learn.community.entity.User;
import com.learn.community.util.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class UserService implements CommunityConstant {
    @Autowired
    private UserMapper userMapper; //注入mapper用于操作数据库

    @Autowired
    private MailClient mailClient;

    @Autowired
    private TemplateEngine templateEngine;

//    @Autowired
//    private LoginTicketMapper loginTicketMapper;

    @Autowired
    private RedisTemplate redisTemplate; //用redis来存ticket而不是mysql

    @Value("${community.path.domain}") //@Value用于注入值(这里注入的是配置文件中的值)，@Autowired用于注入bean
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    public User findUserById(int id) {
        //先从redis缓存里取
        String redisKey = RedisKeyUtil.getUserKey(id);
        User user = (User) redisTemplate.opsForValue().get(redisKey);
        if(user == null){ // 若为空，去数据库里面取并将该user存进redis
            user = userMapper.selectById(id);
            redisTemplate.opsForValue().set(redisKey, user, 600, TimeUnit.SECONDS);//在缓存里放10分钟
        }
        return user;
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

    //激活码验证
    public int activation(int userId, String code) {
        User user = userMapper.selectById(userId);
        if (user.getStatus() == 1) { //说明已经激活了
            return ACTIVATION_REPEAT;
        } else if (user.getActivationCode().equals(code)) { //激活成功
            clearCache(userId);
            userMapper.updateStatus(userId, 1);
            return ACTIVATION_SUCCESS;
        } else {
            return ACTIVATION_FAILURE;
        }
    }

    //登录账号
    public Map<String, Object> login(String username, String password, int expiredSeconds) { //expiredSeconds是登录凭证过期时间
        Map<String, Object> map = new HashMap<>();

        // 空值处理
        if (StringUtils.isBlank(username)) {
            map.put("usernameMsg", "账号不能为空!");
            return map; //返回错误信息给map，map传给html模板model，然后写入html返回给用户看到
        }
        if (StringUtils.isBlank(password)) {
            map.put("passwordMsg", "密码不能为空!");
            return map;
        }

        // 验证账号
        User user = userMapper.selectByName(username);
        if (user == null) {
            map.put("usernameMsg", "该账号不存在!");
            return map;
        }

        // 验证状态
        if (user.getStatus() == 0) {
            map.put("usernameMsg", "该账号未激活!");
            return map;
        }

        // 验证密码
        password = CommunityUtil.md5(password + user.getSalt()); //服务器存的是加密后的密码，要加密后对比
        if (!user.getPassword().equals(password)) {
            map.put("passwordMsg", "密码不正确!");
            return map;
        }

        // 生成登录凭证
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(user.getId());
        loginTicket.setTicket(CommunityUtil.generateUUID()); //登录凭证的查询key是一个字符串，这个字符串也会传回给浏览器，下次登录时浏览器发送该key给服务器，服务器直接验证这个字符串就行
        loginTicket.setStatus(0); //设置状态为0
        loginTicket.setExpired(new Date(System.currentTimeMillis() + expiredSeconds * 1000)); //设置登录凭证到期时间
//        loginTicketMapper.insertLoginTicket(loginTicket);

        String redisKey = RedisKeyUtil.getTicketKey(loginTicket.getTicket()); //将ticket值作为key的后面一部分
        redisTemplate.opsForValue().set(redisKey, loginTicket); //存入ticket,redis会把ticket序列化为JSON格式字符串,取出来的时候是Object对象可以强转回loginTicket

        map.put("ticket", loginTicket.getTicket()); //成功后把该ticket返回
        return map;
    }

    public void logout(String ticket) {
//        loginTicketMapper.updateStatus(ticket, 1);
        String redisKey = RedisKeyUtil.getTicketKey(ticket);
        LoginTicket loginTicket = (LoginTicket) redisTemplate.opsForValue().get(redisKey);
        loginTicket.setStatus(1);
        redisTemplate.opsForValue().set(redisKey, loginTicket);
    }

    public LoginTicket findLoginTicket(String ticket) {
//        return loginTicketMapper.selectByTicket(ticket);
        String redisKey = RedisKeyUtil.getTicketKey(ticket);
        return (LoginTicket) redisTemplate.opsForValue().get(redisKey);
    }

    public int updateHeader(int userId, String headerUrl) {
        clearCache(userId);
        return userMapper.updateHeader(userId, headerUrl);
    }

    public int updatePassword(int userId, String password) {
        clearCache(userId);
        return userMapper.updatePassword(userId, password);
    }

    public User findUserByName(String toName){
        return userMapper.selectByName(toName);
    };

    //当数据变更时我们清理缓存（简化实现，事实上应该更新缓存）
    private void clearCache(int userId) {
        String redisKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.delete(redisKey);
    }
}


