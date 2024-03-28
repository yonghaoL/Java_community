//Service是controller调用的用来处理业务的代码，尽管在本项目中该代码似乎只是简单调用dao的方法
//但这些代码不要写在dao中，我们要尽量保证代码层次的分明，降低耦合性
package com.learn.community.service;

import com.learn.community.dao.DiscussPostMapper;
import com.learn.community.entity.DiscussPost;
import com.learn.community.util.SensitiveFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

@Service //加入该注解使得它可以被容器扫描到
public class DiscussPostService {
    @Autowired
    private DiscussPostMapper discussPostMapper; //注入DiscussPostMapper,尽管我们只定义了接口和对应sql语句（写在xml文件中），mybatis会帮我们实现类并创建实例

    @Autowired
    private SensitiveFilter sensitiveFilter;

    //声明一个业务方法，查询post集合
    public List<DiscussPost> findDiscussPosts(int userId, int offset, int limit){
        return discussPostMapper.selectDiscussPosts(userId, offset, limit);
    }

    //根据id查询post数量
    public int findDiscussPostRows(int userId){
        return discussPostMapper.selectDiscussPostRows(userId);
    }

    public int addDiscussPost(DiscussPost post) {
        if (post == null) {
            throw new IllegalArgumentException("参数不能为空!");
        }

        // 转义HTML标记，避免用户在标题和内容中使用html语法破坏网页内容
        post.setTitle(HtmlUtils.htmlEscape(post.getTitle()));
        post.setContent(HtmlUtils.htmlEscape(post.getContent()));
        // 过滤敏感词
        post.setTitle(sensitiveFilter.filter(post.getTitle()));
        post.setContent(sensitiveFilter.filter(post.getContent()));

        return discussPostMapper.insertDiscussPost(post);
    }

    public DiscussPost findDiscussPostById(int id) {
        return discussPostMapper.selectDiscussPostById(id);
    }

    public int updateCommentCount(int id, int commentCount) {
        return discussPostMapper.updateCommentCount(id, commentCount);
    }
}
