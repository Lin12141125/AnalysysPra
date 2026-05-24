package com.project.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@ComponentScan("com.project") // 扫描整个com.project包
@EnableAspectJAutoProxy // 开启AOP代理
public class AppConfig {
}
