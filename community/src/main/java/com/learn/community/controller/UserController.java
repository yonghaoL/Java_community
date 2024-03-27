package com.learn.community.controller;

import com.learn.community.annotation.LoginRequired;
import com.learn.community.entity.User;
import com.learn.community.service.UserService;
import com.learn.community.util.CommunityUtil;
import com.learn.community.util.HostHolder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

@Controller
@RequestMapping("/user")
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Value("${community.path.upload}")
    private String uploadPath;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder;
    @RequestMapping(path = "/setting", method = RequestMethod.GET)
    @LoginRequired //该方法只有登陆的用户才能访问
    public String getSettingPage() {
        return "/site/setting";
    }

    //上传更新头像处理（POST请求）
    @LoginRequired
    @RequestMapping(path = "/upload", method = RequestMethod.POST)
    public String uploadHeader(MultipartFile headerImage, Model model) { //MultipartFile是mvc提供的类型，这里若有多个文件可以用数组接收
        if (headerImage == null) {
            model.addAttribute("error", "您还没有选择图片!");
            return "/site/setting"; //回到设置页面
        }

        String fileName = headerImage.getOriginalFilename();
        String suffix = fileName.substring(fileName.lastIndexOf(".")); //先暂存文件名字后缀
        if (StringUtils.isBlank(suffix)) {
            model.addAttribute("error", "文件的格式不正确!");
            return "/site/setting";
        }

        // 生成随机文件名（用户上传的文件可能都叫1.jpg）
        fileName = CommunityUtil.generateUUID() + suffix;
        // 确定文件存放的路径
        File dest = new File(uploadPath + "/" + fileName);
        try {
            // 存储文件操作
            headerImage.transferTo(dest);
        } catch (IOException e) {
            logger.error("上传文件失败: " + e.getMessage());
            throw new RuntimeException("上传文件失败,服务器发生异常!", e);
        }

        // 更新当前用户的头像的路径(web访问路径)
        // 更新为：http://localhost:8080/community/user/header/xxx.png
        User user = hostHolder.getUser();
        String headerUrl = domain + contextPath + "/user/header/" + fileName;
        userService.updateHeader(user.getId(), headerUrl);

        return "redirect:/index"; //重定向到首页，可以让用户看到更新后的头像
    }

    //获取头像Request处理
    @RequestMapping(path = "/header/{fileName}", method = RequestMethod.GET)
    public void getHeader(@PathVariable("fileName") String fileName, HttpServletResponse response) {
        //服务器存放路径，拼接路径和文件名即可
        fileName = uploadPath + "/" + fileName;
        //文件后缀获取
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        //响应图片，给浏览器声明我们输出的文件格式！
        response.setContentType("image/" + suffix);
        try (
                FileInputStream fis = new FileInputStream(fileName); //该流读取文件到内存
                OutputStream os = response.getOutputStream(); //该流输出文件到浏览器
        ) {//将文件传给浏览器
            byte[] buffer = new byte[1024]; //一次传输1kb大小
            int b = 0;
            while ((b = fis.read(buffer)) != -1) {
                os.write(buffer, 0, b);
            }
        } catch (IOException e) {
            logger.error("读取头像失败: " + e.getMessage());
        }
    }

    //更新密码处理（POST请求）
    @RequestMapping(path = "/change", method = RequestMethod.POST)
    public String uploadPassword(String oldPassword, String newPassword, Model model) { //MultipartFile是mvc提供的类型，这里若有多个文件可以用数组接收
        if (StringUtils.isBlank(oldPassword)) {
            model.addAttribute("errorOld", "原始密码不能为空!");
            return "/site/setting"; //回到设置页面
        }

        if (StringUtils.isBlank(newPassword)) {
            model.addAttribute("errorNew", "新的密码不能为空!");
            return "/site/setting"; //回到设置页面
        }

        User user = hostHolder.getUser();
        String oldPasswordDatabase = CommunityUtil.md5(oldPassword + user.getSalt());

        if (!user.getPassword().equals(oldPasswordDatabase)) {
            model.addAttribute("errorOld", "原始密码错误!");
            return "/site/setting"; //回到设置页面
        }

        userService.updatePassword(user.getId(), CommunityUtil.md5(newPassword + user.getSalt()));
        System.out.println("修改成功");

        return "redirect:/index"; //重定向到首页，可以让用户看到更新后的头像
    }

}
