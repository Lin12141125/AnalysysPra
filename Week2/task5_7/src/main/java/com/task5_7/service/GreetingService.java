package com.task5_7.service;

import org.springframework.stereotype.Component;

@Component // 把这个类交给 Spring 容器管理
public class GreetingService {
    public String greet(){
        System.out.println("GreetingServive.greet运行中");
        return "Hello from Spring!";
    }
}
