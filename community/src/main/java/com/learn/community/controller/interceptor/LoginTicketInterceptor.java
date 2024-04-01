package com.learn.community.controller.interceptor;

import com.learn.community.entity.LoginTicket;
import com.learn.community.entity.User;
import com.learn.community.service.UserService;
import com.learn.community.util.CookieUtil;
import com.learn.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;

@Component
public class LoginTicketInterceptor implements HandlerInterceptor {

    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
//        System.out.println("Ticket拦截器生效");
        // 先从Request中获取到ticket（它是登录凭证的key值，存放在cookies中）
        String ticket = CookieUtil.getValue(request, "ticket");

        if (ticket != null) {
            // 查询凭证
            LoginTicket loginTicket = userService.findLoginTicket(ticket); //获取整个登录凭证
            // 检查凭证是否有效
            if (loginTicket != null && loginTicket.getStatus() == 0 && loginTicket.getExpired().after(new Date())) {
                // 根据凭证查询用户
                User user = userService.findUserById(loginTicket.getUserId());
                // 在本次请求中持有用户，需要用hostHolder来保存用户以处理并发的请求，如果只用一个简单对象来处理会冲突
                hostHolder.setUser(user); //将用户存入了当前线程对应的map里，线程结束前该user都存在

                // 构建用户认证的结果,并存入SecurityContext,以便于Security进行授权
                Authentication authentication = new UsernamePasswordAuthenticationToken( //表示这是账号密码的认证
                        user, user.getPassword(), userService.getAuthorities(user.getId()));
                // 将该结果存入SecurityContextHolder
                // SecurityContextHolder和hostHolder比较像，都是存入了用户有关信息，只不过后者存入了用户认证的结果，替代了LoginTicket
                SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
            }
        }

        return true;
    }

    //模板之前调用该方法，若用户已存在我们在该方法中将user存入model中！！！！以在html中使用，这也是为什么登录后一段时间不用重新登陆的原因！
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        User user = hostHolder.getUser();
        if (user != null && modelAndView != null) {
            modelAndView.addObject("loginUser", user);
        }
    }

    //请求结束后，清理hostHolder
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        hostHolder.clear();
        SecurityContextHolder.clearContext();
    }
}
