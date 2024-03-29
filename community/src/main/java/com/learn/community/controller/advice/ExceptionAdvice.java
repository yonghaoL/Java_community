package com.learn.community.controller.advice;

import com.learn.community.util.CommunityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@ControllerAdvice(annotations = Controller.class) //让该注解只扫描带有Controller注解的那些bean，而不是所有bean
public class ExceptionAdvice {

    private static final Logger logger = LoggerFactory.getLogger(ExceptionAdvice.class);

    @ExceptionHandler({Exception.class}) //表示用它来处理所有异常，括号里面写异常类型，而Exception.class是所有异常父类，即处理所有异常
    //参数是异常，http请求及回应
    public void handleException(Exception e, HttpServletRequest request, HttpServletResponse response) throws IOException {
        logger.error("服务器发生异常: " + e.getMessage()); //记录总的记录
        for (StackTraceElement element : e.getStackTrace()) { //遍历发生的异常数组一一记录
            logger.error(element.toString());
        }

        //先判断服务器的请求是不是异步请求，异步请求需要返回JSON，不是的话再重定向到/error页面
        String xRequestedWith = request.getHeader("x-requested-with"); //获取该请求的方式
        if ("XMLHttpRequest".equals(xRequestedWith)) { //说明该请求想让服务器返回xml，是异步请求，只有异步请求才会返回xml（可以用JSON格式），否则返回html或http
            //设置response返回类型
//            response.setContentType("application/JSON");//表示向浏览器返回JSON字符串，浏览器得到后需要人为地转换成JSON对象
            response.setContentType("application/plain;charset=utf-8"); //也可以标明返回的是普通字符串(但我们自己确认它是JSON格式字符串)，然后统一转换为JSON对象
            PrintWriter writer = response.getWriter();
            writer.write(CommunityUtil.getJSONString(1, "服务器异常!")); //返回JSON格式字符串，显示在浏览器端
        } else {
            response.sendRedirect(request.getContextPath() + "/error"); //不是异步请求，直接返回错误页面即可
        }
    }

}
