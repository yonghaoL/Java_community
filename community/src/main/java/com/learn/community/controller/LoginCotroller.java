package com.learn.community.controller;

import com.learn.community.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.learn.community.entity.User;
import java.util.Map;

@Controller
public class LoginCotroller {
    @Autowired
    private UserService userService;

    @RequestMapping(path = "/register", method = RequestMethod.GET)
    public String getRegisterPage() {
        return "/site/register";
    } //register页面get请求，返回静态模板对应的网页

    @RequestMapping(path = "/login", method = RequestMethod.GET)
    public String getLoginPage() {
        return "/site/login"; //该动态模板html在templates目录下
    }

    @RequestMapping(path = "/register", method = RequestMethod.POST)
    //这里有疑问，为什么可以自动把三个网页输入注入到user的三个对应属性，是因为html中键的名字和类的属性名字相同吗？测试一下
    public String register(Model model, User user) { //注册时用户传入了账号，邮箱，密码，与user的属性相匹配.SpringMVC会自动注入一个bean作为参数user，并赋值属性
        Map<String, Object> map = userService.register(user);//调用Service注册

        if (map == null || map.isEmpty()) {
            model.addAttribute("msg", "注册成功,我们已经向您的邮箱发送了一封激活邮件,请尽快激活!");
            model.addAttribute("target", "/index");
            return "/site/operate-result"; //这是一个注册成功的跳转页面html
        } else {
            model.addAttribute("usernameMsg", map.get("usernameMsg")); //提取错误信息并返回
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            model.addAttribute("emailMsg", map.get("emailMsg"));
            return "/site/register"; //账号创建失败了，return回创建页面，显示错误信息
        }
    }
}
