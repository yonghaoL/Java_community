package com.learn.community.controller;

import com.alibaba.fastjson.JSONObject;
import com.learn.community.entity.Message;
import com.learn.community.entity.Page;
import com.learn.community.entity.User;
import com.learn.community.event.EventProducer;
import com.learn.community.service.MessageService;
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
import org.springframework.web.util.HtmlUtils;

import java.util.*;

@Controller
public class MessageController implements CommunityConstant {

    @Autowired
    private MessageService messageService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private UserService userService;

    @Autowired
    private EventProducer eventProducer;

    // 私信列表
    @RequestMapping(path = "/letter/list", method = RequestMethod.GET)
    public String getLetterList(Model model, Page page) { //page用于分页
//        Integer.valueOf("abcasdasdas");//测试出异常时错误页面能否正确返回
        User user = hostHolder.getUser();
        // 分页信息
        page.setLimit(5);
        page.setPath("/letter/list");
        page.setRows(messageService.findConversationCount(user.getId()));

        // 会话列表
        List<Message> conversationList = messageService.findConversations(
                user.getId(), page.getOffset(), page.getLimit());
        // 每个会话的未读条数和总条数
        List<Map<String, Object>> conversations = new ArrayList<>();
        if (conversationList != null) {
            for (Message message : conversationList) {
                Map<String, Object> map = new HashMap<>();
                map.put("conversation", message); //传入最新消息
                map.put("letterCount", messageService.findLetterCount(message.getConversationId())); //传入私信条数
                map.put("unreadCount", messageService.findLetterUnreadCount(user.getId(), message.getConversationId())); //传入未读数目
                int targetId = user.getId() == message.getFromId() ? message.getToId() : message.getFromId();
                map.put("target", userService.findUserById(targetId)); //传入私信的接收者

                conversations.add(map);
            }
        }
        model.addAttribute("conversations", conversations);

        // 查询未读消息数量（针对该用户，与会话id无关）
        int letterUnreadCount = messageService.findLetterUnreadCount(user.getId(), null);
        model.addAttribute("letterUnreadCount", letterUnreadCount);

        //查询未读通知数量，也显示在私信页面
        int noticeUnreadCount = messageService.findNoticeUnreadCount(user.getId(), null);
        model.addAttribute("noticeUnreadCount", noticeUnreadCount);

        return "/site/letter";
    }

    //显示会话中消息
    @RequestMapping(path = "/letter/detail/{conversationId}", method = RequestMethod.GET) //路径为会话id
    public String getLetterDetail(@PathVariable("conversationId") String conversationId, Page page, Model model) {
        // 分页信息
        page.setLimit(5);
        page.setPath("/letter/detail/" + conversationId);
        page.setRows(messageService.findLetterCount(conversationId));

        // 私信列表
        List<Message> letterList = messageService.findLetters(conversationId, page.getOffset(), page.getLimit());
        List<Map<String, Object>> letters = new ArrayList<>(); //除了显示会话中的消息，还要显示发信人的头像和名字，将这些数据都装入map
        if (letterList != null) {
            for (Message message : letterList) {
                Map<String, Object> map = new HashMap<>();
                map.put("letter", message);
                map.put("fromUser", userService.findUserById(message.getFromId()));
                letters.add(map);
            }
        }
        model.addAttribute("letters", letters);

        // 私信目标，需要将名字显示再页面上，显示"来自xxx的私信："
        model.addAttribute("target", getName(conversationId));

        // 将该会话的消息都设置已读
        List<Integer> ids = getLetterIds(letterList);
        if (!ids.isEmpty()) {
            messageService.readMessage(ids);
        }

        return "/site/letter-detail";
    }

    //封装一个私有方法
    private User getName(String conversationId) {
        String[] ids = conversationId.split("_");
        int id0 = Integer.parseInt(ids[0]);
        int id1 = Integer.parseInt(ids[1]);

        return hostHolder.getUser().getId() == id0 ? userService.findUserById(id1):userService.findUserById(id0);
    }

