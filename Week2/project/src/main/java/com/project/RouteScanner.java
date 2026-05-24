package com.project;

import org.springframework.aop.support.AopUtils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;


public class RouteScanner {
    // 存放 URL 到 方法 和 所属实例 的映射
    private Map<String, RouteInfo> routeMap = new HashMap<>();

    // 注册控制器中的带@RequestMapping注解的方法
    public void registerController(Object controller) throws Exception{ // 读入class实例
        Class<?> clazz = controller.getClass(); // 获取class实例的Class对象
        // 如果是 Spring AOP 代理，获取原始目标类
        if (AopUtils.isAopProxy(controller)) {
            clazz = AopUtils.getTargetClass(controller);
            System.out.println("检测到代理对象，原始类: " + clazz.getName());
        }
        /*
        如果不获取原始目标类：
        LoggingAspect: @Around("@annotation(com.project.RequestMapping)") --> Spring会为所有带@RequestMapping注解的Controller Bean创建AOP代理对象
        MiniTomcat: routeScanner.registerController(controller); --> Controller是代理对象
        registerController: Class<?> clazz = controller.getClass(); --> 拿到的是代理类，不是原始类
        代理类的方法上没有保留原始注解 --> 没有任何路由被注册
         */
        Method[] methods = clazz.getDeclaredMethods(); // 获取Class对象的所有方法
        for (Method method : methods) { // 遍历所有方法
            RequestMapping anno = method.getAnnotation(RequestMapping.class);
            if (anno != null) { // 如果方法上有@RequestMapping注解
                String url = anno.value(); // url=RequestMapping注解的value值-->该方法对应的url路径
                routeMap.put(url, new RouteInfo(controller, method)); // 储存在映射中，<路径，(class实例,方法)>
                System.out.println("\n"+"Controller: "+controller.getClass().getSimpleName());
                System.out.println("注册路由: " + url + " -> " + method.getName()); // 注册路由: url路径 -> 对应方法名
            }
        }
    }

    static class RouteInfo { //储存路由信息，包括所属控制器实例和方法
        Object controller;
        Method method;
        RouteInfo(Object controller, Method method) {
            this.controller = controller;
            this.method = method;
        }
    }

    public String execute(String url) throws Exception { // 执行路由
        RouteInfo info = routeMap.get(url); // 从映射中获取对应url的RouteInfo(class实例,方法)
        if (info == null) return "404 Not Found"; // 如果没有找到对应的RouteInfo，返回404错误
        String result = (String) info.method.invoke(info.controller); // 反射调用方法，执行对应的控制器方法
        return result; // 返回方法执行结果
    }
}
