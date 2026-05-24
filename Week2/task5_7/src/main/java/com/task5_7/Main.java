package com.task5_7;


import com.task5_7.config.AppConfig;
import com.task5_7.service.GreetingService;
import com.task5_7.service.UserService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class Main {
    public static void main(String[] args) {

        // 1. 启动Spring容器，加载配置类
        ApplicationContext context=new AnnotationConfigApplicationContext(AppConfig.class);

        // 2. 从Spring容器中获取Bean
        GreetingService greetingService=context.getBean(GreetingService.class);
        UserService userService=context.getBean(UserService.class);

        // 3. 调用方法验证
        System.out.println(greetingService.greet());
        userService.showMessage();

        // 4. 关闭Spring容器
        ((AnnotationConfigApplicationContext) context).close();
    }
}