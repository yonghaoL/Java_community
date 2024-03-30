package com.learn.community.controller;

import com.learn.community.entity.DiscussPost;
import com.learn.community.entity.Page;
import com.learn.community.entity.User;
import com.learn.community.service.DiscussPostService;
import com.learn.community.service.LikeService;
import com.learn.community.service.UserService;
import com.learn.community.util.CommunityConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Controller
public class HomeCotroller implements CommunityConstant {
    @Autowired //将Service注入进来，调用服务，服务再访问数据库存取数据
    private DiscussPostService discussPostService;
    @Autowired
    private UserService userService;
    @Autowired
    private LikeService likeService;

    @RequestMapping(path = "/index", method = RequestMethod.GET)//前面是路径，后面表示只处理GET请求而不处理其他请求，index代表首页
    //注意响应返回的是网页视图的名字路径（String类型），就不要加@ResponseBody注解了
    //也可以返回ModelAndView类型对象
    public String getIndexPage(Model model, Page page){ // model可以接收模板/index.html中的变量所需要的数据
        // 方法调用前,SpringMVC会自动实例化Model和Page,并将Page注入Model.
        // 所以,在thymeleaf中可以直接访问Page对象中的数据. 且本方法返回model时不用将page add到 model中
        page.setRows(discussPostService.findDiscussPostRows(0));
        page.setPath("/index");//该链接属性在index.html模板文件中用到，要复用该链接以生成点击下一页或者上一页的跳转链接

        List<DiscussPost> discussPostsNoUser = discussPostService.findDiscussPosts(0, page.getOffset(), page.getLimit());

        List<HashMap<String, Object>> discussPosts = new ArrayList<>();
        if(discussPostsNoUser!=null) {
            for (DiscussPost discussPost : discussPostsNoUser) {
                User user = userService.findUserById(discussPost.getUserId());
                //将用户数据取出来和帖子数组放在hashmap中，一起存进list
                HashMap<String, Object> map = new HashMap<>();
                map.put("post", discussPost);
                map.put("user", user);

                //查询该帖子点赞数量并放进hashmap中
                long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, discussPost.getId());
                map.put("likeCount", likeCount);
                discussPosts.add(map);
            }
        }
        model.addAttribute("discussPosts", discussPosts);
        return "/index";//默认父目录为templates,/index全称/index.html，只是参数里面不用写
    }

    //写一个访问500错误（即服务器出错的错误页面展示请求），便于我们在服务器中出错时重定向到该页面
    @RequestMapping(path = "/error", method = RequestMethod.GET)
    public String getErrorPage() {
        return "/error/500";
    }
}