    private List<Integer> getLetterIds(List<Message> letterList) {
        List<Integer> ids = new ArrayList<>();

        if (letterList != null) {
            for (Message message : letterList) {
                //1.一定要确认该消息是对方发给自己的，不能把自己发给对方的设置已读了！
                //2.已读过的消息就不用重复更新了（减少对数据的写操作）
                if (hostHolder.getUser().getId() == message.getToId() && message.getStatus() == 0) {
                    ids.add(message.getId());
                }
            }
        }

        return ids;
    }

    @RequestMapping(path = "/letter/send", method = RequestMethod.POST)
    @ResponseBody //@ResponseBody注解表示该方法的返回的结果直接写入 HTTP 响应正文（ResponseBody）中，一般在异步获取数据时使用，通常是在使用 @RequestMapping 后，返回值通常解析为跳转路径，加上 @Responsebody 后返回结果不会被解析为跳转路径，而是直接写入HTTP 响应正文中。
    //因为我们此时只需要返回浏览器成功与否的JSON格式字符串就好了，由前端逻辑来刷新页面（增量式更新？），而不是后端返回一个html文件
    public String sendLetter(String toName, String content) {
        User target = userService.findUserByName(toName);
        if (target == null) {
            return CommunityUtil.getJSONString(1, "目标用户不存在!");
        }

        Message message = new Message();
        message.setFromId(hostHolder.getUser().getId());
        message.setToId(target.getId());
        if (message.getFromId() < message.getToId()) {
            message.setConversationId(message.getFromId() + "_" + message.getToId());
        } else {
            message.setConversationId(message.getToId() + "_" + message.getFromId());
        }
        message.setContent(content);
        message.setCreateTime(new Date());
        messageService.addMessage(message);

        return CommunityUtil.getJSONString(0);
    }

    //显示通知
    @RequestMapping(path = "/notice/list", method = RequestMethod.GET) //查询
    public String getNoticeList(Model model) { //返回网页
        User user = hostHolder.getUser();

        // 查询评论类通知
        Message message = messageService.findLatestNotice(user.getId(), TOPIC_COMMENT); //查询最新的评论通知
        Map<String, Object> messageVO = new HashMap<>();
        if (message != null) {
            messageVO.put("message", message);

            //因为我们在addMessage方法中将消息内容HtmlUtils.htmlEscape过，所以要把content内容中的JSON格式字符串中的转义字符等还原回去
            //为什么这里要还原？因为用户发的消息都是字符串文本，要去html标签化，而系统发的通知内容里面，我们是将一个map序列化成了JSON格式字符串，现在要把它反序列化为hashmap，
            //自然要进行去html标签化的逆过程，其实按道理还要进行sensitiveFilter.filter的逆过程，但是content的序列化字符串中几乎不会有敏感词，所以先不处理了
            String content = HtmlUtils.htmlUnescape(message.getContent());

            Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);

            messageVO.put("user", userService.findUserById((Integer) data.get("userId"))); //获取用户对象（该用户进行了某事件，系统通知现在登陆的用户）
            messageVO.put("entityType", data.get("entityType"));
            messageVO.put("entityId", data.get("entityId"));
            messageVO.put("postId", data.get("postId"));

            int count = messageService.findNoticeCount(user.getId(), TOPIC_COMMENT); //评论通知总数量
            messageVO.put("count", count);

            int unread = messageService.findNoticeUnreadCount(user.getId(), TOPIC_COMMENT); //未读评论的总数量
            messageVO.put("unread", unread);
        }
        model.addAttribute("commentNotice", messageVO); //传回给浏览器前端

        // 查询点赞类通知，与comment类似，不在赘述
        message = messageService.findLatestNotice(user.getId(), TOPIC_LIKE);
        messageVO = new HashMap<>();
        if (message != null) {
            messageVO.put("message", message);

            String content = HtmlUtils.htmlUnescape(message.getContent());
            Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);

            messageVO.put("user", userService.findUserById((Integer) data.get("userId")));
            messageVO.put("entityType", data.get("entityType"));
            messageVO.put("entityId", data.get("entityId"));
            messageVO.put("postId", data.get("postId"));

