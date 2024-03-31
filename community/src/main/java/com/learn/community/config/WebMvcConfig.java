package com.learn.community.config;

import com.learn.community.controller.interceptor.AlphaInterceptor;
import com.learn.community.controller.interceptor.LoginRequiredInterceptor;
import com.learn.community.controller.interceptor.LoginTicketInterceptor;
import com.learn.community.controller.interceptor.MessageInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private AlphaInterceptor alphaInterceptor;

    @Autowired
    private LoginTicketInterceptor loginTicketInterceptor;

    //把拦截器注入进来，生成一个实例，这些拦截器都是在interceptor文件下定义好，注入到这个配置文件类里面来的
    @Autowired
    private LoginRequiredInterceptor loginRequiredInterceptor;

    @Autowired
    private MessageInterceptor messageInterceptor;

    // /**/*.css指所有文件夹下的所有.css文件
    @Override
    public void addInterceptors(InterceptorRegistry registry) { //这三个拦截器被我们传进了registry中，然后被spring使用
        registry.addInterceptor(alphaInterceptor)
                .excludePathPatterns("/**/*.css", "/**/*.js", "/**/*.png", "/**/*.jpg", "/**/*.jpeg")//1.这里说明拦截器会拦截除了这些路径格式之外的的请求，然后处理（这些静态和图片不需要拦截处理，没有业务逻辑）
                .addPathPatterns("/register", "/login"); //2.这里说明只拦截对应请求（一般和上面不是一起用的，这里只是举例）


        registry.addInterceptor(loginTicketInterceptor) //该拦截器会拦截除了这些路径格式之外的的请求，然后处理（减少操作）
                .excludePathPatterns("/**/*.css", "/**/*.js", "/**/*.png", "/**/*.jpg", "/**/*.jpeg");

        registry.addInterceptor(loginRequiredInterceptor)//该拦截器会拦截除了这些路径格式之外的的请求，然后处理
                .excludePathPatterns("/**/*.css", "/**/*.js", "/**/*.png", "/**/*.jpg", "/**/*.jpeg");//(说明所有动态请求都被拦截)

        registry.addInterceptor(messageInterceptor)//该拦截器会拦截除了这些路径格式之外的的请求，然后处理
                .excludePathPatterns("/**/*.css", "/**/*.js", "/**/*.png", "/**/*.jpg", "/**/*.jpeg");

        //这里添加了多个拦截器，多个拦截器的执行顺序与添加顺序相同，执行流程见笔记
    }

}
