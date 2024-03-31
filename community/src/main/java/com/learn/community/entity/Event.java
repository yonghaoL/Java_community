package com.learn.community.entity;

import java.util.HashMap;
import java.util.Map;

public class Event {

    private String topic;
    private int userId; //事件的触发着
    private int entityType; //事件发生在什么类型上（帖子？评论？）
    private int entityId; //事件id
    private int entityUserId; //事件发布者的id
    private Map<String, Object> data = new HashMap<>(); //以应对将来可能的功能扩展，定义一个map，除了存消息内容外，还可以存其他东西

    public String getTopic() {
        return topic;
    }

    public Event setTopic(String topic) { //set方法一律返回当前对象，可以实现链式编程set.(xxx).set(xxx).set(xxx)
        this.topic = topic;
        return this;
    }

    public int getUserId() {
        return userId;
    }

    public Event setUserId(int userId) {
        this.userId = userId;
        return this;
    }

    public int getEntityType() {
        return entityType;
    }

    public Event setEntityType(int entityType) {
        this.entityType = entityType;
        return this;
    }

    public int getEntityId() {
        return entityId;
    }

    public Event setEntityId(int entityId) {
        this.entityId = entityId;
        return this;
    }

    public int getEntityUserId() {
        return entityUserId;
    }

    public Event setEntityUserId(int entityUserId) {
        this.entityUserId = entityUserId;
        return this;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public Event setData(String key, Object value) {
        this.data.put(key, value);
        return this;
    }

}