            int count = messageService.findNoticeCount(user.getId(), TOPIC_LIKE);
            messageVO.put("count", count);

            int unread = messageService.findNoticeUnreadCount(user.getId(), TOPIC_LIKE);
            messageVO.put("unread", unread);
        }
        model.addAttribute("likeNotice", messageVO);

        // 查询关注类通知，与comment类似，不在赘述
        message = messageService.findLatestNotice(user.getId(), TOPIC_FOLLOW);
        messageVO = new HashMap<>();
        if (message != null) {
            messageVO.put("message", message);

            String content = HtmlUtils.htmlUnescape(message.getContent());
            Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);

            messageVO.put("user", userService.findUserById((Integer) data.get("userId")));
            messageVO.put("entityType", data.get("entityType"));
            messageVO.put("entityId", data.get("entityId"));
            //不需要帖子id，因为某人关注了我们，我们应该链接到它的主页而不是帖子详情

            int count = messageService.findNoticeCount(user.getId(), TOPIC_FOLLOW);
            messageVO.put("count", count);

            int unread = messageService.findNoticeUnreadCount(user.getId(), TOPIC_FOLLOW);
            messageVO.put("unread", unread);
        }
        model.addAttribute("followNotice", messageVO);

        // 查询未读消息总的数量（其实应该也可以把前面三者加起来）
        int letterUnreadCount = messageService.findLetterUnreadCount(user.getId(), null); //conversationId为null，表示查询所有通知的数量
        model.addAttribute("letterUnreadCount", letterUnreadCount);
        int noticeUnreadCount = messageService.findNoticeUnreadCount(user.getId(), null);
        model.addAttribute("noticeUnreadCount", noticeUnreadCount);

        return "/site/notice";
    }

    //显示系统通知的详情
    @RequestMapping(path = "/notice/detail/{topic}", method = RequestMethod.GET)
    public String getNoticeDetail(@PathVariable("topic") String topic, Page page, Model model) { //得到路径中的topic参数
        User user = hostHolder.getUser();

        //照例进行分页处理
        page.setLimit(5);
        page.setPath("/notice/detail/" + topic);
        page.setRows(messageService.findNoticeCount(user.getId(), topic)); //通知的总行数

        List<Message> noticeList = messageService.findNotices(user.getId(), topic, page.getOffset(), page.getLimit());
        List<Map<String, Object>> noticeVoList = new ArrayList<>(); //定义返回前端的hashmap集合集合
        if (noticeList != null) {
            for (Message notice : noticeList) {
                Map<String, Object> map = new HashMap<>();
                // 通知
                map.put("notice", notice);
                // 内容
                //因为我们在addMessage方法中将消息内容HtmlUtils.htmlEscape过，所以要把content内容中的JSON格式字符串中的转义字符等还原回去
                //为什么这里要还原？因为用户发的消息都是字符串文本，要去html标签化，而系统发的通知内容里面，我们是将一个map序列化成了JSON格式字符串，现在要把它反序列化为hashmap，
                //自然要进行去html标签化的逆过程，其实按道理还要进行sensitiveFilter.filter的逆过程，但是content的序列化字符串中几乎不会有敏感词，所以先不处理了
                String content = HtmlUtils.htmlUnescape(notice.getContent());
                Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);
                map.put("user", userService.findUserById((Integer) data.get("userId")));
                map.put("entityType", data.get("entityType"));
                map.put("entityId", data.get("entityId"));
                map.put("postId", data.get("postId"));
                // 查一下系统用户id，添加系统用户
                map.put("fromUser", userService.findUserById(notice.getFromId()));

                noticeVoList.add(map);
            }
        }
        model.addAttribute("notices", noticeVoList);

        // 设置已读
        List<Integer> ids = getLetterIds(noticeList);
        if (!ids.isEmpty()) {
            messageService.readMessage(ids);//复用以前的方法将系统通知设置为已读
        }

        return "/site/notice-detail";
    }

}
