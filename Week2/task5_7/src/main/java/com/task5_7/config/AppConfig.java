package com.task5_7.config;


import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration // Spring配置类
@ComponentScan("com.task5_7") // 扫描 com.task5_7 包及其子包下所有带 @Component 的类
@EnableAspectJAutoProxy // 开启 Spring AOP 代理（自动为匹配的 Bean 生成代理）
public class AppConfig {

}
