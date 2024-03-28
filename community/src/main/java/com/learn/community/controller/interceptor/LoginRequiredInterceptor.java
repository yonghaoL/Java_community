package com.learn.community.controller.interceptor;

import com.learn.community.annotation.LoginRequired;
import com.learn.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;

@Component
public class LoginRequiredInterceptor implements HandlerInterceptor {

    @Autowired
    private HostHolder hostHolder;

    //重写preHandle方法，在请求前就要判断是否拦截
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
//        System.out.println("Required拦截器生效");
        //首先判断拦截到的是否是一个方法，是方法的话就不拦截
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler; //先强转
            Method method = handlerMethod.getMethod();
            LoginRequired loginRequired = method.getAnnotation(LoginRequired.class);
            //利用反射在运行时去取得该方法的LoginRequired注解（这里传入注解类名，可以单独取得该类型注解）
            //注意这里就是反射的经典运用，检测传进来的方法是否有LoginRequired注解，有的话就进行以下操作！
            if (loginRequired != null && hostHolder.getUser() == null) { //该方法需要登录且用户没有登录（hostHolder.getUser() == null）
//                System.out.println(request.getContextPath() + "/login");
                response.sendRedirect(request.getContextPath() + "/login"); //用response强制重定向到login界面登录
//                response.setStatus(302);
                //且由于此方法是接口声明的，就不能像Controller那样随心所欲地直接返回一个html模板文件，而需要用response重定向
                //话又说回来，其实Controller的底层，重定向返回html也是用response像上面这样写的，封装了而已
                // /login前的路径既可以和以前一样从配置文件中注入进来，也可以直接从request里面取！
                return false;
            }
        }
        return true;
    }
}
