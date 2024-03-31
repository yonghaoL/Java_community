package com.learn.community.dao.elasticsearch;

import com.learn.community.entity.DiscussPost;
import com.learn.community.entity.DiscussPost;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

//该注解是spring提供的数据访问层注解
@Repository
public interface DiscussPostRepository extends ElasticsearchRepository<DiscussPost, Integer> { //继承于spring提供的ElasticsearchRepository接口，实体类型为DiscussPost，主键为integer
    //ElasticsearchRepository类中定义了一系列增删改查方法，spring会帮我们自动实现，我们只需要调用就可以了
}
