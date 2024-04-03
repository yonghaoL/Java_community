package com.learn.community.util;

//用于生成redis里面的key
public class RedisKeyUtil {

    private static final String SPLIT = ":"; //redis中用冒号来分割key
    private static final String PREFIX_ENTITY_LIKE = "like:entity"; //实体的赞的key前缀
    private static final String PREFIX_USER_LIKE = "like:user"; //某个用户收到的赞（他发布的评论帖子以及回复等等）
    private static final String PREFIX_FOLLOWEE = "followee"; //某实体关注的实体
    private static final String PREFIX_FOLLOWER = "follower"; //某实体的粉丝（关注他的人）
    private static final String PREFIX_KAPTCHA = "kaptcha";
    private static final String PREFIX_TICKET = "ticket"; //redis来存登录凭证ticket
    private static final String PREFIX_USER = "user";
    private static final String PREFIX_UV = "uv"; //网站活跃量，独立访问者统计
    private static final String PREFIX_DAU = "dau"; //活跃用户量
    private static final String PREFIX_POST = "post"; //最近更新的帖子（有新的点赞或者评论）
    // 某个实体的赞
    // like:entity:entityType:entityId -> set(userId) //给该实体点赞的用户id集合（set）
    public static String getEntityLikeKey(int entityType, int entityId) { //被点赞的实体类型和id
        return PREFIX_ENTITY_LIKE + SPLIT + entityType + SPLIT + entityId;
    }

    // 某个用户发布的内容收到的赞
    // like:user:userId -> int
    public static String getUserLikeKey(int userId) {
        return PREFIX_USER_LIKE + SPLIT + userId;
    }

    // 某个用户关注的实体
    // followee:userId:entityType -> zset(entityId,now) 该用户id和实体类型放在key中，value值以当前时间作为分数来排序，存关注的实体id
    public static String getFolloweeKey(int userId, int entityType) {
        return PREFIX_FOLLOWEE + SPLIT + userId + SPLIT + entityType;
    }

    // 某个实体拥有的粉丝
    // follower:entityType:entityId -> zset(userId,now) 该实体类型，实体的id，value值以当前时间作为分数来排序，存粉丝id
    public static String getFollowerKey(int entityType, int entityId) {
        return PREFIX_FOLLOWER + SPLIT + entityType + SPLIT + entityId;
    }

    // 登录验证码
    public static String getKaptchaKey(String owner) {//owner是生成的字符串，用来标识当前准备登录网站的用户，因为此时用户没登录，也无从得知他的id等信息
        // 但是验证码在服务器端验证时又需要对比（特别是分布式环境下用session会出问题），所以我们短暂地生成owner作为凭证存到cookie中，来处理分布式请求
        return PREFIX_KAPTCHA + SPLIT + owner;
    }

    // 登录的凭证
    public static String getTicketKey(String ticket) {
        return PREFIX_TICKET + SPLIT + ticket;
    }

    // 用户
    public static String getUserKey(int userId) {
        return PREFIX_USER + SPLIT + userId;
    }

    // 单日UV
    public static String getUVKey(String date) {
        return PREFIX_UV + SPLIT + date;
    }

    // 区间UV
    public static String getUVKey(String startDate, String endDate) {
        return PREFIX_UV + SPLIT + startDate + SPLIT + endDate;
    }

    // 单日活跃用户
    public static String getDAUKey(String date) {
        return PREFIX_DAU + SPLIT + date;
    }

    // 区间活跃用户
    public static String getDAUKey(String startDate, String endDate) {
        return PREFIX_DAU + SPLIT + startDate + SPLIT + endDate;
    }

    // 帖子分数
    public static String getPostScoreKey() {
        return PREFIX_POST + SPLIT + "score";
    }

}
