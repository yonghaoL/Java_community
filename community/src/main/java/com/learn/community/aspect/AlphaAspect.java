package com.learn.community.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

//这是测试用的。所有先关掉注解
//@Component 需要被容器扫描
//@Aspect 说明它是一个方面组件
public class AlphaAspect {

    //第一个星号表示什么返回值都行，第二个星号表示service包下的所有类，第三个星表示类中所有的方法，第四个括号代表方法中所有的参数
    @Pointcut("execution(* com.learn.community.service.*.*(..))") //这里说明pointcut()对应service下所有模块的所有方法
    public void pointcut() {

    }

    @Before("pointcut()") //在pointcut()对应的模块中的前面织入
    public void before() {
        System.out.println("before");
    }

    @After("pointcut()")//在后面织入
    public void after() {
        System.out.println("after");
    }

    @AfterReturning("pointcut()") //在返回值后织入
    public void afterRetuning() {
        System.out.println("afterRetuning");
    }

    @AfterThrowing("pointcut()")//在抛异常后织入
    public void afterThrowing() {
        System.out.println("afterThrowing");
    }

    @Around("pointcut()")//前后都织入
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        System.out.println("around before");//前织
        Object obj = joinPoint.proceed(); //表示执行模块的方法
        System.out.println("around after");//后织
        return obj;
    }

}
