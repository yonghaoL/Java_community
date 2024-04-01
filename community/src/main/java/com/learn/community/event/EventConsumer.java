package com.learn.community.event;

import com.alibaba.fastjson.JSONObject;
import com.learn.community.entity.DiscussPost;
import com.learn.community.entity.Event;
import com.learn.community.entity.Message;
import com.learn.community.service.DiscussPostService;
//import com.learn.community.service.ElasticsearchService;
import com.learn.community.service.ElasticsearchService;
import com.learn.community.service.MessageService;
import com.learn.community.util.CommunityConstant;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class EventConsumer implements CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(EventConsumer.class); //记日志，记录错误异常

    @Autowired
    private MessageService messageService;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private ElasticsearchService elasticsearchService;

    @KafkaListener(topics = {TOPIC_COMMENT, TOPIC_LIKE, TOPIC_FOLLOW}) //该消费者监听三个主题（被动的，不用我们主动调，它会一直监听）
    public void handleCommentMessage(ConsumerRecord record) {
        if (record == null || record.value() == null) { //空消息
            logger.error("消息的内容为空!");
            return;
        }

        Event event = JSONObject.parseObject(record.value().toString(), Event.class); //将消息字符串还原为event对象
        if (event == null) { //消息格式不对
            logger.error("消息格式错误!");
            return;
        }

        // 发送站内通知
        Message message = new Message();
        message.setFromId(SYSTEM_USER_ID); //假设后台系统为用户1，该消息是系统通知
        message.setToId(event.getEntityUserId()); //要通知的是发布帖子或者发布评论的人
        message.setConversationId(event.getTopic()); //系统消息的发送者一定是用户1，所有没必要存会话id，我们拿这一列存主题topic
        message.setCreateTime(new Date());

        Map<String, Object> content = new HashMap<>(); //消息内容以及不方便存的字段统统存到content中，content是hashmap
        content.put("userId", event.getUserId());
        content.put("entityType", event.getEntityType());
        content.put("entityId", event.getEntityId());

        if (!event.getData().isEmpty()) {
            for (Map.Entry<String, Object> entry : event.getData().entrySet()) {
                content.put(entry.getKey(), entry.getValue()); //将event的data中的map内容存进content
            }
        }

        message.setContent(JSONObject.toJSONString(content)); //将content转换为JSON格式字符串
        messageService.addMessage(message);
    }

    // 消费发帖事件
    @KafkaListener(topics = {TOPIC_PUBLISH}) //监听的是发帖主题
    public void handlePublishMessage(ConsumerRecord record) { //record为消息
        if (record == null || record.value() == null) {
            logger.error("消息的内容为空!");
            return;
        }

        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event == null) {
            logger.error("消息格式错误!");
            return;
        }

        DiscussPost post = discussPostService.findDiscussPostById(event.getEntityId()); //从数据库里面拿出该帖子存到ES服务器里
        elasticsearchService.saveDiscussPost(post);
    }

}
