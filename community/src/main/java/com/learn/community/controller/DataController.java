package com.learn.community.controller;

import com.learn.community.service.DataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

@Controller
public class DataController {

    @Autowired
    private DataService dataService;

    // 统计页面,注意该页面只有管理员可以访问！我们已在SecurityConfig里面添加权限限制
    @RequestMapping(path = "/data", method = {RequestMethod.GET, RequestMethod.POST}) //该方法即可处理get请求也可以处理post请求，因为我们可能需要通过网页访问该页面（get），也有可能需要转发下面的两个POST请求到它这里来
    public String getDataPage() {
        return "/site/admin/data";
    }

    // 统计网站UV，转发到/data网页（只有管理员可以访问）
    @RequestMapping(path = "/data/uv", method = RequestMethod.POST) //要统计需要提交表单中的起始和结束日期
    public String getUV(@DateTimeFormat(pattern = "yyyy-MM-dd") Date start, //该注解告诉服务器传入的日期的格式
                        @DateTimeFormat(pattern = "yyyy-MM-dd") Date end, Model model) {
        long uv = dataService.calculateUV(start, end);
        model.addAttribute("uvResult", uv);
        model.addAttribute("uvStartDate", start);
        model.addAttribute("uvEndDate", end);
//        return "site/admin/data"; //直接这样写也没问题
        return "forward:/data"; // 重定向到/data页面显示数据，向编译器说明我们还需要另外一个方法"/data"继续做处理
    }

    // 统计活跃用户，转发到/data网页（只有管理员可以访问）
    @RequestMapping(path = "/data/dau", method = RequestMethod.POST)
    public String getDAU(@DateTimeFormat(pattern = "yyyy-MM-dd") Date start,
                         @DateTimeFormat(pattern = "yyyy-MM-dd") Date end, Model model) {
        long dau = dataService.calculateDAU(start, end);
        model.addAttribute("dauResult", dau);
        model.addAttribute("dauStartDate", start);
        model.addAttribute("dauEndDate", end);
        return "forward:/data"; // 重定向到/data页面显示数据
    }

}
