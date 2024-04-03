package com.learn.community;

import com.learn.community.dao.DiscussPostMapper;
import com.learn.community.dao.elasticsearch.DiscussPostRepository;
import com.learn.community.entity.DiscussPost;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.SearchResultMapper;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class ElasticsearchTests {

    @Autowired
    private DiscussPostMapper discussMapper;

    @Autowired
    private DiscussPostRepository discussRepository;

    @Autowired
    private ElasticsearchTemplate elasticTemplate; //有些情况DiscussPostRepository处理不了，故我们将ElasticsearchTemplate也注入进来

    @Test
    public void testInsert() {
        //将数据用mapper从mysql中取出来然后插进ES服务器的库中去（注意这是在我们用注解写好了字段间一一对应之后）
        discussRepository.save(discussMapper.selectDiscussPostById(241));
        discussRepository.save(discussMapper.selectDiscussPostById(242));
        discussRepository.save(discussMapper.selectDiscussPostById(243));
    }

    @Test
    public void testInsertList() {
        discussRepository.saveAll(discussMapper.selectDiscussPosts(101, 0, 100, 0));
        discussRepository.saveAll(discussMapper.selectDiscussPosts(102, 0, 100, 0));
        discussRepository.saveAll(discussMapper.selectDiscussPosts(103, 0, 100, 0));
        discussRepository.saveAll(discussMapper.selectDiscussPosts(111, 0, 100, 0));
        discussRepository.saveAll(discussMapper.selectDiscussPosts(112, 0, 100, 0));
        discussRepository.saveAll(discussMapper.selectDiscussPosts(131, 0, 100, 0));
        discussRepository.saveAll(discussMapper.selectDiscussPosts(132, 0, 100, 0));
        discussRepository.saveAll(discussMapper.selectDiscussPosts(133, 0, 100, 0));
        discussRepository.saveAll(discussMapper.selectDiscussPosts(134, 0, 100, 0));
    }

    @Test
    public void testUpdate() {
        DiscussPost post = discussMapper.selectDiscussPostById(231);
        post.setContent("我是新人,使劲灌水.");
        //该数据就是存入新的数据以覆盖以前的数据
        discussRepository.save(post);
    }

    @Test
    public void testDelete() {
//         discussRepository.deleteById(231);//按id删除数据
        discussRepository.deleteAll(); //删除所有数据
    }

    //核心方法！！！！！ 搜索
    @Test
    public void testSearchByRepository() {
        //（链式编程）
        SearchQuery searchQuery = new NativeSearchQueryBuilder() //构造接口实现类（接口是spring-ES框架提供的）
                .withQuery(QueryBuilders.multiMatchQuery("互联网寒冬", "title", "content")) //即在title又在content中搜索该关键词（显然smart分词会将其拆分为互联网、寒冬）
                .withSort(SortBuilders.fieldSort("type").order(SortOrder.DESC)) //排序条件：依次按照tpye，score，createTime的降序高低来排序
                .withSort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
                .withPageable(PageRequest.of(0, 10)) //分页条件：当前显示第1页，该页显示10条数据
                .withHighlightFields( //在字段中指定高亮显示关键词，并用标签把它们包起来
                        new HighlightBuilder.Field("title").preTags("<em>").postTags("</em>"), //在标题中把关键词高亮
                        new HighlightBuilder.Field("content").preTags("<em>").postTags("</em>") //在内容中把关键词高亮
                ).build();

        // 下面的Repository底层调用了elasticTemplate.queryForPage(searchQuery, class, SearchResultMapper)这个方法去查询。
        // 该底层获取得到了高亮显示的值, 但是没有返回.Repository的实现类会返回原始结果和高亮数据的部分，一共两份数据，我们需要把高亮数据整合到原始数据里面去。
        // 其中SearchResultMapper是整合高亮数据和原始数据的，但它没有用。
        // 故若要正确显示高亮，我们需要手动实现elasticTemplate.queryForPage，见下一个测试方法

        //注意该Page是spring-ES框架中的
        Page<DiscussPost> page = discussRepository.search(searchQuery); //可以看作集合
        System.out.println(page.getTotalElements()); //有多少数据匹配
        System.out.println(page.getTotalPages()); //总共有多少页
        System.out.println(page.getNumber()); //当前在第几页
        System.out.println(page.getSize()); //每一页几条数据
        for (DiscussPost post : page) {
            System.out.println(post); //遍历打印数据（Page实现了iterable接口,可以遍历）
        }
    }

    //用testSearchByTemplate，该方法重写了elasticTemplate.queryForPage正确返回高亮结果
    @Test
    public void testSearchByTemplate() {
        SearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.multiMatchQuery("互联网寒冬", "title", "content"))
                .withSort(SortBuilders.fieldSort("type").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
                .withPageable(PageRequest.of(0, 10))
                .withHighlightFields(
                        new HighlightBuilder.Field("title").preTags("<em>").postTags("</em>"),
                        new HighlightBuilder.Field("content").preTags("<em>").postTags("</em>")
                ).build();

        Page<DiscussPost> page = elasticTemplate.queryForPage(searchQuery, DiscussPost.class, new SearchResultMapper() {
            //重写该方法处理高亮
            @Override
            public <T> AggregatedPage<T> mapResults(SearchResponse response, Class<T> aClass, Pageable pageable) {
                SearchHits hits = response.getHits(); //取到数据
                if (hits.getTotalHits() <= 0) { //数据量是否为空
                    return null; //判空
                }

                //把数据封装到一个集合里然后返回
                List<DiscussPost> list = new ArrayList<>();
                for (SearchHit hit : hits) { //hit为JSON格式数据
                    DiscussPost post = new DiscussPost();

                    String id = hit.getSourceAsMap().get("id").toString(); //hit.getSourceAsMap()返回map型数据（从JSON格式转化）
                    post.setId(Integer.valueOf(id)); //传入post

                    String userId = hit.getSourceAsMap().get("userId").toString();
                    post.setUserId(Integer.valueOf(userId));

                    String title = hit.getSourceAsMap().get("title").toString();
                    post.setTitle(title); //先插入原始title（万一没有高亮字段，就返回这个，有高亮的就用高亮的取代）

                    String content = hit.getSourceAsMap().get("content").toString();
                    post.setContent(content);

                    String status = hit.getSourceAsMap().get("status").toString();
                    post.setStatus(Integer.valueOf(status));

                    String createTime = hit.getSourceAsMap().get("createTime").toString();
                    post.setCreateTime(new Date(Long.valueOf(createTime)));

                    String commentCount = hit.getSourceAsMap().get("commentCount").toString();
                    post.setCommentCount(Integer.valueOf(commentCount));

                    // 处理高亮显示的结果
                    HighlightField titleField = hit.getHighlightFields().get("title"); //获取与title有关的高亮显示内容
                    if (titleField != null) {//不为空则覆盖之前的title
                        post.setTitle(titleField.getFragments()[0].toString()); //可能根据高亮词匹配了多段，我们取第一段，即只返回第一段，
                        // 因为文章可能很长，我们在搜索页面不可能全部返回。（上一个方法中的discussRepository.search返回了全部内容）
                    }

                    HighlightField contentField = hit.getHighlightFields().get("content");
                    if (contentField != null) {
                        post.setContent(contentField.getFragments()[0].toString());
                    }

                    list.add(post);
                }

                return new AggregatedPageImpl(list, pageable, //返回的是AggregatedPageImpl实现类，需要构造
                        hits.getTotalHits(), response.getAggregations(), response.getScrollId(), hits.getMaxScore());
            }
        });

        System.out.println(page.getTotalElements());
        System.out.println(page.getTotalPages());
        System.out.println(page.getNumber());
        System.out.println(page.getSize());
        for (DiscussPost post : page) {
            System.out.println(post);
        }
    }

}
