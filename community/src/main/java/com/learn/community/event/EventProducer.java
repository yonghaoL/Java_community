package com.learn.community.event;

import com.alibaba.fastjson.JSONObject;
import com.learn.community.entity.Event;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component //交由容器管理
public class EventProducer {

    @Autowired
    private KafkaTemplate kafkaTemplate;

    // 处理事件（发消息）
    public void fireEvent(Event event) {
        // 将事件发布到指定的主题
        kafkaTemplate.send(event.getTopic(), JSONObject.toJSONString(event)); //内容要包含event所有信息，故直接传送JSON格式字符串，消费者接收后再还原为event对象
    }

}
