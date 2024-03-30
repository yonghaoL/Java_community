package com.learn.community.controller;

import com.google.code.kaptcha.Producer;
import com.learn.community.service.UserService;
import com.learn.community.util.CommunityConstant;
import com.learn.community.util.CommunityUtil;
import com.learn.community.util.RedisKeyUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.learn.community.entity.User;

import javax.imageio.ImageIO;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Controller
public class LoginController implements CommunityConstant {
    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private Producer kaptchaProducer;

    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${server.servlet.context-path}") //将配置文件中的context-path注入到属性
    private String contextPath;

    @RequestMapping(path = "/register", method = RequestMethod.GET)
    public String getRegisterPage() {
        return "/site/register";
    } //register页面get请求，返回静态模板对应的网页

    @RequestMapping(path = "/login", method = RequestMethod.GET)
    public String getLoginPage() {
        return "/site/login"; //该动态模板html在templates目录下
    }


    //注册页面
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

    //处理激活码页面
    // http://localhost:8080/community/activation/xxx/code：注意该网址应该是激活邮件中的链接网址（由用户id和激活码拼接的）
    @RequestMapping(path = "/activation/{userId}/{code}", method = RequestMethod.GET)//该路径是变量，所有/activation/{xxx}/{xxx}都会由它处理
    public String activation(Model model, @PathVariable("userId") int userId, @PathVariable("code") String code) {
        int result = userService.activation(userId, code);
        //根据结果向html模板中写入变量的值（msg和target）并返回页面
        if (result == ACTIVATION_SUCCESS) {
            model.addAttribute("msg", "激活成功,您的账号已经可以正常使用了!");
            model.addAttribute("target", "/login"); //跳转到登录页面
        } else if (result == ACTIVATION_REPEAT) {
            model.addAttribute("msg", "无效操作,该账号已经激活过了!");
            model.addAttribute("target", "/index"); //跳转到首页
        } else {
            model.addAttribute("msg", "激活失败,您提供的激活码不正确!");
            model.addAttribute("target", "/index");
        }
        return "/site/operate-result";
    }

    //浏览器向服务器申请一个验证码（旧版实现）
//    @RequestMapping(path = "/kaptcha", method = RequestMethod.GET)
//    public void getKaptcha(HttpServletResponse response, HttpSession session) { //response用于返回该图片
//        //由于获取图片和浏览器输入验证码验证是不同的请求，故服务器需要将生成的验证码存在cookie中，但存在cookie不安全，故使用session，注意分布式会出问题，session是存数据在服务器端
//        // 生成验证码
//        String text = kaptchaProducer.createText();
//        BufferedImage image = kaptchaProducer.createImage(text);
//
//        // 将验证码存入session
//        session.setAttribute("kaptcha", text);
//
//        // 将图片输出给浏览器
//        response.setContentType("image/png");//向浏览器声明返回的文件格式是"image/png"
//        try (OutputStream os = response.getOutputStream();){//获取response的输出字节流
//            ImageIO.write(image, "png", os);
//        } catch (IOException e) {
//            logger.error("响应验证码失败:" + e.getMessage());
//        }
//    }

    //发送验证码（新版实现）
    @RequestMapping(path = "/kaptcha", method = RequestMethod.GET)
    public void getKaptcha(HttpServletResponse response) { //response用于返回该图片
        // 生成验证码
        String text = kaptchaProducer.createText();
        BufferedImage image = kaptchaProducer.createImage(text);

        // 验证码的归属
        String kaptchaOwner = CommunityUtil.generateUUID();
        Cookie cookie = new Cookie("kaptchaOwner", kaptchaOwner); //将验证码的凭证存到cookie中,就是存字符串
        cookie.setMaxAge(60); //验证码60s失效
        cookie.setPath(contextPath); //所有路径生效
        response.addCookie(cookie);
        // 将验证码存入Redis
        String redisKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner); //设置该验证码对应凭证命名的key，这样服务器在验证用户发送的验证码时能够找到
        redisTemplate.opsForValue().set(redisKey, text, 60, TimeUnit.SECONDS);//redis中同样60s过期

        // 将突图片输出给浏览器
        response.setContentType("image/png");
        try {
            OutputStream os = response.getOutputStream();
            ImageIO.write(image, "png", os);
        } catch (IOException e) {
            logger.error("响应验证码失败:" + e.getMessage());
        }
    }

    //浏览器向服务器发送登录信息：
    @RequestMapping(path = "/login", method = RequestMethod.POST)
    /**
     * 需要的参数解释
     * rememberme:是网页选项“记住我”，可以记住密码？
     * session:用来获取服务器之前生成的对应验证码来和用户传输的进行比较，它是先接收了浏览器发的sessionid然后查找到对应session的
     * response:用来传输cookie返回给浏览器
     */
    public String login(String username, String password, String code, boolean rememberme,
                        //用注解从cookie中取值
                        Model model, @CookieValue("kaptchaOwner") String cookieKaptcha, /*HttpSession session, */HttpServletResponse response) {
//        // 检查验证码 旧版实现
//        String kaptcha = (String) session.getAttribute("kaptcha"); //从session中获取之前生成的验证码
//        if (StringUtils.isBlank(kaptcha) || StringUtils.isBlank(code) || !kaptcha.equalsIgnoreCase(code)) { //equalsIgnoreCase忽略大小写
//            model.addAttribute("codeMsg", "验证码不正确!");
//            return "/site/login";
//        }

        // 检查验证码（新版实现）
        String verificationCode = null;
        if (!StringUtils.isBlank(cookieKaptcha)){
            String redisKey = RedisKeyUtil.getKaptchaKey(cookieKaptcha); //先构造对应key
            verificationCode = (String) redisTemplate.opsForValue().get(redisKey); //从redis取出验证码
        }
        if (StringUtils.isBlank(verificationCode) || StringUtils.isBlank(code) || !verificationCode.equalsIgnoreCase(code)) { //equalsIgnoreCase忽略大小写
            model.addAttribute("codeMsg", "验证码不正确!");
            return "/site/login";
        }

        // 检查账号,密码
        int expiredSeconds = rememberme ? REMEMBER_EXPIRED_SECONDS : DEFAULT_EXPIRED_SECONDS;
        Map<String, Object> map = userService.login(username, password, expiredSeconds);
        if (map.containsKey("ticket")) { //用户login成功才往map中放ticket
            //需要将该ticket放在cookie里面发给浏览器储存
            Cookie cookie = new Cookie("ticket", map.get("ticket").toString());
            cookie.setPath(contextPath);
            cookie.setMaxAge(expiredSeconds);
            response.addCookie(cookie);
            return "redirect:/index"; //重定向
//            return "index"; //为什么不能直接转发？因为转发直接返回的index.html中的变量没有对应model给它们传递参数，所有需要用户再次访问
//            /index，去HomeCotroller中处理请求，给html传参，才能正确显示index.html主页！！！！
        } else {
            model.addAttribute("usernameMsg", map.get("usernameMsg")); //否则返回错误信息
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            return "/site/login";
        }
    }

    @RequestMapping(path = "/logout", method = RequestMethod.GET)
    public String logout(@CookieValue("ticket") String ticket) { //接收cookie中的登录凭证的ticket值（注意浏览器端只存储该key值即map.get("ticket").toString()，完整登录凭证对象在数据库），用来退出
        userService.logout(ticket);
        return "redirect:/login";
    }
}
