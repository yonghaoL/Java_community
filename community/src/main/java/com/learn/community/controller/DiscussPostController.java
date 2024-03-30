package com.learn.community.controller;

//import com.learn.community.entity.Comment;
import com.learn.community.annotation.LoginRequired;
import com.learn.community.entity.Comment;
import com.learn.community.entity.DiscussPost;
import com.learn.community.entity.Page;
import com.learn.community.entity.User;
//import com.learn.community.service.CommentService;
import com.learn.community.service.CommentService;
import com.learn.community.service.DiscussPostService;
//import com.learn.community.service.LikeService;
import com.learn.community.service.LikeService;
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

import java.util.*;

@Controller
@RequestMapping("/discuss")
public class DiscussPostController implements CommunityConstant {

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private UserService userService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private LikeService likeService;

    //提交帖子请求，这里需要增量式处理，即ajax，具体获取数据和重定向的操作在index.js文件中
    @RequestMapping(path = "/add", method = RequestMethod.POST)
    @ResponseBody
//    @LoginRequired //该方法只有登陆的用户才能访问
    public String addDiscussPost(String title, String content) {
        User user = hostHolder.getUser(); //获取当前登陆的用户
        //能否直接用@LoginRequired注解,试了下暂时不可以，因为拦截器中是重定向返回了页面，而本方法是异步请求，需要服务器返回JSON数据，可以
        //->在拦截器中写CommunityUtil.getJSONString(403, "你还没有登录哦!")，为了方便就直接在这个方法中写了，也就不用@LoginRequired了
        if (user == null) {
            return CommunityUtil.getJSONString(403, "你还没有登录哦!");
        }

        DiscussPost post = new DiscussPost();
        post.setUserId(user.getId());
        post.setTitle(title);
        post.setContent(content);
        post.setCreateTime(new Date());
        discussPostService.addDiscussPost(post);

        // 报错的情况,将来统一处理.
        return CommunityUtil.getJSONString(0, "发布成功!");
    }

    @RequestMapping(path = "/detail/{discussPostId}", method = RequestMethod.GET)
    public String getDiscussPost(@PathVariable("discussPostId") int discussPostId, Model model, Page page) {
        DiscussPost discussPost = discussPostService.findDiscussPostById(discussPostId);
        model.addAttribute("post", discussPost);

        //获取作者名字和头像
        int userId = discussPost.getUserId();
        User user = userService.findUserById(userId);
        model.addAttribute("user", user);

        // 帖子的点赞数量
        long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, discussPostId);
        model.addAttribute("likeCount", likeCount);
        // 帖子的当前用户的对它的点赞状态（没登录则肯定状态为未点赞）
        int likeStatus = hostHolder.getUser() == null ? 0 :
                likeService.findEntityLikeStatus(hostHolder.getUser().getId(), ENTITY_TYPE_POST, discussPostId);
        model.addAttribute("likeStatus", likeStatus);

        //评论分页信息
        page.setLimit(5);
        page.setPath("/discuss/detail/" + discussPostId);
        page.setRows(discussPost.getCommentCount()); //从帖子对象里取出评论的数量（冗余存储，这样效率高）
        //也可以通过 commentService.findCommentCount(ENTITY_TYPE_POST, discussPost.getId()); 表示拿出所有属于该post的评论

        //该帖子的评论列表
        List<Comment> commentList = commentService.findCommentsByEntity(
                ENTITY_TYPE_POST, discussPost.getId(), page.getOffset(), page.getLimit());
        List<Map<String, Object>> commentVoList = new ArrayList<>();
        //评论中需要显示作者姓名和头像等，统一存到map中去
        if (commentList != null) {
            for (Comment comment : commentList) {
                // 评论显示对象列表
                Map<String, Object> commentVo = new HashMap<>();
                // 评论
                commentVo.put("comment", comment);
                // 作者
                commentVo.put("user", userService.findUserById(comment.getUserId()));
                // 当前评论的点赞数量
                likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_COMMENT, comment.getId());
                commentVo.put("likeCount", likeCount);
                // 当前评论的点赞状态
                likeStatus = hostHolder.getUser() == null ? 0 :
                        likeService.findEntityLikeStatus(hostHolder.getUser().getId(), ENTITY_TYPE_COMMENT, comment.getId());
                commentVo.put("likeStatus", likeStatus);

                // 评论的回复列表
                List<Comment> replyList = commentService.findCommentsByEntity(
                        ENTITY_TYPE_COMMENT, comment.getId(), 0, Integer.MAX_VALUE); // 表示拿出所有属于该评论的reply
                // 回复VO列表
                List<Map<String, Object>> replyVoList = new ArrayList<>();
                if (replyList != null) {
                    for (Comment reply : replyList) {
                        Map<String, Object> replyVo = new HashMap<>();
                        // 回复
                        replyVo.put("reply", reply);
                        // 作者
                        replyVo.put("user", userService.findUserById(reply.getUserId()));
                        // 回复的目标，即评论下的回复针对的人
                        User target = reply.getTargetId() == 0 ? null : userService.findUserById(reply.getTargetId());
                        replyVo.put("target", target);
                        // 当前回复的点赞数量
                        likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_COMMENT, reply.getId());
                        replyVo.put("likeCount", likeCount);
                        // 当前回复的点赞状态
                        likeStatus = hostHolder.getUser() == null ? 0 :
                                likeService.findEntityLikeStatus(hostHolder.getUser().getId(), ENTITY_TYPE_COMMENT, reply.getId());
                        replyVo.put("likeStatus", likeStatus);

                        replyVoList.add(replyVo);
                    }
                }
                commentVo.put("replys", replyVoList);

                // 回复数量
                int replyCount = commentService.findCommentCount(ENTITY_TYPE_COMMENT, comment.getId()); // 表示查询属于该comment的reply条数
                commentVo.put("replyCount", replyCount);

                commentVoList.add(commentVo);
            }
        }
        model.addAttribute("comments", commentVoList);
        return "/site/discuss-detail"; //成功发帖后到帖子页面
    }

}
