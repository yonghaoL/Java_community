package com.learn.community.controller;

import com.learn.community.util.CommunityUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.*;

@Controller
@RequestMapping("/alpha") //给浏览器访问的路径叫/alpha
public class AlphaCotroller {
    @RequestMapping("/Hello")
    @ResponseBody
    public String Hello(){
        return "Hello World!";
    }

    /************GET请求处理,有两个参数,该参数名需要和网页端输入的参数名一致***************/
    // /students?current=1&limit=20
    @RequestMapping(path = "/students", method = RequestMethod.GET)//前面是路径，后面表示只处理GET请求而不处理其他请求
    @ResponseBody
    public String getStudents(
            //这里的注解是对current作说明，即该参数不是必须的，默认值为1，limit注解同理
            @RequestParam(name="current", required = false, defaultValue = "1" ) int current,
            @RequestParam(name="limit",   required = false, defaultValue = "20") int limit){
        return "some students:" + current + ":" + limit;
    }

    //另一种获取参数方式（通过路径）
    // /students/123
    @RequestMapping(path = "/students/{id}", method = RequestMethod.GET)//前面是路径，后面表示只处理GET请求而不处理其他请求
    @ResponseBody
    public String getStudents1(@PathVariable("id") int id){ //注解可以从路径中得到变量，注意需要类型一致
        return "some students:" + id;
    }
    /************GET请求处理,有两个参数,该参数名需要和网页端输入的参数名一致***************/

    /************POST请求处理***************/
    @RequestMapping(path = "/student", method = RequestMethod.POST)//前面是路径，后面表示只处理GET请求而不处理其他请求
    @ResponseBody
    public String saveStudents1(String name, int age){ //该参数需要和我们的html表单中的名字一致（name和age）
        System.out.println(name);
        System.out.println(age);
        return "Save successfully";
    }
    /************POST请求处理***************/

    /************响应html数据***************/
    @RequestMapping(path = "/teacher", method = RequestMethod.GET)
    public ModelAndView getTeacher(){ //返回model和视图数据
        ModelAndView mav = new ModelAndView();
        //模板需要几个变量就add几个数据
        mav.addObject("name", "张三");
        mav.addObject("age", "12");
        //模板路径
        mav.setViewName("/demo/view");//默认父目录为templates,view全称view.html，只是参数里面不用写
        return mav; //返回的是html，没有加@ResponseBody注解
    }

    //实现响应html数据的另一种方法，返回视图路径，并通过参数引用传递返回模型
    @RequestMapping(path = "/school", method = RequestMethod.GET)
    public String getSchool(Model mod){ //返回model和视图数据
        //模板需要几个变量就add几个数据
        mod.addAttribute("name", "ustc");
        mod.addAttribute("age", "72");
        //模板路径
        return "/demo/view"; //返回的是html，没有加@ResponseBody注解
    }
    /************响应html数据***************/

    /************响应JSON数据（异步请求）***************/
    @RequestMapping(path = "/emp", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> getEmployee(){
        Map<String, Object> emp = new HashMap<>();
        emp.put("name", "李四");
        emp.put("age", 20);
        emp.put("salary", 20000);
        return emp;
    }

    //若要返回一组员工：
    @RequestMapping(path = "/emps", method = RequestMethod.GET)
    @ResponseBody
    public List<Map<String, Object>> getEmployees(){
        List<Map<String, Object>> emps = new ArrayList<>();

        Map<String, Object> emp = new HashMap<>();
        emp.put("name", "李四");
        emp.put("age", 20);
        emp.put("salary", 20000);

        Map<String, Object> emp2 = new HashMap<>();
        emp2.put("name", "王五");
        emp2.put("age", 20);
        emp2.put("salary", 20000);

        emps.add(emp);
        emps.add(emp2);
        return emps;
    }
    /************响应JSON数据（异步请求）***************/

    // cookie示例

    @RequestMapping(path = "/cookie/set", method = RequestMethod.GET)
    @ResponseBody
    public String setCookie(HttpServletResponse response) {//response是服务器返回给浏览器的内容
        // 创建cookie
        Cookie cookie = new Cookie("code", CommunityUtil.generateUUID());
        // 设置cookie生效的范围,浏览器访问/community/alpha下的路径时才发送cookie给服务器
        cookie.setPath("/community/alpha");
        // 设置cookie的生存时间
        cookie.setMaxAge(60 * 10);
        // 发送cookie
        response.addCookie(cookie);

        return "set cookie";
    }

    @RequestMapping(path = "/cookie/get", method = RequestMethod.GET)
    @ResponseBody
    public String getCookie(@CookieValue("code") String code) { //该注解可以得到浏览器的request请求中key为“code”对应的cookie
        System.out.println(code);
        return "get cookie";
    }

    // session示例

    @RequestMapping(path = "/session/set", method = RequestMethod.GET)
    @ResponseBody
    public String setSession(HttpSession session) { //session可以存很多数据（因为它不用来回传输，而是只存在服务器），而cookie只能存字符串，因为要传输
        session.setAttribute("id", 1);
        session.setAttribute("name", "Test");
        return "set session";
    }

    @RequestMapping(path = "/session/get", method = RequestMethod.GET)
    @ResponseBody
    public String getSession(HttpSession session) {
        System.out.println(session.getAttribute("id"));
        System.out.println(session.getAttribute("name"));
        return "get session";
    }
}
