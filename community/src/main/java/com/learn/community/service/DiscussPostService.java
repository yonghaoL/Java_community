//Service是controller调用的用来处理业务的代码，尽管在本项目中该代码似乎只是简单调用dao的方法
//但这些代码不要写在dao中，我们要尽量保证代码层次的分明，降低耦合性
package com.learn.community.service;

import com.learn.community.dao.DiscussPostMapper;
import com.learn.community.entity.DiscussPost;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service //加入该注解使得它可以被容器扫描到
public class DiscussPostService {
    @Autowired
    private DiscussPostMapper discussPostMapper; //注入DiscussPostMapper,尽管我们只定义了接口和对应sql语句（写在xml文件中），mybatis会帮我们实现类并创建实例

    //声明一个业务方法，查询post集合
    public List<DiscussPost> findDiscussPosts(int userId, int offset, int limit){
        return discussPostMapper.selectDiscussPosts(userId, offset, limit);
    }

    public int findDiscussPostRows(int userId){
        return discussPostMapper.selectDiscussPostRows(userId);
    }
}
