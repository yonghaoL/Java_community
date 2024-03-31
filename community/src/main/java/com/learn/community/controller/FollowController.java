package com.learn.community.controller;

import com.learn.community.entity.Event;
import com.learn.community.entity.Page;
import com.learn.community.entity.User;
import com.learn.community.event.EventProducer;
import com.learn.community.service.FollowService;
import com.learn.community.service.UserService;
import com.learn.community.util.CommunityConstant;
import com.learn.community.util.CommunityUtil;
import com.learn.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

@Controller
public class FollowController implements CommunityConstant {

    @Autowired
    private FollowService followService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private UserService userService;

    @Autowired
    private EventProducer eventProducer;

    //异步的post请求
    @RequestMapping(path = "/follow", method = RequestMethod.POST)
    @ResponseBody
    public String follow(int entityType, int entityId) { //关注时只需要传实体的类型和id即可，用户肯定是当前用户
        User user = hostHolder.getUser();

        followService.follow(user.getId(), entityType, entityId); //运行关注 操作

        // 触发关注事件
        Event event = new Event()
                .setTopic(TOPIC_FOLLOW)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(entityType)
                .setEntityId(entityId)
                .setEntityUserId(entityId);
        eventProducer.fireEvent(event);

        return CommunityUtil.getJSONString(0, "已关注!"); //返回浏览器信息
    }

    //取消关注
    @RequestMapping(path = "/unfollow", method = RequestMethod.POST)
    @ResponseBody
    public String unfollow(int entityType, int entityId) {
        User user = hostHolder.getUser();

        followService.unfollow(user.getId(), entityType, entityId);

        return CommunityUtil.getJSONString(0, "已取消关注!");
    }

    //get方法，获取关注的用户列表（todo：获取关注的实体列表）
    @RequestMapping(path = "/followees/{userId}", method = RequestMethod.GET)
    public String getFollowees(@PathVariable("userId") int userId, Page page, Model model) { //需要得知用户的id（注意不是此时登陆的用户），需要page分页
        User user = userService.findUserById(userId);
        if (user == null) {
            throw new RuntimeException("该用户不存在!"); //注意我们已经统一处理了异常
        }
        model.addAttribute("user", user); //为了显示当前我们查询的关注列表的拥有者名字等

        page.setLimit(5);
        page.setPath("/followees/" + userId); //翻页所需要用到的的路径
        page.setRows((int) followService.findFolloweeCount(userId, ENTITY_TYPE_USER)); //查询关注的用户数目

        List<Map<String, Object>> userList = followService.findFollowees(userId, page.getOffset(), page.getLimit());
        if (userList != null) {
            for (Map<String, Object> map : userList) {
                User u = (User) map.get("user"); //注意取出的是Object类型，需要强转为User类型
                map.put("hasFollowed", hasFollowed(u.getId())); //一一查询这些用户是否被当前登陆的用户关注了，并将结果加入userList中的map中，然后返回给前端以显示按钮
            }
        }
        model.addAttribute("users", userList);

        return "/site/followee";
    }

    //与上面的操作基本一样，不再赘述
    @RequestMapping(path = "/followers/{userId}", method = RequestMethod.GET)
    public String getFollowers(@PathVariable("userId") int userId, Page page, Model model) {
        User user = userService.findUserById(userId);
        if (user == null) {
            throw new RuntimeException("该用户不存在!");
        }
        model.addAttribute("user", user);

        page.setLimit(5);
        page.setPath("/followers/" + userId);
        page.setRows((int) followService.findFollowerCount(ENTITY_TYPE_USER, userId));

        List<Map<String, Object>> userList = followService.findFollowers(userId, page.getOffset(), page.getLimit());
        if (userList != null) {
            for (Map<String, Object> map : userList) {
                User u = (User) map.get("user");
                map.put("hasFollowed", hasFollowed(u.getId()));
            }
        }
        model.addAttribute("users", userList);

        return "/site/follower";
    }

    //查询当前用户是否关注了传入的用户id
    private boolean hasFollowed(int userId) {
        if (hostHolder.getUser() == null) { //未登录则直接显示未关注即可
            return false;
        }

        //查询当前用户是否关注了传入的用户id
        return followService.hasFollowed(hostHolder.getUser().getId(), ENTITY_TYPE_USER, userId);
    }

}
